package com.ticketbooking.auth_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {
    private String secret;
    private Duration accessTokenExpiration;
    private Duration refreshTokenExpiration;
}
