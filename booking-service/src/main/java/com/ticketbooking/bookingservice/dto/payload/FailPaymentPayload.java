package com.ticketbooking.bookingservice.dto.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailPaymentPayload {
    private String bookingUUID;
    private String razorpayOrderId;
    private String reason;
}
