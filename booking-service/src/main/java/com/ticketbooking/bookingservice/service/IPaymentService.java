package com.ticketbooking.bookingservice.service;

import com.ticketbooking.bookingservice.dto.payload.VerifyPaymentPayload;
import com.ticketbooking.bookingservice.dto.response.CreatePaymentResponse;

public interface IPaymentService {
    CreatePaymentResponse createOrder(String bookingUUID) throws Exception;
    void verifyPayment(VerifyPaymentPayload payload) throws Exception;
    void processWebhook(String payload, String signature) throws Exception;
    void failPayment(String bookingUUID, String razorpayOrderId, String reason) throws Exception;
}
