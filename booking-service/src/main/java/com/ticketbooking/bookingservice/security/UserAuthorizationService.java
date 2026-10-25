package com.ticketbooking.bookingservice.security;

import com.ticketbooking.bookingservice.client.UserClient;
import com.ticketbooking.bookingservice.dto.response.ApiResponse;
import com.ticketbooking.bookingservice.dto.response.UserResponseDto;
import com.ticketbooking.exception.ApplicationException;
import com.ticketbooking.exception.ApplicationExceptionTypes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAuthorizationService {
    private final UserClient userClient;

    public boolean isAdmin(String userUuid) {
        return "ADMIN".equals(getActiveUser(userUuid).getRole());
    }

    public void requireAdmin(String userUuid) {
        if (!isAdmin(userUuid)) throw new ApplicationException(ApplicationExceptionTypes.ACCESS_DENIED);
    }

    public void requireOrganizer(String userUuid) {
        if (!"ORGANIZER".equals(getActiveUser(userUuid).getRole()))
            throw new ApplicationException(ApplicationExceptionTypes.ACCESS_DENIED);
    }

    public void requireOwnerOrAdmin(String requesterUuid, String ownerUuid) {
        if (!requesterUuid.equals(ownerUuid) && !isAdmin(requesterUuid))
            throw new ApplicationException(ApplicationExceptionTypes.ACCESS_DENIED);
    }

    public void requireActiveUser(String userUuid) {
        getActiveUser(userUuid);
    }

    private UserResponseDto getActiveUser(String userUuid) {
        if (userUuid == null || userUuid.isBlank()) throw new ApplicationException(ApplicationExceptionTypes.ACCESS_DENIED);
        try {
            ApiResponse<UserResponseDto> response = userClient.getUser(userUuid);
            if (response.getData() == null || !Boolean.TRUE.equals(response.getData().getActive()))
                throw new ApplicationException(ApplicationExceptionTypes.ACCESS_DENIED);
            return response.getData();
        } catch (ApplicationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApplicationException(ApplicationExceptionTypes.ACCESS_DENIED);
        }
    }
}
