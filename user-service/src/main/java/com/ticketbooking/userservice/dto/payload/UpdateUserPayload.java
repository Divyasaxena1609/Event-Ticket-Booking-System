package com.ticketbooking.userservice.dto.payload;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateUserPayload {
    private String firstName;
    private String lastName;
    @Email(message = "Email must be valid")
    private String email;
    @Pattern(regexp = "^[+]?[0-9]{7,15}$", message = "Phone number must contain 7 to 15 digits")
    private String phoneNumber;
}
