package com.ticketbooking.bookingservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AdminAnalyticsResponse {
    private long totalBookings;
    private long confirmedBookings;
    private long ticketsSold;
    private BigDecimal revenue;
}
