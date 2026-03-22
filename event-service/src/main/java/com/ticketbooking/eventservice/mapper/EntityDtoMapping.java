package com.ticketbooking.eventservice.mapper;

import com.ticketbooking.eventservice.dto.payload.CreateEventPayload;
import com.ticketbooking.eventservice.dto.payload.UpdateEventPayload;
import com.ticketbooking.eventservice.dto.response.CreateEventResponse;
import com.ticketbooking.eventservice.dto.response.UpdateEventResponse;
import com.ticketbooking.eventservice.entity.Event;

public class EntityDtoMapping {

    public static Event toEntity(CreateEventPayload dto){
        Event event = new Event();

        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setCategory(dto.getCategory());
        event.setOrganizerName(dto.getOrganizerName());
        event.setEventDate(dto.getEventDate());
        event.setStartTime(dto.getStartTime());
        event.setEndTime(dto.getEndTime());
        event.setVenueName(dto.getVenueName());
        event.setCity(dto.getCity());
        event.setAddress(dto.getAddress());
        event.setTotalSeats(dto.getTotalSeats());
        event.setTicketPrice(dto.getTicketPrice());

        return event;
    }

    public static void updateEntity(Event event, UpdateEventPayload dto){

        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setCategory(dto.getCategory());
        event.setOrganizerName(dto.getOrganizerName());
        event.setEventDate(dto.getEventDate());
        event.setStartTime(dto.getStartTime());
        event.setEndTime(dto.getEndTime());
        event.setVenueName(dto.getVenueName());
        event.setCity(dto.getCity());
        event.setAddress(dto.getAddress());
        event.setTotalSeats(dto.getTotalSeats());
        event.setTicketPrice(dto.getTicketPrice());
    }


    public static CreateEventResponse toDTO(Event event){

        CreateEventResponse dto = new CreateEventResponse();

        dto.setEventUuid(event.getEventUuid());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setCategory(event.getCategory());
        dto.setOrganizerName(event.getOrganizerName());
        dto.setEventDate(event.getEventDate());
        dto.setStartTime(event.getStartTime());
        dto.setEndTime(event.getEndTime());
        dto.setVenueName(event.getVenueName());
        dto.setCity(event.getCity());
        dto.setAddress(event.getAddress());
        dto.setTotalSeats(event.getTotalSeats());
        dto.setAvailableSeats(event.getAvailableSeats());
        dto.setTicketPrice(event.getTicketPrice());

        return dto;
    }

    public static UpdateEventResponse UpdateDTO(Event event){

        UpdateEventResponse dto = new UpdateEventResponse();

        dto.setEventUuid(event.getEventUuid());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setCategory(event.getCategory());
        dto.setOrganizerName(event.getOrganizerName());
        dto.setEventDate(event.getEventDate());
        dto.setStartTime(event.getStartTime());
        dto.setEndTime(event.getEndTime());
        dto.setVenueName(event.getVenueName());
        dto.setCity(event.getCity());
        dto.setAddress(event.getAddress());
        dto.setTotalSeats(event.getTotalSeats());
        dto.setAvailableSeats(event.getAvailableSeats());
        dto.setTicketPrice(event.getTicketPrice());

        return dto;
    }

}
