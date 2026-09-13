package com.ticketbooking.bookingservice.dto.payload;

import lombok.Data;

@Data
public class VerifyPaymentPayload {
    private String razorpayOrderId;

    private String razorpayPaymentId;

    private String razorpaySignature;

}
