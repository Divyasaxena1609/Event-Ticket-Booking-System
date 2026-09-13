package com.ticketbooking.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "auth_accounts", indexes = @Index(name = "idx_auth_account_email", columnList = "email"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "user_uuid")
    private String userUuid;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "refresh_token_hash")
    private String refreshTokenHash;

    @Column(name = "refresh_token_expires_at")
    private OffsetDateTime refreshTokenExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void beforeInsert() {
        createdAt = OffsetDateTime.now();
        active = true;
        email = email.toLowerCase();
    }
}
