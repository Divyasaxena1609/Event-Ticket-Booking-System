package com.ticketbooking.userservice.controller;

import com.ticketbooking.model.ApiResponse;
import com.ticketbooking.model.ResponseBuilder;
import com.ticketbooking.userservice.dto.payload.CreateUserPayload;
import com.ticketbooking.userservice.dto.payload.UpdateUserPayload;
import com.ticketbooking.userservice.dto.response.UserResponse;
import com.ticketbooking.userservice.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final IUserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserPayload payload
    ) {
        return ResponseEntity.ok(ResponseBuilder.success(userService.createUser(payload), "User created successfully"));
    }

    @GetMapping("/{userUuid}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @PathVariable String userUuid
    ) {
        return ResponseEntity.ok(ResponseBuilder.success(userService.getUser(userUuid)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(ResponseBuilder.success(userService.getAllUsers()));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByEmail(
            @PathVariable String email
    ) {
        return ResponseEntity.ok(ResponseBuilder.success(userService.getUserByEmail(email)));
    }

    @PutMapping("/{userUuid}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable String userUuid,
            @Valid @RequestBody UpdateUserPayload payload
    ) {
        return ResponseEntity.ok(ResponseBuilder.success(userService.updateUser(userUuid, payload), "User updated successfully"));
    }

    @DeleteMapping("/{userUuid}")
    public ResponseEntity<ApiResponse<String>> deleteUser(
            @PathVariable String userUuid
    ) {
        userService.deleteUser(userUuid);
        return ResponseEntity.ok(ResponseBuilder.success("Deleted", "User deleted successfully"));
    }

    @PatchMapping("/{userUuid}/activate")
    public ResponseEntity<ApiResponse<String>> activateUser(
            @PathVariable String userUuid
    ) {
        userService.activateUser(userUuid);
        return ResponseEntity.ok(ResponseBuilder.success("Activated", "User activated successfully"));
    }

    @PatchMapping("/{userUuid}/deactivate")
    public ResponseEntity<ApiResponse<String>> deactivateUser(
            @PathVariable String userUuid
    ) {
        userService.deactivateUser(userUuid);
        return ResponseEntity.ok(ResponseBuilder.success("Deactivated", "User deactivated successfully"));
    }

    @PatchMapping("/{userUuid}/role/{role}")
    public ResponseEntity<ApiResponse<String>> updateRole(
            @PathVariable String userUuid,
            @PathVariable String role,
            @RequestHeader("X-User-Id") String requesterUuid
    ) {
        userService.updateRole(userUuid, role, requesterUuid);
        return ResponseEntity.ok(ResponseBuilder.success("Updated", "User role updated successfully"));
    }
}
