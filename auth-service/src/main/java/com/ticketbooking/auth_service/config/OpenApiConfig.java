package com.ticketbooking.auth_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI authOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Ticket Booking - Authentication API").version("v1")
                        .description("Registration, sign-in, token refresh, password reset, and token validation."))
                .addServersItem(new Server().url("/api").description("Docker gateway"));
    }
}
