package com.ticketbooking.bookingservice.dto.payload;

import lombok.*;

import java.math.BigDecimal;
import java.util.*;

@Data
public class CreateBookingPayload {
    private String eventUuid;
    private String userId;
    private List<String> seats;
    private BigDecimal totalAmount;
    private BigDecimal ticketPrice;
}
