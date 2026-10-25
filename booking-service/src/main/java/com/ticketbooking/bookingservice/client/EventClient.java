package com.ticketbooking.bookingservice.client;

import com.ticketbooking.bookingservice.dto.response.ApiResponse;
import com.ticketbooking.bookingservice.dto.response.EventResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "event-service", url = "${services.event.url:http://localhost:8083}")
public interface EventClient {

    @GetMapping("/events/{eventUuid}")
    ApiResponse<EventResponseDto> getEvent(
            @PathVariable("eventUuid") String eventUuid
    );
}
