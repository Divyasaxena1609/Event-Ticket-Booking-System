package com.ticketbooking.bookingservice.service;

import com.ticketbooking.bookingservice.client.EventClient;
import com.ticketbooking.bookingservice.dto.payload.CreateBookingPayload;
import com.ticketbooking.bookingservice.dto.response.ApiResponse;
import com.ticketbooking.bookingservice.dto.response.CreateBookingResponse;
import com.ticketbooking.bookingservice.dto.response.EventResponseDto;
import com.ticketbooking.bookingservice.dto.response.SeatAvailabilityResponse;
import com.ticketbooking.bookingservice.entity.Booking;
import com.ticketbooking.bookingservice.entity.BookingSeat;
import com.ticketbooking.bookingservice.entity.BookingStatus;
import com.ticketbooking.bookingservice.mapper.BookingDtoMapping;
import com.ticketbooking.bookingservice.repository.BookingRepository;
import com.ticketbooking.bookingservice.repository.BookingSeatRepository;
import com.ticketbooking.bookingservice.security.UserAuthorizationService;
import com.ticketbooking.bookingservice.service.Impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingSeatRepository bookingSeatRepository;

    @Mock
    private BookingDtoMapping bookingMapper;

    @Mock
    private EventClient eventClient;

    @Mock
    private UserAuthorizationService userAuthorizationService;

    @Mock
    private ISeatLockService seatLockService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private EventResponseDto mockEvent;
    private ApiResponse<EventResponseDto> apiResponse;

    @BeforeEach
    void setUp() {
        mockEvent = new EventResponseDto();
        mockEvent.setEventUuid("event-123");
        mockEvent.setTitle("Grand Rock Concert");
        mockEvent.setCategory("CONCERT");
        mockEvent.setTicketPrice(new BigDecimal("750.00"));

        apiResponse = new ApiResponse<>();
        apiResponse.setSuccess(true);
        apiResponse.setData(mockEvent);
    }

    @Test
    void testCreateBooking_FixedPriceAndLockAcquired() {
        CreateBookingPayload payload = new CreateBookingPayload();
        payload.setEventUuid("event-123");
        payload.setUserId("user-456");
        payload.setSeats(Arrays.asList("PIT-1", "A1", "C5"));
        payload.setTicketPrice(new BigDecimal("750.00"));

        when(eventClient.getEvent("event-123")).thenReturn(apiResponse);
        when(bookingRepository.findByEventUuid("event-123")).thenReturn(Collections.emptyList());
        when(seatLockService.acquireLocks(eq("event-123"), anyList(), eq("user-456"), anyString(), any(Duration.class)))
                .thenReturn(true);

        Booking bookingEntity = new Booking();
        bookingEntity.setBookingUUID("booking-999");
        bookingEntity.setEventUuid("event-123");
        bookingEntity.setUserId("user-456");
        bookingEntity.setStatus(BookingStatus.CREATED);

        when(bookingMapper.toEntity(any(CreateBookingPayload.class))).thenReturn(bookingEntity);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<BookingSeat> seats = Arrays.asList(
                new BookingSeat(1L, "booking-999", "PIT-1", new BigDecimal("750.00"), "event-123"),
                new BookingSeat(2L, "booking-999", "A1", new BigDecimal("750.00"), "event-123"),
                new BookingSeat(3L, "booking-999", "C5", new BigDecimal("750.00"), "event-123")
        );
        when(bookingMapper.toBookingSeats(any(CreateBookingPayload.class), anyString(), eq(new BigDecimal("750.00"))))
                .thenReturn(seats);

        CreateBookingResponse responseDto = new CreateBookingResponse();
        responseDto.setBookingUUID("booking-999");
        responseDto.setStatus(BookingStatus.CREATED.name());
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(responseDto);

        CreateBookingResponse response = bookingService.createBooking(payload, "user-456");

        assertNotNull(response);
        assertEquals("booking-999", response.getBookingUUID());
        assertEquals("CREATED", response.getStatus());

        verify(seatLockService).acquireLocks(eq("event-123"), eq(payload.getSeats()), eq("user-456"), anyString(), any(Duration.class));
        verify(bookingRepository).save(argThat(b ->
                b.getTotalAmount().compareTo(new BigDecimal("2250.00")) == 0
        ));
        verify(bookingSeatRepository).saveAll(anyList());
    }

    @Test
    void testCreateBooking_RejectsWhenLockFails() {
        CreateBookingPayload payload = new CreateBookingPayload();
        payload.setEventUuid("event-123");
        payload.setUserId("user-456");
        payload.setSeats(Arrays.asList("A1", "A2"));

        when(eventClient.getEvent("event-123")).thenReturn(apiResponse);
        when(bookingRepository.findByEventUuid("event-123")).thenReturn(Collections.emptyList());
        when(bookingMapper.toEntity(any(CreateBookingPayload.class))).thenReturn(new Booking());
        when(seatLockService.acquireLocks(anyString(), anyList(), anyString(), anyString(), any(Duration.class)))
                .thenReturn(false); // Lock failed because another user held the lock

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                bookingService.createBooking(payload, "user-456")
        );

        assertTrue(ex.getMessage().contains("currently held by another user"));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testReleaseBooking_ReleasesLocksAndDeletesPendingSeats() {
        Booking booking = new Booking();
        booking.setBookingUUID("booking-999");
        booking.setEventUuid("event-123");
        booking.setUserId("user-456");
        booking.setStatus(BookingStatus.CREATED);

        when(bookingRepository.findByBookingUUID("booking-999")).thenReturn(Optional.of(booking));

        bookingService.releaseBooking("booking-999", "user-456");

        verify(seatLockService).releaseBookingLocks("event-123", "booking-999");
        verify(bookingSeatRepository).deleteByBookingUUID("booking-999");
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void testGetBookedSeats_IncludesActiveTemporaryLocks() {
        when(eventClient.getEvent("event-123")).thenReturn(apiResponse);
        when(bookingRepository.findByEventUuid("event-123")).thenReturn(Collections.emptyList());
        when(seatLockService.getLockedSeats("event-123")).thenReturn(new HashSet<>(Arrays.asList("A1", "A2")));

        SeatAvailabilityResponse availability = bookingService.getBookedSeats("event-123");

        assertNotNull(availability);
        assertEquals(2, availability.getBlockedSeats().size());
        assertTrue(availability.getBlockedSeats().contains("A1"));
        assertTrue(availability.getBlockedSeats().contains("A2"));
    }
}
