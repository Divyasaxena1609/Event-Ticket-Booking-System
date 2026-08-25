package com.ticketbooking.auth_service.repository;

import com.ticketbooking.auth_service.entity.AuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthAccountRepository extends JpaRepository<AuthAccount, UUID> {
    Optional<AuthAccount> findByEmailIgnoreCase(String email);
}
