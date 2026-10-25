# EventHorizon web client

React + Vite + Tailwind UI for the Event Ticket Booking System.

```bash
pnpm install
pnpm dev
```

Set `VITE_API_URL` to the API gateway URL (defaults to `http://localhost:8080`). The current UI uses polished local demo data for the event catalogue while `src/lib/api.js` contains the gateway integration surface for the backend services.

The Razorpay payment selection is ready for connecting to a server-created Razorpay order; add `VITE_RAZORPAY_KEY_ID` and invoke Razorpay Checkout only after the booking service creates that order.
