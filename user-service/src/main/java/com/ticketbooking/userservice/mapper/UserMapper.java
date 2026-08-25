package com.ticketbooking.userservice.mapper;

import com.ticketbooking.userservice.dto.payload.CreateUserPayload;
import com.ticketbooking.userservice.dto.payload.UpdateUserPayload;
import com.ticketbooking.userservice.dto.response.UserResponse;
import com.ticketbooking.userservice.entity.User;

public final class UserMapper {
    private UserMapper() { }

    public static User toEntity(CreateUserPayload payload) {
        return User.builder()
                .firstName(payload.getFirstName().trim())
                .lastName(payload.getLastName().trim())
                .email(payload.getEmail().trim().toLowerCase())
                .phoneNumber(payload.getPhoneNumber().trim())
                .build();
    }

    public static void updateEntity(User user, UpdateUserPayload payload) {
        if (payload.getFirstName() != null) user.setFirstName(payload.getFirstName().trim());
        if (payload.getLastName() != null) user.setLastName(payload.getLastName().trim());
        if (payload.getEmail() != null) user.setEmail(payload.getEmail().trim().toLowerCase());
        if (payload.getPhoneNumber() != null) user.setPhoneNumber(payload.getPhoneNumber().trim());
    }

    public static UserResponse toResponse(User user) {
        return UserResponse.builder()
                .userUuid(user.getUserUuid()).firstName(user.getFirstName()).lastName(user.getLastName())
                .email(user.getEmail()).phoneNumber(user.getPhoneNumber()).role(user.getRole())
                .active(user.getActive()).createdAt(user.getCreatedAt()).updatedAt(user.getUpdatedAt()).build();
    }
}
