package com.ticketbooking.eventservice.service;

import com.ticketbooking.eventservice.dto.payload.CreateEventPayload;
import com.ticketbooking.eventservice.dto.payload.UpdateEventPayload;
import com.ticketbooking.eventservice.dto.response.CreateEventResponse;
import com.ticketbooking.eventservice.dto.response.UpdateEventResponse;
import com.ticketbooking.eventservice.entity.Event;


import java.util.List;

public interface IEventService {

     CreateEventResponse createEvent(CreateEventPayload request, String organizerUserUuid);

     List<CreateEventResponse>  getAllEvents();

     List<CreateEventResponse> getOrganizerEvents(String organizerUserUuid);

     CreateEventResponse getEventByUuid(String eventUuid);

     UpdateEventResponse updateEvent(String eventUuid, UpdateEventPayload payload);

     void deleteEvent(String eventUuid);

}
