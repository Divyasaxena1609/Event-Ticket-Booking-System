package com.ticketbooking.bookingservice.service.Impl;

import com.ticketbooking.bookingservice.client.EventClient;
import com.ticketbooking.bookingservice.dto.payload.CreateBookingPayload;
import com.ticketbooking.bookingservice.dto.response.CreateBookingResponse;
import com.ticketbooking.bookingservice.dto.response.EventResponseDto;
import com.ticketbooking.bookingservice.entity.Booking;
import com.ticketbooking.bookingservice.entity.BookingSeat;
import com.ticketbooking.bookingservice.entity.BookingStatus;
import com.ticketbooking.bookingservice.mapper.BookingDtoMapping;
import com.ticketbooking.bookingservice.repository.BookingRepository;
import com.ticketbooking.bookingservice.repository.BookingSeatRepository;
import com.ticketbooking.bookingservice.service.IBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements IBookingService {
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingDtoMapping bookingMapper;
    private final EventClient eventClient;

    @Override
    @Transactional
    public CreateBookingResponse createBooking(CreateBookingPayload payload){
        EventResponseDto event;
        try {
            event = eventClient.getEvent(payload.getEventUuid());
        } catch (Exception e) {
            throw new RuntimeException("Event not found");
        }

        if (payload.getSeats() == null || payload.getSeats().isEmpty()) {
            throw new RuntimeException("Seats cannot be empty");
        }

        for (String seat : payload.getSeats()) {
            boolean exists = bookingSeatRepository
                    .existsByEventUuidAndSeatNumber(payload.getEventUuid(), seat);

            if (exists) {
                throw new RuntimeException("Seat already booked: " + seat);
            }
        }

        Booking booking = bookingMapper.toEntity(payload);

        booking.setTotalAmount(
                event.getTicketPrice().multiply(
                        BigDecimal.valueOf(payload.getSeats().size())
                )
        );

        booking = bookingRepository.save(booking);

        List<BookingSeat> seats =
                bookingMapper.toBookingSeats(payload, booking.getBookingUUID());

        bookingSeatRepository.saveAll(seats);

        return bookingMapper.toResponse(booking);
    }

    @Override
    public CreateBookingResponse getBooking(String bookingUUID) {
        Booking booking = bookingRepository.findByBookingUUID(bookingUUID)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        return bookingMapper.toResponse(booking);
    }

    @Override
    public List<Booking> getUserBookings(String userId) {
        return bookingRepository.findByUserId(userId);
    }

    @Override
    public List<Booking> getEventBookings(String eventUuid) {
        return bookingRepository.findByEventUuid(eventUuid);
    }

    @Override
    @Transactional
    public void cancelBooking(String bookingUUID) {

        Booking booking = bookingRepository.findByBookingUUID(bookingUUID)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking already cancelled");
        }

        bookingSeatRepository.deleteByBookingUUID(bookingUUID);

        booking.setStatus(BookingStatus.CANCELLED);

        bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public void confirmBooking(String bookingUUID) {

        Booking booking = bookingRepository.findByBookingUUID(bookingUUID)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.CREATED) {
            throw new RuntimeException("Only created bookings can be confirmed");
        }

        booking.setStatus(BookingStatus.CONFIRMED);

        bookingRepository.save(booking);
    }
}
