package com.ticketbooking.bookingservice.repository;

import com.ticketbooking.bookingservice.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingUUID(String bookingUUID);
    List<Booking> findByUserId(String userId);
    List<Booking> findByEventUuid(String eventUuid);
}
