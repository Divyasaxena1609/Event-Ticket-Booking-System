package com.ticketbooking.eventservice.controller;

import com.ticketbooking.eventservice.dto.payload.CreateEventPayload;
import com.ticketbooking.eventservice.dto.payload.UpdateEventPayload;
import com.ticketbooking.eventservice.dto.response.CreateEventResponse;
import com.ticketbooking.eventservice.dto.response.UpdateEventResponse;

import com.ticketbooking.eventservice.service.IEventService;
import com.ticketbooking.eventservice.security.EventAuthorizationService;
import com.ticketbooking.model.ApiResponse;
import com.ticketbooking.model.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {
    private final IEventService eventService;
    private final EventAuthorizationService eventAuthorizationService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateEventResponse>> createEvent(@Valid @RequestBody CreateEventPayload request,
                                                                         @RequestHeader("X-User-Id") String requesterUuid){
        eventAuthorizationService.requireOrganizerOrAdmin(requesterUuid);
        CreateEventResponse response = eventService.createEvent(request, requesterUuid);

        return ResponseEntity.ok(
                ResponseBuilder.success(response, "Event created successfully")
        );

    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CreateEventResponse>>> getAllEvents(){
        List<CreateEventResponse> events = eventService.getAllEvents();
        return ResponseEntity.ok(ResponseBuilder.success(events, "events fetched successfully"));
    }

    @GetMapping("/organizer/me")
    public ResponseEntity<ApiResponse<List<CreateEventResponse>>> getMyEvents(
            @RequestHeader("X-User-Id") String requesterUuid) {
        eventAuthorizationService.requireOrganizerOrAdmin(requesterUuid);
        return ResponseEntity.ok(ResponseBuilder.success(eventService.getOrganizerEvents(requesterUuid), "Organizer events fetched successfully"));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<CreateEventResponse>> getEventByUuid(
            @PathVariable String uuid){
        CreateEventResponse event = eventService.getEventByUuid(uuid);
        return ResponseEntity.ok(ResponseBuilder.success(event, "event fetched successfully"));
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponse<UpdateEventResponse>> updateEvent(
            @PathVariable String uuid,
            @Valid @RequestBody UpdateEventPayload request,
            @RequestHeader("X-User-Id") String requesterUuid){

        eventAuthorizationService.requireEventOwnerOrAdmin(requesterUuid, uuid);

        UpdateEventResponse response = eventService.updateEvent(uuid, request);

        return ResponseEntity.ok(ResponseBuilder.success(response, "event updated successfully"));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<String>> deleteEvent(@PathVariable String uuid,
                                                           @RequestHeader("X-User-Id") String requesterUuid){
        eventAuthorizationService.requireEventOwnerOrAdmin(requesterUuid, uuid);
        eventService.deleteEvent(uuid);
        return ResponseEntity.ok(ResponseBuilder.success("Deleted", "event deleted successfully"));
    }
}
