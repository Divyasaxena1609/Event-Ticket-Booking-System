package com.ticketbooking.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class ErrorController {
    private static final Logger log = LoggerFactory.getLogger(ErrorController.class);

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<Map<String, Object>> handleApplicationException(ApplicationException ex) {
        log.warn("ApplicationException handled: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("code", ex.getCode());
        response.put("status", ex.getStatus());
        response.put("message", ex.getMessage());
        response.put("details", ex.getDetails());

        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unhandled application exception", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 500);
        response.put("status", "INTERNAL_SERVER_ERROR");
        response.put("message", ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "Something went wrong");

        return ResponseEntity.internalServerError().body(response);
    }
}
