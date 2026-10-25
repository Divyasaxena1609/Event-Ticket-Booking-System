package com.ticketbooking.eventservice.dto.payload;
import lombok.Data;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CreateEventPayload {
    @NotBlank private String title;

    private String description;

    private String category;

    private String organizerName;

    @NotNull @FutureOrPresent private LocalDate eventDate;

    private LocalTime startTime;

    private LocalTime endTime;

    @NotBlank private String venueName;

    @NotBlank private String city;

    private String address;

    @Positive private int totalSeats;

    @NotNull @PositiveOrZero private BigDecimal ticketPrice;
}
