package com.ticketbooking.bookingservice.controller;

import com.ticketbooking.bookingservice.dto.payload.CreateBookingPayload;
import com.ticketbooking.bookingservice.dto.response.CreateBookingResponse;
import com.ticketbooking.bookingservice.dto.response.BookingDetailsResponse;
import com.ticketbooking.bookingservice.dto.response.SeatAvailabilityResponse;
import com.ticketbooking.bookingservice.dto.response.AdminAnalyticsResponse;
import com.ticketbooking.bookingservice.entity.Booking;
import com.ticketbooking.bookingservice.service.IBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bookings")
public class BookingController {
    private final IBookingService bookingService;

    @PostMapping
    public CreateBookingResponse createBooking(@RequestBody CreateBookingPayload payload,
                                               @RequestHeader("X-User-Id") String requesterUuid) {
        return bookingService.createBooking(payload, requesterUuid);
    }

    @GetMapping("/{bookingUUID}")
    public CreateBookingResponse getBooking(@PathVariable String bookingUUID,
                                            @RequestHeader("X-User-Id") String requesterUuid) {
        return bookingService.getBooking(bookingUUID, requesterUuid);
    }

    @GetMapping("/user/{userId}")
    public List<BookingDetailsResponse> getUserBookings(@PathVariable String userId,
                                         @RequestHeader("X-User-Id") String requesterUuid) {
        return bookingService.getUserBookings(userId, requesterUuid);
    }

    @GetMapping("/event/{eventUuid}")
    public List<Booking> getEventBookings(@PathVariable String eventUuid,
                                          @RequestHeader("X-User-Id") String requesterUuid) {
        return bookingService.getEventBookings(eventUuid, requesterUuid);
    }

    @GetMapping("/event/{eventUuid}/seats")
    public SeatAvailabilityResponse getBookedSeats(@PathVariable String eventUuid) {
        return bookingService.getBookedSeats(eventUuid);
    }

    @PostMapping("/event/{eventUuid}/seat-prices")
    public java.util.Map<String, java.math.BigDecimal> calculateSeatPrices(@PathVariable String eventUuid,
                                                                            @RequestBody List<String> seats) {
        return bookingService.calculateSeatPrices(eventUuid, seats);
    }

    @GetMapping("/admin/analytics")
    public AdminAnalyticsResponse getAdminAnalytics(@RequestHeader("X-User-Id") String requesterUuid) {
        return bookingService.getAdminAnalytics(requesterUuid);
    }

    @PostMapping("/{bookingUUID}/release")
    public String releaseBooking(@PathVariable String bookingUUID,
                                 @RequestHeader(value = "X-User-Id", required = false) String requesterUuid) {
        bookingService.releaseBooking(bookingUUID, requesterUuid);
        return "Booking released successfully";
    }

    @PostMapping("/event/{eventUuid}/release-seats")
    public String releaseSeats(@PathVariable String eventUuid,
                               @RequestBody List<String> seats,
                               @RequestHeader(value = "X-User-Id", required = false) String requesterUuid) {
        bookingService.releaseSeats(eventUuid, seats, requesterUuid);
        return "Seats released successfully";
    }

    @DeleteMapping("/{bookingUUID}")
    public String cancelBooking(@PathVariable String bookingUUID,
                                @RequestHeader("X-User-Id") String requesterUuid) {
        bookingService.cancelBooking(bookingUUID, requesterUuid);
        return "Booking cancelled successfully";
    }

    @PostMapping("/{bookingUUID}/confirm")
    public String confirmBooking(@PathVariable String bookingUUID,
                                 @RequestHeader("X-User-Id") String requesterUuid) {
        bookingService.confirmBooking(bookingUUID, requesterUuid);
        return "Booking confirmed successfully";
    }

}
