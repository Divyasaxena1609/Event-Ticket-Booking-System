package com.ticketbooking.userservice.service;

import com.ticketbooking.userservice.dto.payload.CreateUserPayload;
import com.ticketbooking.userservice.dto.payload.UpdateUserPayload;
import com.ticketbooking.userservice.dto.response.UserResponse;

import java.util.List;

public interface IUserService {
    UserResponse createUser(CreateUserPayload payload);

    UserResponse getUser(String userUuid);

    UserResponse getUserByEmail(String email);

    List<UserResponse> getAllUsers();

    UserResponse updateUser(String userUuid, UpdateUserPayload payload);

    void deleteUser(String userUuid);

    void activateUser(String userUuid);

    void deactivateUser(String userUuid);

    void updateRole(String userUuid, String role, String requesterUuid);
}
