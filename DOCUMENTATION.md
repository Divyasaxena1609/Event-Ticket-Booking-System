# Event Ticket Booking System

## Run the complete application

Install Docker Desktop, start it, then run from the repository root:

```bash
docker compose up --build
```

Open `http://localhost:8088`. The browser talks to Nginx, which proxies `/api/*` to the API gateway. The gateway is internal to Docker and is not exposed directly. Stop the stack with `docker compose down`; use `docker compose down -v` only when you intentionally want to remove all database and Redis data.

## API documentation (Swagger)

After the stack has started, open `http://localhost/swagger-ui.html` (or `http://localhost/swagger-ui/index.html`). This is one Swagger UI served by the API gateway. The `/swagger-ui/index.html` address redirects to the dedicated Swagger page. The same pages remain available on port `8088` for compatibility. Use its specification selector to switch between **Authentication**, **Events**, **Bookings and Payments**, and **Users**.

For protected endpoints, first call `POST /auth/login`, copy the returned access token, select **Authorize**, and enter `Bearer <access-token>`. Requests from Swagger go through the Docker gateway at `/api`, so they follow the same routing and JWT checks as the frontend. The raw specifications are also available at `/v3/api-docs/auth`, `/v3/api-docs/events`, `/v3/api-docs/booking`, and `/v3/api-docs/users`.

Razorpay and email are optional for a basic local startup. To enable them, create a root `.env` file (never commit it) with `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET`, `SMTP_HOST`, `SMTP_USERNAME`, and `SMTP_PASSWORD`. Change `SECURITY_JWT_SECRET` outside local development.

## Architecture

```
Browser -> Nginx web-client (:8088) -> API gateway (:8080, internal)
                                      |-> auth-service (:8081) -> auth-db
                                      |-> booking-service (:8082) -> booking-db + Redis
                                      |-> event-service (:8083) -> event-db
                                      |-> notification-service (:8084)
                                      `-> user-service (:8085) -> user-db
```

Every backend is a Spring Boot module. PostgreSQL stores durable service data; Redis stores short-lived, distributed seat holds. The gateway routes `/auth`, `/booking`, `/payment`, `/event`, `/notification`, and `/user` to their owning service. JWTs created by auth-service are validated by the gateway and service authorization checks protect ownership and admin-only actions.

## Application workflow

1. A user registers or signs in through auth-service. It creates the auth account and issues access/refresh tokens.
2. Organizers create and manage events through event-service. Event data includes status, price, and organizer UUID.
3. A signed-in attendee opens an event. The frontend fetches event information and booking seat availability through the gateway.
4. The attendee submits seats to `POST /booking`. booking-service verifies the user, event status, duplicate selections, and existing confirmed seats.
5. booking-service creates a `CREATED` booking, creates its booking-seat rows, and holds its selected seats for 12 minutes.
6. The frontend requests a Razorpay order from `POST /payment/order`, completes payment, then submits verification. Razorpay webhooks provide the server-to-server confirmation path.
7. A valid payment marks the payment successful, makes a permanent seat claim, confirms the booking, and releases its temporary holds. A failure, cancellation, or expiry releases the temporary holds.

## How double booking is prevented

The system uses two layers because each solves a different race:

1. **Short-lived distributed hold:** `SeatLockServiceImpl` stores one Redis key per `eventUuid + seat`, using Redis `SETNX` with a 12-minute TTL. Two checkout requests cannot acquire the same key. If a multi-seat request partially acquires locks, it rolls them back. Redis is mandatory in the Docker profile; the local-only in-memory fallback is disabled there.
2. **Permanent database guarantee:** on confirmation, `ConfirmedSeatService` inserts a row for each seat into `confirmed_seats`. PostgreSQL enforces a unique constraint on `(event_uuid, seat_number)`. Only one transaction can permanently claim a seat, even if a Redis key expires, a service restarts, or the booking service is scaled to multiple instances. A uniqueness conflict rolls the confirmation transaction back instead of allowing duplicate tickets.

Payment callbacks are idempotent: an already-confirmed booking is ignored. If payment arrives after a booking has expired, the service checks for another confirmed booking; it reconfirms only when the seats are still unclaimed, otherwise it marks the payment for refund. This protects the edge case where a payment provider callback is delayed.

## Booking states

`CREATED` means payment is pending and Redis holds the seats. `CONFIRMED` means payment succeeded and PostgreSQL owns the seats permanently. `FAILED`, `CANCELLED`, and `EXPIRED` are not permanent claims; their temporary holds are released. `REFUNDED` records a late-payment collision that was refunded.

## Development and operational notes

The backend is built by a multi-stage Maven image and the frontend by a Node/pnpm image served from Nginx. Persistent Docker volumes retain the four PostgreSQL databases and Redis AOF data across restarts. The Compose file uses service health checks for databases and Redis before dependent services start.

Useful commands:

```bash
docker compose up --build
docker compose logs -f booking-service
docker compose down
docker compose down -v
```

Do not place real credentials in `booking-service/.env.example`; rotate any keys that were ever committed. Keep production secrets in the deployment secret store or a local root `.env` excluded from Git.
