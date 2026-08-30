package com.ticketbooking.eventservice.service.impl;

import com.ticketbooking.exception.ApplicationException;
import com.ticketbooking.exception.ApplicationExceptionTypes;
import com.ticketbooking.eventservice.dto.payload.CreateEventPayload;
import com.ticketbooking.eventservice.dto.payload.UpdateEventPayload;
import com.ticketbooking.eventservice.dto.response.CreateEventResponse;
import com.ticketbooking.eventservice.dto.response.UpdateEventResponse;
import com.ticketbooking.eventservice.entity.Event;
import com.ticketbooking.eventservice.entity.EventStatus;
import com.ticketbooking.eventservice.mapper.EntityDtoMapping;
import com.ticketbooking.eventservice.repository.EventRepository;
import com.ticketbooking.eventservice.service.IEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements IEventService {
    private final EventRepository eventRepository;

    @Override
    public CreateEventResponse createEvent(CreateEventPayload request, String organizerUserUuid){
        if(request.getEventDate() != null &&
                request.getEventDate().isBefore(java.time.LocalDate.now())) {

            throw new ApplicationException(ApplicationExceptionTypes.INVALID_EVENT_DATE);
        }

        Event event = EntityDtoMapping.toEntity(request); // convert DTO → Entity
        event.setOrganizerUserUuid(organizerUserUuid);
        event.setAvailableSeats(event.getTotalSeats());
        if (event.getStatus() == null) {
            event.setStatus(EventStatus.UPCOMING);
        }
        eventRepository.save(event);
        return EntityDtoMapping.toDTO(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreateEventResponse> getAllEvents(){
        return eventRepository.findByDeletedAtIsNullAndEventDateGreaterThanEqualAndStatusNot(LocalDate.now(), EventStatus.CANCELLED).stream()
                .map(EntityDtoMapping::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreateEventResponse> getOrganizerEvents(String organizerUserUuid) {
        return eventRepository.findByOrganizerUserUuidAndDeletedAtIsNull(organizerUserUuid).stream()
                .map(EntityDtoMapping::toDTO)
                .toList();
    }

    @Transactional
    @Override
    public CreateEventResponse getEventByUuid(String eventUuid){

        Event event = eventRepository.findByEventUuid(eventUuid)
                .orElseThrow(() -> new ApplicationException(ApplicationExceptionTypes.EVENT_NOT_FOUND));

        updateEventStatus(event);

        return EntityDtoMapping.toDTO(event);
    }

    @Override
    public UpdateEventResponse updateEvent(String eventUuid, UpdateEventPayload payload) {

        Event event = eventRepository.findByEventUuid(eventUuid)
                .orElseThrow(() -> new ApplicationException(ApplicationExceptionTypes.EVENT_NOT_FOUND));

        if(event.getStatus() == EventStatus.CANCELLED){
            throw new ApplicationException(ApplicationExceptionTypes.EVENT_ALREADY_CANCELLED);
        }

        if(event.getStatus() == EventStatus.COMPLETED){
            throw new ApplicationException(ApplicationExceptionTypes.EVENT_ALREADY_COMPLETED);
        }

        if (payload.getEventDate() != null && payload.getEventDate().isBefore(LocalDate.now())) {
            throw new ApplicationException(ApplicationExceptionTypes.INVALID_EVENT_DATE);
        }

        EntityDtoMapping.updateEntity(event, payload);

        Event updatedEvent = eventRepository.save(event);

        return EntityDtoMapping.UpdateDTO(updatedEvent);
    }

    @Override
    public void deleteEvent(String eventUuid){
        Event event = eventRepository.findByEventUuid(eventUuid)
                .orElseThrow(() -> new ApplicationException(ApplicationExceptionTypes.EVENT_NOT_FOUND));

        if(event.getStatus() == EventStatus.CANCELLED){
            throw new ApplicationException(ApplicationExceptionTypes.EVENT_ALREADY_CANCELLED);
        }

        event.setStatus(EventStatus.CANCELLED);
        event.setDeletedAt(OffsetDateTime.now());
        eventRepository.save(event);
    }
    
    private void updateEventStatus(Event event) {

        if(event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.COMPLETED){
            return;
        }

        if (event.getEventDate() != null && event.getEndTime() != null && event.getEventDate()
                .atTime(event.getEndTime())
                .isBefore(java.time.LocalDateTime.now())){

            event.setStatus(EventStatus.COMPLETED);
            eventRepository.save(event);
            return;
        }

        if (Integer.valueOf(0).equals(event.getAvailableSeats())) {
            event.setStatus(EventStatus.SOLD_OUT);
            eventRepository.save(event);
        }

    }
}
