package com.ticketbooking.eventservice.entity;

import com.ticketbooking.utils.StringUtils;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "events",
    indexes = {
        @Index(name = "idx_event_uuid", columnList = "event_uuid")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id", nullable = false, unique = true)
    private Long id;

    @Column(name = "event_uuid", nullable = false, unique = true, updatable = false)
    private String eventUuid;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    private String category;

    private String organizerName;

    @Column(name = "organizer_user_uuid", nullable = false, updatable = false)
    private String organizerUserUuid;

    @NotNull
    private LocalDate eventDate;

    private LocalTime startTime;

    private LocalTime endTime;

    @NotNull
    private String venueName;

    @NotNull
    private String city;

    private String address;

    @Positive
    private int totalSeats;

    @PositiveOrZero
    private int availableSeats;

    private BigDecimal ticketPrice;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private OffsetDateTime deletedAt;

    @PrePersist
    public void onCreate() {
        this.eventUuid = StringUtils.generateUUID();
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        this.status = EventStatus.UPCOMING;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
