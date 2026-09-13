package com.ticketbooking.auth_service.dto.payload;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank @Email
    private String email;
    @NotBlank @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String password;
    private String userUuid;
}
