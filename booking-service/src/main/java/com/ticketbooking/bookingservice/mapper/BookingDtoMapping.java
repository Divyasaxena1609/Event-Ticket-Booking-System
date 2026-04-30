package com.ticketbooking.bookingservice.mapper;

import com.ticketbooking.bookingservice.dto.payload.CreateBookingPayload;
import com.ticketbooking.bookingservice.dto.response.CreateBookingResponse;
import com.ticketbooking.bookingservice.entity.Booking;
import com.ticketbooking.bookingservice.entity.BookingSeat;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class BookingDtoMapping {
    public Booking toEntity(CreateBookingPayload payload) {
        Booking booking = new Booking();
        booking.setEventUuid(payload.getEventUuid());
        booking.setUserId(payload.getUserId());
        return booking;
    }

    public CreateBookingResponse toResponse(Booking booking) {
        CreateBookingResponse response = new CreateBookingResponse();
        response.setBookingUUID(booking.getBookingUUID());
        response.setStatus(booking.getStatus().name());
        return response;
    }

    public List<BookingSeat> toBookingSeats(CreateBookingPayload payload, String bookingUuid) {
        return payload.getSeats().stream().map(seat -> {
            BookingSeat bs = new BookingSeat();
            bs.setBookingUUID(bookingUuid);
            bs.setEventUuid(payload.getEventUuid());
            bs.setSeatNumber(seat);
            return bs;
        }).toList();
    }


}
