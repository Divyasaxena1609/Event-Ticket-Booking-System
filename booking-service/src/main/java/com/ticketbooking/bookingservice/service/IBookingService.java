package com.ticketbooking.bookingservice.service;

import com.ticketbooking.bookingservice.dto.payload.CreateBookingPayload;
import com.ticketbooking.bookingservice.dto.response.CreateBookingResponse;
import com.ticketbooking.bookingservice.entity.Booking;

import java.util.List;

public interface IBookingService {
    CreateBookingResponse createBooking(CreateBookingPayload payload);

    CreateBookingResponse getBooking(String bookingUUID);

    List<Booking> getUserBookings(String userId);

    List<Booking> getEventBookings(String eventUuid);

    void cancelBooking(String bookingUUID);

    void confirmBooking(String bookingUUID);
}
