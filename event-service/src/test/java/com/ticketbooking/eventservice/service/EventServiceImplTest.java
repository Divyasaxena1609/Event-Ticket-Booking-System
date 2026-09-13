package com.ticketbooking.eventservice.service;

import com.ticketbooking.eventservice.dto.payload.CreateEventPayload;
import com.ticketbooking.eventservice.dto.payload.UpdateEventPayload;
import com.ticketbooking.eventservice.dto.response.CreateEventResponse;
import com.ticketbooking.eventservice.dto.response.UpdateEventResponse;
import com.ticketbooking.eventservice.entity.Event;
import com.ticketbooking.eventservice.entity.EventStatus;
import com.ticketbooking.eventservice.repository.EventRepository;
import com.ticketbooking.eventservice.service.impl.EventServiceImpl;
import com.ticketbooking.exception.ApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventServiceImpl eventService;

    private CreateEventPayload createPayload;

    @BeforeEach
    void setUp() {
        createPayload = new CreateEventPayload();
        createPayload.setTitle("Tech Innovation Summit");
        createPayload.setDescription("Annual developer conference");
        createPayload.setCategory("CONFERENCE");
        createPayload.setOrganizerName("Tech Hub");
        createPayload.setEventDate(LocalDate.now().plusDays(10));
        createPayload.setStartTime(LocalTime.of(9, 0));
        createPayload.setEndTime(LocalTime.of(17, 0));
        createPayload.setVenueName("Convention Hall");
        createPayload.setCity("Bengaluru");
        createPayload.setAddress("MG Road");
        createPayload.setTotalSeats(500);
        createPayload.setTicketPrice(new BigDecimal("999.00"));
    }

    @Test
    void testCreateEvent_SetsInitialAvailableSeatsAndStatus() {
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateEventResponse response = eventService.createEvent(createPayload, "org-user-1");

        assertNotNull(response);
        assertEquals("Tech Innovation Summit", response.getTitle());
        assertEquals(500, response.getTotalSeats());
        assertEquals(500, response.getAvailableSeats());
        assertEquals(new BigDecimal("999.00"), response.getTicketPrice());
        assertEquals(EventStatus.UPCOMING, response.getStatus());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void testCreateEvent_RejectsPastEventDate() {
        createPayload.setEventDate(LocalDate.now().minusDays(1));

        assertThrows(ApplicationException.class, () ->
                eventService.createEvent(createPayload, "org-user-1")
        );
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void testUpdateEvent_UpdatesFieldsAndAdjustsSeats() {
        Event existingEvent = new Event();
        existingEvent.setEventUuid("event-abc");
        existingEvent.setTitle("Old Title");
        existingEvent.setTotalSeats(100);
        existingEvent.setAvailableSeats(100);
        existingEvent.setTicketPrice(new BigDecimal("500.00"));
        existingEvent.setOrganizerUserUuid("org-user-1");
        existingEvent.setStatus(EventStatus.UPCOMING);

        UpdateEventPayload updatePayload = new UpdateEventPayload();
        updatePayload.setTitle("New Title");
        updatePayload.setCategory("CONFERENCE");
        updatePayload.setOrganizerName("New Org");
        updatePayload.setEventDate(LocalDate.now().plusDays(20));
        updatePayload.setStartTime(LocalTime.of(10, 0));
        updatePayload.setEndTime(LocalTime.of(18, 0));
        updatePayload.setVenueName("New Venue");
        updatePayload.setCity("Mumbai");
        updatePayload.setAddress("BKC");
        updatePayload.setTotalSeats(150);
        updatePayload.setTicketPrice(new BigDecimal("600.00"));

        when(eventRepository.findByEventUuid("event-abc")).thenReturn(Optional.of(existingEvent));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateEventResponse response = eventService.updateEvent("event-abc", updatePayload);

        assertNotNull(response);
        assertEquals("New Title", response.getTitle());
        assertEquals(150, response.getTotalSeats());
        assertEquals(150, response.getAvailableSeats()); // 100 + (150 - 100)
        assertEquals(new BigDecimal("600.00"), response.getTicketPrice());
    }

    @Test
    void testGetEventByUuid_TransitionsToSoldOutWhenZeroSeats() {
        Event event = new Event();
        event.setEventUuid("event-abc");
        event.setTitle("Sold Out Show");
        event.setTotalSeats(100);
        event.setAvailableSeats(0);
        event.setEventDate(LocalDate.now().plusDays(5));
        event.setEndTime(LocalTime.of(22, 0));
        event.setStatus(EventStatus.UPCOMING);

        when(eventRepository.findByEventUuid("event-abc")).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateEventResponse response = eventService.getEventByUuid("event-abc");

        assertNotNull(response);
        assertEquals(EventStatus.SOLD_OUT, response.getStatus());
        verify(eventRepository).save(event);
    }
}
