package com.ticketbooking.bookingservice.client;

import com.ticketbooking.bookingservice.dto.response.ApiResponse;
import com.ticketbooking.bookingservice.dto.response.UserResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${services.user.url:http://localhost:8085}")
public interface UserClient {
    @GetMapping("/users/{userUuid}")
    ApiResponse<UserResponseDto> getUser(@PathVariable("userUuid") String userUuid);
}
