package com.ticketbooking.bookingservice.dto.payload;

import lombok.*;

import java.util.*;

@Data
public class CreateBookingPayload {
    private String eventUuid;
    private String userId;
    private List<String> seats;
}
