package com.ticketbooking.bookingservice.service;

import com.ticketbooking.bookingservice.dto.payload.CreateBookingPayload;
import com.ticketbooking.bookingservice.dto.response.CreateBookingResponse;
import com.ticketbooking.bookingservice.dto.response.BookingDetailsResponse;
import com.ticketbooking.bookingservice.dto.response.SeatAvailabilityResponse;
import com.ticketbooking.bookingservice.dto.response.AdminAnalyticsResponse;
import com.ticketbooking.bookingservice.entity.Booking;

import java.util.List;

public interface IBookingService {
    CreateBookingResponse createBooking(CreateBookingPayload payload, String requesterUuid);

    CreateBookingResponse getBooking(String bookingUUID, String requesterUuid);

    List<BookingDetailsResponse> getUserBookings(String userId, String requesterUuid);

    List<Booking> getEventBookings(String eventUuid, String requesterUuid);

    SeatAvailabilityResponse getBookedSeats(String eventUuid);

    java.util.Map<String, java.math.BigDecimal> calculateSeatPrices(String eventUuid, List<String> seats);

    void cancelBooking(String bookingUUID, String requesterUuid);

    void releaseBooking(String bookingUUID, String requesterUuid);

    void releaseSeats(String eventUuid, List<String> seats, String requesterUuid);

    void confirmBooking(String bookingUUID, String requesterUuid);

    AdminAnalyticsResponse getAdminAnalytics(String requesterUuid);
}
