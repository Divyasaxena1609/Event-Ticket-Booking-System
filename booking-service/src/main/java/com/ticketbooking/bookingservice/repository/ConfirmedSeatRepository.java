package com.ticketbooking.bookingservice.repository;

import com.ticketbooking.bookingservice.entity.ConfirmedSeat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfirmedSeatRepository extends JpaRepository<ConfirmedSeat, Long> {}
