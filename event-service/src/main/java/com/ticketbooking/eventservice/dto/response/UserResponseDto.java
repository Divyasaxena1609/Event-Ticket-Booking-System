package com.ticketbooking.eventservice.dto.response;

import lombok.Data;

@Data
public class UserResponseDto {
    private Boolean active;
    private String role;
}
