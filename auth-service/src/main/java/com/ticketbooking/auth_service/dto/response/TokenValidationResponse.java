package com.ticketbooking.auth_service.dto.response;

import lombok.Builder;
import lombok.Value;

@Value @Builder
public class TokenValidationResponse { boolean valid; String subject; String email; String userUuid; }
