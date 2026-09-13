package com.ticketbooking.userservice.service;

import com.ticketbooking.exception.ApplicationException;
import com.ticketbooking.exception.ApplicationExceptionTypes;
import com.ticketbooking.userservice.dto.payload.CreateUserPayload;
import com.ticketbooking.userservice.dto.response.UserResponse;
import com.ticketbooking.userservice.entity.User;
import com.ticketbooking.userservice.entity.UserRole;
import com.ticketbooking.userservice.repository.UserRepository;
import com.ticketbooking.userservice.service.Impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private CreateUserPayload basePayload;

    @BeforeEach
    void setUp() {
        basePayload = new CreateUserPayload();
        basePayload.setFirstName("Alex");
        basePayload.setLastName("Morgan");
        basePayload.setEmail("alex@example.com");
        basePayload.setPhoneNumber("+919876543210");
    }

    @Test
    void testCreateUser_DefaultUserRole() {
        when(userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumberAndDeletedAtIsNull(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            u.setUserUuid("user-uuid-1");
            return u;
        });

        UserResponse response = userService.createUser(basePayload);

        assertNotNull(response);
        assertEquals(UserRole.USER, response.getRole());
        assertEquals("alex@example.com", response.getEmail());
    }

    @Test
    void testCreateUser_OrganizerRole() {
        basePayload.setRole(UserRole.ORGANIZER);
        when(userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumberAndDeletedAtIsNull(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(2L);
            u.setUserUuid("organizer-uuid-1");
            return u;
        });

        UserResponse response = userService.createUser(basePayload);

        assertNotNull(response);
        assertEquals(UserRole.ORGANIZER, response.getRole());
    }

    @Test
    void testCreateUser_AdminRole_Denied() {
        basePayload.setRole(UserRole.ADMIN);
        when(userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumberAndDeletedAtIsNull(anyString())).thenReturn(false);

        ApplicationException ex = assertThrows(ApplicationException.class, () -> userService.createUser(basePayload));
        assertEquals(ApplicationExceptionTypes.ACCESS_DENIED.code(), ex.getCode());
        verify(userRepository, never()).save(any(User.class));
    }
}
