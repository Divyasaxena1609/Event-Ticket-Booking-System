package com.ticketbooking.bookingservice.service;

import com.ticketbooking.bookingservice.entity.Booking;
import com.ticketbooking.bookingservice.entity.ConfirmedSeat;
import com.ticketbooking.bookingservice.repository.BookingSeatRepository;
import com.ticketbooking.bookingservice.repository.ConfirmedSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfirmedSeatService {
    private final ConfirmedSeatRepository confirmedSeats;
    private final BookingSeatRepository bookingSeats;
    public void claimSeats(Booking booking) {
        try {
            confirmedSeats.saveAll(bookingSeats.findByBookingUUID(booking.getBookingUUID()).stream()
                    .map(seat -> new ConfirmedSeat(booking.getEventUuid(), seat.getSeatNumber(), booking.getBookingUUID())).toList());
            confirmedSeats.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalStateException("One or more seats were confirmed by another booking", exception);
        }
    }
}
