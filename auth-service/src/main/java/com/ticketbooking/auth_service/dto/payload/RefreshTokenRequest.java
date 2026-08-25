package com.ticketbooking.auth_service.dto.payload;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest { @NotBlank private String refreshToken; }
