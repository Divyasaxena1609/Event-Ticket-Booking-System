package com.ticketbooking.eventservice.repository;

import com.ticketbooking.eventservice.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findByEventUuid(String eventUuid);
}
