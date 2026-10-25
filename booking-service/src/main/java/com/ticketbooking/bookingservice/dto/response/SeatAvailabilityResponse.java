package com.ticketbooking.bookingservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class SeatAvailabilityResponse {
    private List<String> bookedSeats;
    private List<String> blockedSeats;
    private BigDecimal baseTicketPrice;
    private String category;
    private Map<String, BigDecimal> seatPrices;
}
