package com.ticketbooking.bookingservice.service.Impl;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import com.ticketbooking.bookingservice.dto.payload.VerifyPaymentPayload;
import com.ticketbooking.bookingservice.dto.response.CreatePaymentResponse;
import com.ticketbooking.bookingservice.entity.Booking;
import com.ticketbooking.bookingservice.entity.BookingSeat;
import com.ticketbooking.bookingservice.entity.BookingStatus;
import com.ticketbooking.bookingservice.entity.Payment;
import com.ticketbooking.bookingservice.entity.PaymentStatus;
import com.ticketbooking.bookingservice.repository.BookingRepository;
import com.ticketbooking.bookingservice.repository.BookingSeatRepository;
import com.ticketbooking.bookingservice.repository.PaymentRepository;
import com.ticketbooking.bookingservice.service.IPaymentService;
import com.ticketbooking.bookingservice.service.ISeatLockService;
import com.ticketbooking.bookingservice.security.UserAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements IPaymentService {

    private final RazorpayClient razorpayClient;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final PaymentRepository paymentRepository;
    private final ISeatLockService seatLockService;
    private final UserAuthorizationService userAuthorizationService;

    @Value("${razorpay.key-id}")
    private String key;

    @Value("${razorpay.key-secret}")
    private String secret;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    @Override
    public CreatePaymentResponse createOrder(String bookingUUID, String requesterUuid) throws Exception {

        Booking booking = bookingRepository.findByBookingUUID(bookingUUID)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        userAuthorizationService.requireOwnerOrAdmin(requesterUuid, booking.getUserId());
        if (booking.getStatus() != BookingStatus.CREATED) {
            throw new RuntimeException("Only pending bookings can be paid");
        }

        Payment existingPayment = paymentRepository
                .findByBookingUuid(bookingUUID)
                .orElse(null);

        if (existingPayment != null
                && existingPayment.getStatus() == PaymentStatus.SUCCESS) {
            throw new RuntimeException("Payment already completed");
        }

        JSONObject orderRequest = new JSONObject();

        orderRequest.put(
                "amount",
                booking.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue()
        );

        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", bookingUUID);

        Order order = razorpayClient.orders.create(orderRequest);

        Payment payment = existingPayment != null ? existingPayment : new Payment();

        payment.setBookingUuid(bookingUUID);
        payment.setAmount(booking.getTotalAmount());
        payment.setPaymentMethod("RAZORPAY");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setRazorpayOrderId(order.get("id"));

        paymentRepository.save(payment);

        return CreatePaymentResponse.builder()
                .orderId(order.get("id"))
                .amount(order.get("amount"))
                .currency(order.get("currency"))
                .key(key)
                .build();
    }

    @Override
    @Transactional
    public void verifyPayment(VerifyPaymentPayload payload, String requesterUuid) throws Exception {

        Payment payment = paymentRepository
                .findByRazorpayOrderId(payload.getRazorpayOrderId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        Booking booking = bookingRepository
                .findByBookingUUID(payment.getBookingUuid())
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        userAuthorizationService.requireOwnerOrAdmin(requesterUuid, booking.getUserId());

        JSONObject options = new JSONObject();

        options.put("razorpay_order_id", payload.getRazorpayOrderId());
        options.put("razorpay_payment_id", payload.getRazorpayPaymentId());
        options.put("razorpay_signature", payload.getRazorpaySignature());

        boolean isValid = Utils.verifyPaymentSignature(options, secret);

        if (!isValid) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new RuntimeException("Invalid payment signature");
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId(payload.getRazorpayPaymentId());
        payment.setRazorpayPaymentId(payload.getRazorpayPaymentId());
        payment.setRazorpaySignature(payload.getRazorpaySignature());
        paymentRepository.save(payment);

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        // Seat is now confirmed permanently in PostgreSQL; release temporary Redis lock
        seatLockService.releaseBookingLocks(booking.getEventUuid(), booking.getBookingUUID());
        log.info("Payment verified and booking confirmed for '{}'", booking.getBookingUUID());
    }

    @Override
    @Transactional
    public void processWebhook(String payload, String signature) throws Exception {
        if (signature == null || signature.isBlank()) {
            throw new SecurityException("Missing Razorpay webhook signature");
        }
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException("Razorpay webhook secret is not configured");
        }

        try {
            if (!Utils.verifyWebhookSignature(payload, signature, webhookSecret)) {
                throw new SecurityException("Invalid Razorpay webhook signature");
            }
        } catch (SecurityException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Razorpay webhook signature validation failed: {}", exception.getMessage());
            throw new SecurityException("Invalid Razorpay webhook signature", exception);
        }

        JSONObject eventObj = new JSONObject(payload);
        String eventType = eventObj.optString("event");
        JSONObject payloadObj = eventObj.optJSONObject("payload");
        if (payloadObj == null) return;

        JSONObject paymentEntity = payloadObj.optJSONObject("payment") != null
                ? payloadObj.getJSONObject("payment").optJSONObject("entity")
                : null;
        JSONObject orderEntity = payloadObj.optJSONObject("order") != null
                ? payloadObj.getJSONObject("order").optJSONObject("entity")
                : null;

        String orderId = null;
        String paymentId = null;

        if (paymentEntity != null) {
            orderId = paymentEntity.optString("order_id", null);
            paymentId = paymentEntity.optString("id", null);
        } else if (orderEntity != null) {
            orderId = orderEntity.optString("id", null);
        }

        if (orderId == null) {
            log.info("Webhook received for event '{}' without order_id", eventType);
            return;
        }

        Payment payment = paymentRepository.findByRazorpayOrderId(orderId).orElse(null);
        if (payment == null) {
            log.warn("No payment record found for webhook orderId '{}'", orderId);
            return;
        }

        Booking booking = bookingRepository.findByBookingUUID(payment.getBookingUuid()).orElse(null);
        if (booking == null) {
            log.warn("No booking record found for UUID '{}'", payment.getBookingUuid());
            return;
        }

        switch (eventType) {
            case "order.paid":
            case "payment.captured":
                handlePaymentSuccessWebhook(payment, booking, paymentId);
                break;

            case "payment.failed":
                handlePaymentFailedWebhook(payment, booking);
                break;

            default:
                log.info("Ignored Razorpay webhook event: '{}'", eventType);
                break;
        }
    }

    private void handlePaymentSuccessWebhook(Payment payment, Booking booking, String paymentId) {
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            log.info("Booking '{}' is already confirmed.", booking.getBookingUUID());
            return;
        }

        if (paymentId != null) {
            payment.setRazorpayPaymentId(paymentId);
            payment.setTransactionId(paymentId);
        }

        if (booking.getStatus() == BookingStatus.CREATED) {
            payment.setStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);

            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            seatLockService.releaseBookingLocks(booking.getEventUuid(), booking.getBookingUUID());
            log.info("Webhook successfully confirmed booking '{}'", booking.getBookingUUID());
            return;
        }

        // Late payment deduction on EXPIRED or CANCELLED booking
        if (booking.getStatus() == BookingStatus.EXPIRED || booking.getStatus() == BookingStatus.CANCELLED) {
            List<BookingSeat> seats = bookingSeatRepository.findByBookingUUID(booking.getBookingUUID());
            Set<String> requestedSeats = seats.stream().map(BookingSeat::getSeatNumber).collect(Collectors.toSet());

            // Check if seats were taken by another confirmed booking
            List<Booking> otherConfirmed = bookingRepository.findByEventUuid(booking.getEventUuid()).stream()
                    .filter(b -> b.getStatus() == BookingStatus.CONFIRMED && !b.getBookingUUID().equals(booking.getBookingUUID()))
                    .toList();

            boolean conflict = false;
            for (Booking oc : otherConfirmed) {
                List<BookingSeat> ocSeats = bookingSeatRepository.findByBookingUUID(oc.getBookingUUID());
                for (BookingSeat ocs : ocSeats) {
                    if (requestedSeats.contains(ocs.getSeatNumber())) {
                        conflict = true;
                        break;
                    }
                }
                if (conflict) break;
            }

            if (conflict) {
                // Auto-refund user via Razorpay API
                log.warn("Late payment for expired booking '{}', seats already booked by another user. Triggering auto-refund...", booking.getBookingUUID());
                try {
                    if (paymentId != null) {
                        JSONObject refundRequest = new JSONObject();
                        refundRequest.put("amount", payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue());
                        razorpayClient.payments.refund(paymentId, refundRequest);
                    }
                } catch (Exception e) {
                    log.error("Failed to execute Razorpay auto-refund for payment '{}': {}", paymentId, e.getMessage());
                }
                payment.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);

                booking.setStatus(BookingStatus.REFUNDED);
                bookingRepository.save(booking);
            } else {
                // Seats are still free: re-confirm the booking!
                payment.setStatus(PaymentStatus.SUCCESS);
                paymentRepository.save(payment);

                booking.setStatus(BookingStatus.CONFIRMED);
                bookingRepository.save(booking);

                seatLockService.releaseBookingLocks(booking.getEventUuid(), booking.getBookingUUID());
                log.info("Seats were still free; re-confirmed late payment for booking '{}'", booking.getBookingUUID());
            }
        }
    }

    private void handlePaymentFailedWebhook(Payment payment, Booking booking) {
        if (booking.getStatus() == BookingStatus.CONFIRMED || payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Ignoring stale payment.failed webhook for confirmed booking '{}'", booking.getBookingUUID());
            return;
        }

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        if (booking.getStatus() == BookingStatus.CREATED) {
            // Instantly release temporary seat locks upon failure
            seatLockService.releaseBookingLocks(booking.getEventUuid(), booking.getBookingUUID());
            booking.setStatus(BookingStatus.FAILED);
            bookingRepository.save(booking);
            log.info("Payment failed webhook processed. Immediately released seats for booking '{}'", booking.getBookingUUID());
        }
    }

    @Override
    @Transactional
    public void failPayment(String bookingUUID, String razorpayOrderId, String reason, String requesterUuid) throws Exception {
        Payment payment = null;
        if (razorpayOrderId != null && !razorpayOrderId.isBlank()) {
            payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId).orElse(null);
        }
        if (payment == null && bookingUUID != null && !bookingUUID.isBlank()) {
            payment = paymentRepository.findByBookingUuid(bookingUUID).orElse(null);
        }

        if (payment != null) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
        }

        String targetBookingUuid = bookingUUID != null ? bookingUUID : (payment != null ? payment.getBookingUuid() : null);
        if (targetBookingUuid != null) {
            Booking booking = bookingRepository.findByBookingUUID(targetBookingUuid).orElse(null);
            if (booking != null) {
                userAuthorizationService.requireOwnerOrAdmin(requesterUuid, booking.getUserId());
            }
            if (booking != null && booking.getStatus() != BookingStatus.CONFIRMED) {
                seatLockService.releaseBookingLocks(booking.getEventUuid(), targetBookingUuid);
                booking.setStatus(BookingStatus.FAILED);
                bookingRepository.save(booking);
                log.info("Payment explicitly marked as FAILED and seats released for booking '{}'. Reason: {}", targetBookingUuid, reason);
            }
        }
    }
}
