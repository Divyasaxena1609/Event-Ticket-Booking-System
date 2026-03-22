package com.ticketbooking.model;

public class ResponseBuilder {
        public static <T> ApiResponse<T> success(T data, String message) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message(message)
                    .data(data)
                    .build();
        }

        public static <T> ApiResponse<T> success(T data) {
            return success(data, "Success");
        }
}
