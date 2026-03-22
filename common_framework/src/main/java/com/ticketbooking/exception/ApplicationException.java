package com.ticketbooking.exception;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"cause", "stackTrace", "status", "suppressed", "localizedMessage", "headers"})
public class ApplicationException extends RuntimeException {
    private Integer code;
    private HttpStatus status;
    private String message;
    private Object details;
    private HttpHeaders headers;

    public ApplicationException(@NotNull ApplicationExceptionTypes applicationexceptiontypes,
                                HttpHeaders headers, Object details ) {
        this.code = applicationexceptiontypes.code();
        this.status = applicationexceptiontypes.status();
        this.message = applicationexceptiontypes.message();
        this.details = details;
        this.headers = headers;
    }

    public ApplicationException(@NotNull ApplicationExceptionTypes applicationexceptiontypes,
                                Object details ) {
        this.code = applicationexceptiontypes.code();
        this.status = applicationexceptiontypes.status();
        this.message = applicationexceptiontypes.message();
        this.details = details;
    }

    public ApplicationException(@NotNull ApplicationExceptionTypes applicationexceptiontypes,
                                HttpHeaders headers) {
        this.code = applicationexceptiontypes.code();
        this.status = applicationexceptiontypes.status();
        this.message = applicationexceptiontypes.message();
        this.headers = headers;
    }

    public ApplicationException(@NotNull ApplicationExceptionTypes applicationexceptiontypes) {
        this.code = applicationexceptiontypes.code();
        this.status = applicationexceptiontypes.status();
        this.message = applicationexceptiontypes.message();
    }

    public ApplicationException(Integer code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
