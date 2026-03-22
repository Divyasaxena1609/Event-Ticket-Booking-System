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
import java.util.List;

@Service
@RequiredArgsConstructor // create lombok constructor automatically for repository
public class EventServiceImpl implements IEventService {
    private final EventRepository eventRepository;

    @Override
    public CreateEventResponse createEvent(CreateEventPayload request){
        if(request.getEventDate() != null &&
                request.getEventDate().isBefore(java.time.LocalDate.now())) {

            throw new ApplicationException(ApplicationExceptionTypes.INVALID_EVENT_DATE);
        }

        Event event = EntityDtoMapping.toEntity(request); // convert DTO → Entity
        event.setAvailableSeats(event.getTotalSeats());
        eventRepository.save(event);
        return EntityDtoMapping.toDTO(event);
    }

    @Override
    public  List<CreateEventResponse> getAllEvents(){
        List<Event> events = eventRepository.findAll();

        events.forEach(this::updateEventStatus);

        return events.stream()
                .map(EntityDtoMapping::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
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

        if(event.getEventDate()
                .atTime(event.getEndTime())
                .isBefore(java.time.LocalDateTime.now())){

            event.setStatus(EventStatus.COMPLETED);
            eventRepository.save(event);
            return;
        }

        if(event.getAvailableSeats() == 0){
            event.setStatus(EventStatus.SOLD_OUT);
            eventRepository.save(event);
        }

    }
}
