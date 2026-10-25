package com.ticketbooking.eventservice.dto.payload;

import lombok.Data;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class UpdateEventPayload {
    private String title;

    private String description;

    private String category;

    private String organizerName;

    @FutureOrPresent private LocalDate eventDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private String venueName;

    private String city;

    private String address;

    @Positive private Integer totalSeats;

    @PositiveOrZero private BigDecimal ticketPrice;
}
