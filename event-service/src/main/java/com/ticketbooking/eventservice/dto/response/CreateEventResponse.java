package com.ticketbooking.eventservice.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CreateEventResponse {

    private String eventUuid;

    private String title;

    private String description;

    private String category;

    private String organizerName;

    private LocalDate eventDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private String venueName;

    private String city;

    private String address;

    private int totalSeats;

    private Integer availableSeats;

    private BigDecimal ticketPrice;
}
