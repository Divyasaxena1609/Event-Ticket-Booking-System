package com.ticketbooking.userservice.service.Impl;

import com.ticketbooking.exception.ApplicationException;
import com.ticketbooking.exception.ApplicationExceptionTypes;
import com.ticketbooking.userservice.dto.payload.CreateUserPayload;
import com.ticketbooking.userservice.dto.payload.UpdateUserPayload;
import com.ticketbooking.userservice.dto.response.UserResponse;
import com.ticketbooking.userservice.entity.User;
import com.ticketbooking.userservice.entity.UserRole;
import com.ticketbooking.userservice.mapper.UserMapper;
import com.ticketbooking.userservice.repository.UserRepository;
import com.ticketbooking.userservice.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserPayload payload) {
        if (userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(payload.getEmail()) || userRepository.existsByPhoneNumberAndDeletedAtIsNull(payload.getPhoneNumber())) {
            throw new ApplicationException(ApplicationExceptionTypes.USER_ALREADY_EXISTS);
        }
        return UserMapper.toResponse(userRepository.save(UserMapper.toEntity(payload)));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUser(String userUuid) { return UserMapper.toResponse(findUser(userUuid)); }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        return UserMapper.toResponse(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email)
                .orElseThrow(() -> new ApplicationException(ApplicationExceptionTypes.USER_NOT_FOUND)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAllByDeletedAtIsNull().stream().map(UserMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public UserResponse updateUser(String userUuid, UpdateUserPayload payload) {
        User user = findUser(userUuid);
        if (payload.getEmail() != null && userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(payload.getEmail()).filter(found -> !found.getId().equals(user.getId())).isPresent())
            throw new ApplicationException(ApplicationExceptionTypes.USER_ALREADY_EXISTS);
        if (payload.getPhoneNumber() != null && userRepository.existsByPhoneNumberAndDeletedAtIsNull(payload.getPhoneNumber()) && !payload.getPhoneNumber().equals(user.getPhoneNumber()))
            throw new ApplicationException(ApplicationExceptionTypes.USER_ALREADY_EXISTS);
        UserMapper.updateEntity(user, payload);
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(String userUuid) {
        User user = findUser(userUuid);
        user.setActive(false);
        user.setDeletedAt(OffsetDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void activateUser(String userUuid) { setActive(userUuid, true); }

    @Override
    @Transactional
    public void deactivateUser(String userUuid) { setActive(userUuid, false); }

    @Override
    @Transactional
    public void updateRole(String userUuid, String role, String requesterUuid) {
        User requester = findUser(requesterUuid);
        if (requester.getRole() != UserRole.ADMIN) {
            throw new ApplicationException(ApplicationExceptionTypes.ACCESS_DENIED);
        }
        User user = findUser(userUuid);
        try { user.setRole(UserRole.valueOf(role.toUpperCase())); }
        catch (IllegalArgumentException ex) { throw new ApplicationException(ApplicationExceptionTypes.INVALID_USER_ROLE); }
        userRepository.save(user);
    }

    private User findUser(String userUuid) {
        return userRepository.findByUserUuidAndDeletedAtIsNull(userUuid)
                .orElseThrow(() -> new ApplicationException(ApplicationExceptionTypes.USER_NOT_FOUND));
    }
    private void setActive(String userUuid, boolean active) {
        User user = findUser(userUuid); user.setActive(active); userRepository.save(user);
    }
}
