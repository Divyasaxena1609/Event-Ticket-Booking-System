package com.ticketbooking.eventservice.security;

import com.ticketbooking.eventservice.client.UserClient;
import com.ticketbooking.eventservice.dto.response.UserResponseDto;
import com.ticketbooking.eventservice.entity.Event;
import com.ticketbooking.eventservice.repository.EventRepository;
import com.ticketbooking.exception.ApplicationException;
import com.ticketbooking.exception.ApplicationExceptionTypes;
import com.ticketbooking.model.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventAuthorizationService {
    private final UserClient userClient;
    private final EventRepository eventRepository;

    public void requireAdmin(String userUuid) {
        if (!"ADMIN".equals(getActiveUser(userUuid).getRole()))
            throw new ApplicationException(ApplicationExceptionTypes.ACCESS_DENIED);
    }

    public void requireOrganizerOrAdmin(String userUuid) {
        String role = getActiveUser(userUuid).getRole();
        if (!"ADMIN".equals(role) && !"ORGANIZER".equals(role))
            throw new ApplicationException(ApplicationExceptionTypes.ACCESS_DENIED);
    }

    public void requireEventOwnerOrAdmin(String userUuid, String eventUuid) {
        Event event = eventRepository.findByEventUuid(eventUuid)
                .orElseThrow(() -> new ApplicationException(ApplicationExceptionTypes.EVENT_NOT_FOUND));
        UserResponseDto user = getActiveUser(userUuid);
        boolean isOwner = "ORGANIZER".equals(user.getRole()) && userUuid.equals(event.getOrganizerUserUuid());
        if (!"ADMIN".equals(user.getRole()) && !isOwner)
            throw new ApplicationException(ApplicationExceptionTypes.ACCESS_DENIED);
    }

    private UserResponseDto getActiveUser(String userUuid) {
        if (userUuid == null || userUuid.isBlank()) throw new ApplicationException(ApplicationExceptionTypes.ACCESS_DENIED);
        try {
            ApiResponse<UserResponseDto> response = userClient.getUser(userUuid);
            UserResponseDto user = response.getData();
            if (user == null || !Boolean.TRUE.equals(user.getActive()))
                throw new ApplicationException(ApplicationExceptionTypes.ACCESS_DENIED);
            return user;
        } catch (ApplicationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApplicationException(ApplicationExceptionTypes.ACCESS_DENIED);
        }
    }
}
