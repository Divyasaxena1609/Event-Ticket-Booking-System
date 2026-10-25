package com.ticketbooking.bookingservice.repository;

import com.ticketbooking.bookingservice.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.ticketbooking.bookingservice.entity.BookingStatus;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingUUID(String bookingUUID);
    List<Booking> findByUserId(String userId);
    List<Booking> findByEventUuid(String eventUuid);
    @Query("select b from Booking b where b.status = :status and (b.expiresAt < :now or b.createdAt < :createdBefore)")
    List<Booking> findExpiredPendingBookings(@Param("status") BookingStatus status, @Param("now") OffsetDateTime now, @Param("createdBefore") OffsetDateTime createdBefore);
}
