package com.ticketbooking.exception;

import org.springframework.http.HttpStatus;

public record ApplicationExceptionTypes(Integer code, HttpStatus status, String message){

    public static final ApplicationExceptionTypes EVENT_NOT_FOUND =
            new ApplicationExceptionTypes(1001, HttpStatus.NOT_FOUND,
                    "Event not found.");

    public static final ApplicationExceptionTypes EVENT_ALREADY_CANCELLED =
            new ApplicationExceptionTypes(1002, HttpStatus.BAD_REQUEST,
                    "Event is already cancelled.");

    public static final ApplicationExceptionTypes EVENT_ALREADY_COMPLETED =
            new ApplicationExceptionTypes(1003, HttpStatus.BAD_REQUEST,
                    "Event is already completed.");

    public static final ApplicationExceptionTypes INVALID_EVENT_DATE =
            new ApplicationExceptionTypes(1004, HttpStatus.BAD_REQUEST,
                    "Invalid event dates.");

    public static final ApplicationExceptionTypes EVENT_SOLD_OUT =
            new ApplicationExceptionTypes(1005, HttpStatus.BAD_REQUEST,
                    "Tickets for this event are sold out.");

}
