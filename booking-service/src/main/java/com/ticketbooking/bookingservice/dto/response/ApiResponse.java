package com.ticketbooking.bookingservice.dto.response;

import lombok.Data;

@Data
public class ApiResponse<T>{
    private Boolean success;

    private String message;

    private T data;
}
