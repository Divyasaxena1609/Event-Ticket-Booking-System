package com.ticketbooking.userservice.mapper;

import com.ticketbooking.exception.ApplicationException;
import com.ticketbooking.exception.ApplicationExceptionTypes;
import com.ticketbooking.userservice.dto.payload.CreateUserPayload;
import com.ticketbooking.userservice.dto.payload.UpdateUserPayload;
import com.ticketbooking.userservice.dto.response.UserResponse;
import com.ticketbooking.userservice.entity.User;
import com.ticketbooking.userservice.entity.UserRole;

public final class UserMapper {
    private UserMapper() { }

    public static User toEntity(CreateUserPayload payload) {
        UserRole role = payload.getRole() != null ? payload.getRole() : UserRole.USER;
        if (role == UserRole.ADMIN) {
            throw new ApplicationException(ApplicationExceptionTypes.ACCESS_DENIED, "Admin role cannot be self-assigned during registration. Admin roles must be configured directly in the database.");
        }
        return User.builder()
                .firstName(payload.getFirstName().trim())
                .lastName(payload.getLastName().trim())
                .email(payload.getEmail().trim().toLowerCase())
                .phoneNumber(payload.getPhoneNumber().trim())
                .role(role)
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
