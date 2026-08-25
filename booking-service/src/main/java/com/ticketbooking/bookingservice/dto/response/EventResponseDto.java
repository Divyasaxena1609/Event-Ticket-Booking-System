package com.ticketbooking.bookingservice.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class EventResponseDto {
    private String eventUuid;
    private String title;
    private String category;
    private String organizerUserUuid;
    private Integer availableSeats;
    private BigDecimal ticketPrice;
}
