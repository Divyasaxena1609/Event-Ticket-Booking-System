package com.ticketbooking.bookingservice.controller;

import com.ticketbooking.bookingservice.dto.payload.CreateBookingPayload;
import com.ticketbooking.bookingservice.dto.response.CreateBookingResponse;
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
    public CreateBookingResponse createBooking(@RequestBody CreateBookingPayload payload) {
        return bookingService.createBooking(payload);
    }

    @GetMapping("/{bookingUUID}")
    public CreateBookingResponse getBooking(@PathVariable String bookingUUID) {
        return bookingService.getBooking(bookingUUID);
    }

    @GetMapping("/user/{userId}")
    public List<Booking> getUserBookings(@PathVariable String userId) {
        return bookingService.getUserBookings(userId);
    }

    @GetMapping("/event/{eventUuid}")
    public List<Booking> getEventBookings(@PathVariable String eventUuid) {
        return bookingService.getEventBookings(eventUuid);
    }

    @DeleteMapping("/{bookingUUID}")
    public String cancelBooking(@PathVariable String bookingUUID) {
        bookingService.cancelBooking(bookingUUID);
        return "Booking cancelled successfully";
    }

    @PostMapping("/{bookingUUID}/confirm")
    public String confirmBooking(@PathVariable String bookingUUID) {
        bookingService.confirmBooking(bookingUUID);
        return "Booking confirmed successfully";
    }

}
