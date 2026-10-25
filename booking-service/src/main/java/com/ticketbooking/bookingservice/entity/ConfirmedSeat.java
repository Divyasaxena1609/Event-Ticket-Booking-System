package com.ticketbooking.bookingservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "confirmed_seats", uniqueConstraints = @UniqueConstraint(
        name = "uk_confirmed_event_seat", columnNames = {"event_uuid", "seat_number"}))
@Getter @Setter @NoArgsConstructor
public class ConfirmedSeat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_uuid", nullable = false, updatable = false)
    private String eventUuid;
    @Column(name = "seat_number", nullable = false, updatable = false)
    private String seatNumber;
    @Column(name = "booking_uuid", nullable = false, updatable = false)
    private String bookingUuid;
    public ConfirmedSeat(String eventUuid, String seatNumber, String bookingUuid) {
        this.eventUuid = eventUuid; this.seatNumber = seatNumber; this.bookingUuid = bookingUuid;
    }
}
