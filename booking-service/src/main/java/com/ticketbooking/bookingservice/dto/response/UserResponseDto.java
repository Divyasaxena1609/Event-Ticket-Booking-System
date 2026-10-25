package com.ticketbooking.bookingservice.dto.response;

import lombok.Data;

@Data
public class UserResponseDto {
    private String userUuid;
    private Boolean active;
    private String role;
}
