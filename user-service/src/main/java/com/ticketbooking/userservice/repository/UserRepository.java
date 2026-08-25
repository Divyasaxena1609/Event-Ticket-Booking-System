package com.ticketbooking.userservice.repository;

import com.ticketbooking.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserUuidAndDeletedAtIsNull(String userUuid);
    Optional<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);
    List<User> findAllByDeletedAtIsNull();
    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);
    boolean existsByPhoneNumberAndDeletedAtIsNull(String phoneNumber);
}
