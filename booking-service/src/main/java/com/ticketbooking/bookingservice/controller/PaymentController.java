package com.ticketbooking.bookingservice.controller;

import com.ticketbooking.bookingservice.dto.payload.CreatePaymentPayload;
import com.ticketbooking.bookingservice.dto.payload.FailPaymentPayload;
import com.ticketbooking.bookingservice.dto.payload.VerifyPaymentPayload;
import com.ticketbooking.bookingservice.dto.response.CreatePaymentResponse;
import com.ticketbooking.bookingservice.service.IPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final IPaymentService paymentService;

    @PostMapping("/create-order")
    public CreatePaymentResponse createOrder(
            @RequestBody CreatePaymentPayload request
    ) throws Exception {
        return paymentService.createOrder(request.getBookingUUID());
    }

    @PostMapping("/verify")
    public String verifyPayment(
            @RequestBody VerifyPaymentPayload payload
    ) throws Exception {
        paymentService.verifyPayment(payload);
        return "Payment verified successfully";
    }

    @PostMapping("/fail")
    public ResponseEntity<String> failPayment(
            @RequestBody FailPaymentPayload payload
    ) throws Exception {
        paymentService.failPayment(payload.getBookingUUID(), payload.getRazorpayOrderId(), payload.getReason());
        return ResponseEntity.ok("Payment failure recorded and seats released");
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature
    ) throws Exception {
        try {
            paymentService.processWebhook(payload, signature);
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (SecurityException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid webhook signature");
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Webhook processing is not configured");
        }
    }
}
