package com.ticketbooking.bookingservice.service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

public interface ISeatLockService {

    /**
     * Attempts to acquire temporary locks for the given seats for an event.
     *
     * @param eventUuid   The event UUID
     * @param seats       List of seat IDs to lock
     * @param userUuid    The requesting user UUID
     * @param bookingUuid Optional booking reference UUID
     * @param ttl         Duration for which seats are locked (e.g., 5 minutes)
     * @return true if all seats were locked successfully, false if any seat is already locked
     */
    boolean acquireLocks(String eventUuid, List<String> seats, String userUuid, String bookingUuid, Duration ttl);

    /**
     * Releases locks for specific seats.
     *
     * @param eventUuid The event UUID
     * @param seats     List of seat IDs to release
     * @param userUuid  The user releasing the lock
     */
    void releaseLocks(String eventUuid, List<String> seats, String userUuid);

    /**
     * Releases all locks associated with a booking.
     *
     * @param eventUuid   The event UUID
     * @param bookingUuid The booking UUID
     */
    void releaseBookingLocks(String eventUuid, String bookingUuid);

    /**
     * Gets all currently locked seat IDs for an event.
     *
     * @param eventUuid The event UUID
     * @return Set of locked seat IDs
     */
    Set<String> getLockedSeats(String eventUuid);

    /**
     * Checks if a specific seat is locked.
     *
     * @param eventUuid The event UUID
     * @param seat      Seat ID
     * @return true if seat is locked
     */
    boolean isSeatLocked(String eventUuid, String seat);
}
