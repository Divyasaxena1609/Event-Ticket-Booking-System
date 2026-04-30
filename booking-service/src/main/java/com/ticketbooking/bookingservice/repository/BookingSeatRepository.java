package com.ticketbooking.bookingservice.repository;

import java.util.*;
import com.ticketbooking.bookingservice.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat , Long> {
    List<BookingSeat> findByBookingUUID(String bookingUUID);
    boolean existsByEventUuidAndSeatNumber(String eventUuid, String seatNumber);
    void deleteByBookingUUID(String bookingUUID);
}
