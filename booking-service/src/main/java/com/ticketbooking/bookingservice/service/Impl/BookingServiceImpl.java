package com.ticketbooking.bookingservice.service.Impl;

import com.ticketbooking.bookingservice.client.EventClient;
import com.ticketbooking.bookingservice.dto.payload.CreateBookingPayload;
import com.ticketbooking.bookingservice.dto.response.ApiResponse;
import com.ticketbooking.bookingservice.dto.response.CreateBookingResponse;
import com.ticketbooking.bookingservice.dto.response.BookingDetailsResponse;
import com.ticketbooking.bookingservice.dto.response.SeatAvailabilityResponse;
import com.ticketbooking.bookingservice.dto.response.EventResponseDto;
import com.ticketbooking.bookingservice.dto.response.AdminAnalyticsResponse;
import com.ticketbooking.bookingservice.entity.Booking;
import com.ticketbooking.bookingservice.entity.BookingSeat;
import com.ticketbooking.bookingservice.entity.BookingStatus;
import com.ticketbooking.bookingservice.mapper.BookingDtoMapping;
import com.ticketbooking.bookingservice.repository.BookingRepository;
import com.ticketbooking.bookingservice.repository.BookingSeatRepository;
import com.ticketbooking.bookingservice.service.IBookingService;
import com.ticketbooking.bookingservice.service.ISeatLockService;
import com.ticketbooking.bookingservice.security.UserAuthorizationService;
import com.ticketbooking.exception.ApplicationException;
import com.ticketbooking.exception.ApplicationExceptionTypes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements IBookingService {
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingDtoMapping bookingMapper;
    private final EventClient eventClient;
    private final UserAuthorizationService userAuthorizationService;
    private final ISeatLockService seatLockService;

    private static final Duration LOCK_TTL = Duration.ofMinutes(12);

    @Override
    @Transactional
    public CreateBookingResponse createBooking(CreateBookingPayload payload, String requesterUuid){
        userAuthorizationService.requireOwnerOrAdmin(requesterUuid, payload.getUserId());
        validateBookingUser(payload.getUserId());
        EventResponseDto event = null;
        try {
            ApiResponse<EventResponseDto> response = eventClient.getEvent(payload.getEventUuid());
            if (response != null && response.getData() != null) {
                event = response.getData();
            }
        } catch (Exception ignored) {}

        if (event == null) {
            event = new EventResponseDto();
            event.setEventUuid(payload.getEventUuid());
            event.setCategory("Event");
            event.setTitle("");
            event.setTicketPrice(payload.getTicketPrice() != null ? payload.getTicketPrice() : new BigDecimal("500"));
        }

        if (payload.getSeats() == null || payload.getSeats().isEmpty()) {
            throw new RuntimeException("Seats cannot be empty");
        }

        if (new HashSet<>(payload.getSeats()).size() != payload.getSeats().size()) {
            throw new RuntimeException("Duplicate seats specified in booking request");
        }

        // Check if any seat is already confirmed in PostgreSQL
        List<Booking> confirmedBookings = bookingRepository.findByEventUuid(payload.getEventUuid()).stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .toList();
        Set<String> confirmedSeats = new HashSet<>();
        for (Booking cb : confirmedBookings) {
            bookingSeatRepository.findByBookingUUID(cb.getBookingUUID()).forEach(s -> confirmedSeats.add(s.getSeatNumber()));
        }

        for (String seat : payload.getSeats()) {
            if (confirmedSeats.contains(seat)) {
                throw new RuntimeException("Seat already booked: " + seat);
            }
        }

        Booking booking = bookingMapper.toEntity(payload);
        String bookingUuid = booking.getBookingUUID() != null ? booking.getBookingUUID() : UUID.randomUUID().toString();
        booking.setBookingUUID(bookingUuid);

        // Acquire 5-minute TTL distributed seat locks
        boolean locked = seatLockService.acquireLocks(
                payload.getEventUuid(),
                payload.getSeats(),
                payload.getUserId(),
                bookingUuid,
                LOCK_TTL
        );

        if (!locked) {
            throw new RuntimeException("One or more selected seats are currently held by another user or already booked. Please choose different seats.");
        }

        BigDecimal unitPrice = (event != null && event.getTicketPrice() != null)
                ? event.getTicketPrice()
                : (payload.getTicketPrice() != null ? payload.getTicketPrice() : new BigDecimal("500"));

        booking.setTotalAmount(unitPrice.multiply(BigDecimal.valueOf(payload.getSeats().size())));
        booking = bookingRepository.save(booking);

        List<BookingSeat> seats =
                bookingMapper.toBookingSeats(payload, booking.getBookingUUID(), unitPrice);

        bookingSeatRepository.saveAll(seats);

        return bookingMapper.toResponse(booking);
    }

    public BigDecimal calculateSeatPrice(String category, String title, BigDecimal basePrice, String seatNumber) {
        if (basePrice == null) {
            basePrice = new BigDecimal("500");
        }
        return basePrice.setScale(0, java.math.RoundingMode.HALF_UP);
    }

    private void validateBookingUser(String userUuid) {
        userAuthorizationService.requireActiveUser(userUuid);
    }

    @Override
    public CreateBookingResponse getBooking(String bookingUUID, String requesterUuid) {
        Booking booking = bookingRepository.findByBookingUUID(bookingUUID)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        userAuthorizationService.requireOwnerOrAdmin(requesterUuid, booking.getUserId());
        return bookingMapper.toResponse(booking);
    }

    @Override
    public List<BookingDetailsResponse> getUserBookings(String userId, String requesterUuid) {
        userAuthorizationService.requireOwnerOrAdmin(requesterUuid, userId);
        return bookingRepository.findByUserId(userId).stream()
                .map(booking -> BookingDetailsResponse.builder()
                        .bookingUUID(booking.getBookingUUID())
                        .eventUuid(booking.getEventUuid())
                        .totalAmount(booking.getTotalAmount())
                        .status(booking.getStatus().name())
                        .createdAt(booking.getCreatedAt())
                        .seats(bookingSeatRepository.findByBookingUUID(booking.getBookingUUID()).stream()
                                .map(BookingSeat::getSeatNumber)
                                .toList())
                        .build())
                .toList();
    }

    @Override
    public List<Booking> getEventBookings(String eventUuid, String requesterUuid) {
        requireEventOwnerOrAdmin(requesterUuid, eventUuid);
        return bookingRepository.findByEventUuid(eventUuid);
    }

    @Override
    public SeatAvailabilityResponse getBookedSeats(String eventUuid) {
        List<Booking> bookings = bookingRepository.findByEventUuid(eventUuid);
        BigDecimal basePrice = new BigDecimal("500");
        String category = "Event";
        try {
            ApiResponse<EventResponseDto> response = eventClient.getEvent(eventUuid);
            if (response != null && response.getData() != null) {
                EventResponseDto event = response.getData();
                if (event.getTicketPrice() != null) basePrice = event.getTicketPrice();
                if (event.getCategory() != null) category = event.getCategory();
            }
        } catch (Exception ignored) {}

        List<String> confirmedSeats = bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
                .flatMap(booking -> bookingSeatRepository.findByBookingUUID(booking.getBookingUUID()).stream())
                .map(BookingSeat::getSeatNumber)
                .distinct()
                .toList();

        // Get currently active 5-minute locked seats from Redis / In-Memory
        Set<String> activeLocks = seatLockService.getLockedSeats(eventUuid);
        List<String> blockedSeats = activeLocks.stream()
                .filter(seat -> !confirmedSeats.contains(seat))
                .distinct()
                .toList();

        return SeatAvailabilityResponse.builder()
                .bookedSeats(confirmedSeats)
                .blockedSeats(blockedSeats)
                .baseTicketPrice(basePrice)
                .category(category)
                .build();
    }

    @Override
    public Map<String, BigDecimal> calculateSeatPrices(String eventUuid, List<String> seats) {
        BigDecimal basePrice = new BigDecimal("500");
        String category = "Event";
        String title = "";
        try {
            ApiResponse<EventResponseDto> response = eventClient.getEvent(eventUuid);
            if (response != null && response.getData() != null) {
                EventResponseDto event = response.getData();
                if (event.getTicketPrice() != null) basePrice = event.getTicketPrice();
                if (event.getCategory() != null) category = event.getCategory();
                if (event.getTitle() != null) title = event.getTitle();
            }
        } catch (Exception ignored) {}

        Map<String, BigDecimal> prices = new HashMap<>();
        if (seats != null) {
            for (String seat : seats) {
                prices.put(seat, calculateSeatPrice(category, title, basePrice, seat));
            }
        }
        return prices;
    }

    private void requireEventOwnerOrAdmin(String requesterUuid, String eventUuid) {
        if (userAuthorizationService.isAdmin(requesterUuid)) return;
        userAuthorizationService.requireOrganizer(requesterUuid);
        EventResponseDto event;
        try {
            ApiResponse<EventResponseDto> response = eventClient.getEvent(eventUuid);
            event = response.getData();
        } catch (Exception ex) {
            throw new ApplicationException(ApplicationExceptionTypes.ACCESS_DENIED);
        }
        if (event == null || !requesterUuid.equals(event.getOrganizerUserUuid()))
            throw new ApplicationException(ApplicationExceptionTypes.ACCESS_DENIED);
    }

    @Override
    @Transactional
    public void releaseBooking(String bookingUUID, String requesterUuid) {
        Booking booking = bookingRepository.findByBookingUUID(bookingUUID).orElse(null);
        if (booking == null) return;
        userAuthorizationService.requireOwnerOrAdmin(requesterUuid, booking.getUserId());

        if (booking.getStatus() == BookingStatus.CREATED) {
            seatLockService.releaseBookingLocks(booking.getEventUuid(), bookingUUID);
            bookingSeatRepository.deleteByBookingUUID(bookingUUID);
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            log.info("Released and cancelled unconfirmed booking '{}'", bookingUUID);
        }
    }

    @Override
    public void releaseSeats(String eventUuid, List<String> seats, String requesterUuid) {
        seatLockService.releaseLocks(eventUuid, seats, requesterUuid);
    }

    @Override
    @Transactional
    public void cancelBooking(String bookingUUID, String requesterUuid) {
        Booking booking = bookingRepository.findByBookingUUID(bookingUUID)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        userAuthorizationService.requireOwnerOrAdmin(requesterUuid, booking.getUserId());

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking already cancelled");
        }

        seatLockService.releaseBookingLocks(booking.getEventUuid(), bookingUUID);
        bookingSeatRepository.deleteByBookingUUID(bookingUUID);
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public void confirmBooking(String bookingUUID, String requesterUuid) {
        userAuthorizationService.requireAdmin(requesterUuid);

        Booking booking = bookingRepository.findByBookingUUID(bookingUUID)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.CREATED) {
            throw new RuntimeException("Only created bookings can be confirmed");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        // Seat is now confirmed permanently in PostgreSQL; release temporary Redis lock
        seatLockService.releaseBookingLocks(booking.getEventUuid(), bookingUUID);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAnalyticsResponse getAdminAnalytics(String requesterUuid) {
        userAuthorizationService.requireAdmin(requesterUuid);
        List<Booking> bookings = bookingRepository.findAll();
        List<Booking> confirmedBookings = bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
                .toList();

        return AdminAnalyticsResponse.builder()
                .totalBookings(bookings.size())
                .confirmedBookings(confirmedBookings.size())
                .ticketsSold(confirmedBookings.stream()
                        .mapToLong(booking -> bookingSeatRepository.findByBookingUUID(booking.getBookingUUID()).size())
                        .sum())
                .revenue(confirmedBookings.stream()
                        .map(Booking::getTotalAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .build();
    }
}
