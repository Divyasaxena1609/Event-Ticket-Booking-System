package com.ticketbooking.eventservice.client;

import com.ticketbooking.eventservice.dto.response.UserResponseDto;
import com.ticketbooking.model.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${services.user.url:http://localhost:8085}")
public interface UserClient {
    @GetMapping("/users/{userUuid}")
    ApiResponse<UserResponseDto> getUser(@PathVariable("userUuid") String userUuid);
}
