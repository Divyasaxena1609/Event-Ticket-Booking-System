package com.ticketbooking.bookingservice.service.Impl;

import com.ticketbooking.bookingservice.entity.Booking;
import com.ticketbooking.bookingservice.entity.BookingStatus;
import com.ticketbooking.bookingservice.repository.BookingRepository;
import com.ticketbooking.bookingservice.repository.BookingSeatRepository;
import com.ticketbooking.bookingservice.service.ISeatLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ISeatLockService seatLockService;

    /**
     * Runs every 60 seconds to release seats for any unconfirmed bookings that have exceeded the 12-minute TTL.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredBookings() {
        OffsetDateTime now = OffsetDateTime.now();
        List<Booking> pendingBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CREATED)
                .filter(b -> (b.getExpiresAt() != null && b.getExpiresAt().isBefore(now)) ||
                        (b.getCreatedAt() != null && b.getCreatedAt().isBefore(now.minusMinutes(12))))
                .toList();

        if (!pendingBookings.isEmpty()) {
            log.info("Reconciliation sweeper found {} expired pending bookings to clean up.", pendingBookings.size());
            for (Booking booking : pendingBookings) {
                try {
                    seatLockService.releaseBookingLocks(booking.getEventUuid(), booking.getBookingUUID());
                    bookingSeatRepository.deleteByBookingUUID(booking.getBookingUUID());
                    booking.setStatus(BookingStatus.EXPIRED);
                    bookingRepository.save(booking);
                    log.info("Auto-expired booking '{}' and released seat locks after 12-minute window.", booking.getBookingUUID());
                } catch (Exception e) {
                    log.error("Error expiring booking '{}': {}", booking.getBookingUUID(), e.getMessage());
                }
            }
        }
    }
}
