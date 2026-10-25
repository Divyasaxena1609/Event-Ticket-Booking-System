package com.ticketbooking.bookingservice.entity;

import com.ticketbooking.utils.StringUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id", nullable = false, unique = true)
    private Long id;

    @Column(name = "booking_uuid", nullable = false, unique = true, updatable = false)
    private String bookingUUID;

    @NotNull
    @Column(name = "event_uuid", nullable = false)
    private String eventUuid;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private OffsetDateTime deletedAt;

    @PrePersist
    public void onCreate() {
        this.bookingUUID = StringUtils.generateUUID();
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        this.expiresAt = OffsetDateTime.now().plusMinutes(12);
        this.status = BookingStatus.CREATED;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
