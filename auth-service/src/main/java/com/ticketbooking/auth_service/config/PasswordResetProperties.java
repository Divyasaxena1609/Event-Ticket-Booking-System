package com.ticketbooking.auth_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "app.password-reset")
public class PasswordResetProperties {
    private String frontendUrl = "http://localhost:5173";
    private String from;
    private Duration expiration = Duration.ofMinutes(30);
}
