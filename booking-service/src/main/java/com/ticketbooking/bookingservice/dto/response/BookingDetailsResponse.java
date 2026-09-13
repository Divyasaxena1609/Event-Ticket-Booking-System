package com.ticketbooking.bookingservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
public class BookingDetailsResponse {
    private String bookingUUID;
    private String eventUuid;
    private BigDecimal totalAmount;
    private String status;
    private OffsetDateTime createdAt;
    private List<String> seats;
}
