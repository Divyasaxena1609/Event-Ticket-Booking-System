package com.ticketbooking.auth_service.controller;

import com.ticketbooking.auth_service.dto.payload.LoginRequest;
import com.ticketbooking.auth_service.dto.payload.RefreshTokenRequest;
import com.ticketbooking.auth_service.dto.payload.RegisterRequest;
import com.ticketbooking.auth_service.dto.response.TokenResponse;
import com.ticketbooking.auth_service.dto.response.TokenValidationResponse;
import com.ticketbooking.auth_service.exception.AuthException;
import com.ticketbooking.auth_service.service.AuthService;
import com.ticketbooking.model.ApiResponse;
import com.ticketbooking.model.ResponseBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthServiceController {
    private final AuthService authService;

    @GetMapping("/ping")
    public String ping() { return "Auth service is UP"; }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TokenResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(ResponseBuilder.success(authService.register(request), "Account registered"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ResponseBuilder.success(authService.login(request), "Authenticated"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ResponseBuilder.success(authService.refresh(request), "Token refreshed"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ResponseBuilder.success(null, "Logged out"));
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<TokenValidationResponse>> validate(@RequestHeader("Authorization") String authorization) {
        if (!authorization.startsWith("Bearer ")) throw new AuthException(HttpStatus.UNAUTHORIZED, "Authorization header must use Bearer authentication");
        return ResponseEntity.ok(ResponseBuilder.success(authService.validate(authorization.substring(7))));
    }
}
