package com.ticketbooking.bookingservice.service;

import com.ticketbooking.bookingservice.service.Impl.SeatLockServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SeatLockServiceImplTest {

    private SeatLockServiceImpl lockService;

    @BeforeEach
    void setUp() {
        // Test with in-memory fallback (null RedisTemplate) to verify standalone behavior without Redis running
        lockService = new SeatLockServiceImpl(null);
    }

    @Test
    void testAcquireLocks_Success() {
        List<String> seats = Arrays.asList("A1", "A2", "A3");
        boolean locked = lockService.acquireLocks("event-100", seats, "user-1", "booking-1", Duration.ofMinutes(5));

        assertTrue(locked);
        assertTrue(lockService.isSeatLocked("event-100", "A1"));
        assertTrue(lockService.isSeatLocked("event-100", "A2"));
        assertTrue(lockService.isSeatLocked("event-100", "A3"));

        Set<String> lockedSeats = lockService.getLockedSeats("event-100");
        assertEquals(3, lockedSeats.size());
        assertTrue(lockedSeats.containsAll(seats));
    }

    @Test
    void testAcquireLocks_FailsAndRollsBackWhenConflict() {
        // User 1 locks A1
        lockService.acquireLocks("event-100", Collections.singletonList("A1"), "user-1", "booking-1", Duration.ofMinutes(5));

        // User 2 tries to lock A2 and A1 -> should fail and roll back A2
        boolean user2Locked = lockService.acquireLocks("event-100", Arrays.asList("A2", "A1"), "user-2", "booking-2", Duration.ofMinutes(5));

        assertFalse(user2Locked);
        // A2 should NOT be locked because of atomic batch rollback
        assertFalse(lockService.isSeatLocked("event-100", "A2"));
        // A1 remains locked by User 1
        assertTrue(lockService.isSeatLocked("event-100", "A1"));
    }

    @Test
    void testReleaseLocks_UnblocksSpecificSeats() {
        List<String> seats = Arrays.asList("B1", "B2");
        lockService.acquireLocks("event-100", seats, "user-1", "booking-1", Duration.ofMinutes(5));

        assertTrue(lockService.isSeatLocked("event-100", "B1"));
        assertTrue(lockService.isSeatLocked("event-100", "B2"));

        lockService.releaseLocks("event-100", Collections.singletonList("B1"), "user-1");

        assertFalse(lockService.isSeatLocked("event-100", "B1"));
        assertTrue(lockService.isSeatLocked("event-100", "B2"));
    }

    @Test
    void testReleaseBookingLocks_UnblocksAllSeatsOfBooking() {
        List<String> seats = Arrays.asList("C1", "C2", "C3");
        lockService.acquireLocks("event-100", seats, "user-1", "booking-99", Duration.ofMinutes(5));

        assertEquals(3, lockService.getLockedSeats("event-100").size());

        lockService.releaseBookingLocks("event-100", "booking-99");

        assertEquals(0, lockService.getLockedSeats("event-100").size());
        assertFalse(lockService.isSeatLocked("event-100", "C1"));
    }
}
