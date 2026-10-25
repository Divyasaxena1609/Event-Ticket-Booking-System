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

    public static final ApplicationExceptionTypes USER_NOT_FOUND =
            new ApplicationExceptionTypes(2001, HttpStatus.NOT_FOUND, "User not found.");

    public static final ApplicationExceptionTypes USER_ALREADY_EXISTS =
            new ApplicationExceptionTypes(2002, HttpStatus.CONFLICT, "A user with this email or phone number already exists.");

    public static final ApplicationExceptionTypes USER_INACTIVE =
            new ApplicationExceptionTypes(2003, HttpStatus.BAD_REQUEST, "User account is inactive.");

    public static final ApplicationExceptionTypes INVALID_USER_ROLE =
            new ApplicationExceptionTypes(2004, HttpStatus.BAD_REQUEST, "Invalid user role.");

    public static final ApplicationExceptionTypes ACCESS_DENIED =
            new ApplicationExceptionTypes(2005, HttpStatus.FORBIDDEN, "You do not have permission to perform this action.");

}
