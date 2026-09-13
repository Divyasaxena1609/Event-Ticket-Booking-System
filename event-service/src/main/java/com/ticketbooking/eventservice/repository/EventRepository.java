package com.ticketbooking.eventservice.repository;

import com.ticketbooking.eventservice.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;
import com.ticketbooking.eventservice.entity.EventStatus;

public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findByEventUuid(String eventUuid);
    List<Event> findByOrganizerUserUuid(String organizerUserUuid);
    List<Event> findByDeletedAtIsNullAndEventDateGreaterThanEqualAndStatusNot(LocalDate date, EventStatus status);
    List<Event> findByOrganizerUserUuidAndDeletedAtIsNull(String organizerUserUuid);
}
