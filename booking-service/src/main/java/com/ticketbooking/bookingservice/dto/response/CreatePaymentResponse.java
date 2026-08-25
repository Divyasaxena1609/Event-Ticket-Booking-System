package com.ticketbooking.bookingservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CreatePaymentResponse {
    private String orderId;
    private Integer amount;
    private String currency;
    private String key;
}
