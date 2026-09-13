package com.ticketbooking.bookingservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "booking_seats",
        indexes = {
                @Index(name = "idx_booking_uuid", columnList = "booking_uuid")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_seat_id", nullable = false, unique = true)
    private Long id;

    @Column(name = "booking_uuid", nullable = false)
    private String bookingUUID;

    @Column(name = "seat_number", nullable = false)
    private String seatNumber;

    private BigDecimal price;

    private String eventUuid;
}