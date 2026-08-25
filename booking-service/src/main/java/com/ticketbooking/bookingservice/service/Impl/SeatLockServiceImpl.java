package com.ticketbooking.bookingservice.service.Impl;

import com.ticketbooking.bookingservice.service.ISeatLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SeatLockServiceImpl implements ISeatLockService {

    private static final String KEY_PREFIX = "seat_lock:";
    private final RedisTemplate<String, String> redisTemplate;
    private volatile boolean isRedisAvailable = false;

    // In-memory fallback lock store for when standalone Redis is not installed locally
    private final Map<String, LockEntry> inMemoryLocks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "seat-lock-cleaner");
        t.setDaemon(true);
        return t;
    });

    public static class LockEntry {
        private final String userUuid;
        private final String bookingUuid;
        private final Instant expiresAt;

        public LockEntry(String userUuid, String bookingUuid, Duration ttl) {
            this.userUuid = userUuid;
            this.bookingUuid = bookingUuid;
            this.expiresAt = Instant.now().plus(ttl);
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }

        public String getUserUuid() {
            return userUuid;
        }

        public String getBookingUuid() {
            return bookingUuid;
        }
    }

    @Autowired
    public SeatLockServiceImpl(@Autowired(required = false) RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        checkRedisAvailability();
        // Periodically evict expired in-memory locks every 15 seconds
        cleanupExecutor.scheduleAtFixedRate(this::evictExpiredInMemoryLocks, 15, 15, TimeUnit.SECONDS);
    }

    private void checkRedisAvailability() {
        if (redisTemplate != null) {
            try {
                if (redisTemplate.getConnectionFactory() != null) {
                    redisTemplate.getConnectionFactory().getConnection().ping();
                    isRedisAvailable = true;
                    log.info("Connected to Redis server. Distributed seat locking active.");
                    return;
                }
            } catch (Exception e) {
                log.warn("Redis server not detected on localhost:6379 ({}). Using embedded in-memory TTL seat lock manager.", e.getMessage());
            }
        }
        isRedisAvailable = false;
    }

    private String buildKey(String eventUuid, String seat) {
        return KEY_PREFIX + eventUuid + ":" + seat;
    }

    private String buildValue(String userUuid, String bookingUuid) {
        return userUuid + ":" + (bookingUuid != null ? bookingUuid : "");
    }

    @Override
    public synchronized boolean acquireLocks(String eventUuid, List<String> seats, String userUuid, String bookingUuid, Duration ttl) {
        if (seats == null || seats.isEmpty()) {
            return true;
        }

        List<String> successfullyLockedKeys = new ArrayList<>();
        String value = buildValue(userUuid, bookingUuid);

        for (String seat : seats) {
            String key = buildKey(eventUuid, seat);
            boolean locked = false;

            if (isRedisAvailable) {
                try {
                    Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
                    locked = Boolean.TRUE.equals(acquired);
                } catch (Exception e) {
                    log.warn("Redis error on lock acquisition, falling back to in-memory: {}", e.getMessage());
                    isRedisAvailable = false;
                }
            }

            if (!isRedisAvailable) {
                // In-memory atomic locking with TTL
                evictExpiredInMemoryLocks();
                LockEntry existing = inMemoryLocks.get(key);
                if (existing == null || existing.isExpired()) {
                    inMemoryLocks.put(key, new LockEntry(userUuid, bookingUuid, ttl));
                    locked = true;
                } else {
                    locked = false;
                }
            }

            if (locked) {
                successfullyLockedKeys.add(key);
            } else {
                // Rollback any acquired locks in this batch to maintain atomicity
                rollbackKeys(successfullyLockedKeys);
                log.info("Seat lock failed for seat '{}' on event '{}'. Rolled back {} locks.", seat, eventUuid, successfullyLockedKeys.size());
                return false;
            }
        }

        log.info("Successfully locked {} seats for event '{}' by user '{}' (TTL: {}s)", seats.size(), eventUuid, userUuid, ttl.toSeconds());
        return true;
    }

    private void rollbackKeys(List<String> keys) {
        for (String key : keys) {
            if (isRedisAvailable) {
                try {
                    redisTemplate.delete(key);
                } catch (Exception ignored) {}
            }
            inMemoryLocks.remove(key);
        }
    }

    @Override
    public void releaseLocks(String eventUuid, List<String> seats, String userUuid) {
        if (seats == null || seats.isEmpty()) return;

        for (String seat : seats) {
            String key = buildKey(eventUuid, seat);
            if (isRedisAvailable) {
                try {
                    redisTemplate.delete(key);
                } catch (Exception ignored) {}
            }
            inMemoryLocks.remove(key);
        }
        log.info("Released seat locks for seats {} on event '{}'", seats, eventUuid);
    }

    @Override
    public void releaseBookingLocks(String eventUuid, String bookingUuid) {
        if (eventUuid == null || bookingUuid == null) return;

        String prefix = KEY_PREFIX + eventUuid + ":";
        if (isRedisAvailable) {
            try {
                Set<String> keys = redisTemplate.keys(prefix + "*");
                if (keys != null) {
                    for (String key : keys) {
                        String val = redisTemplate.opsForValue().get(key);
                        if (val != null && val.contains(":" + bookingUuid)) {
                            redisTemplate.delete(key);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        inMemoryLocks.entrySet().removeIf(entry ->
                entry.getKey().startsWith(prefix) && bookingUuid.equals(entry.getValue().getBookingUuid())
        );
        log.info("Released all seat locks for booking '{}' on event '{}'", bookingUuid, eventUuid);
    }

    @Override
    public Set<String> getLockedSeats(String eventUuid) {
        Set<String> lockedSeats = new HashSet<>();
        String prefix = KEY_PREFIX + eventUuid + ":";

        if (isRedisAvailable) {
            try {
                Set<String> keys = redisTemplate.keys(prefix + "*");
                if (keys != null) {
                    for (String key : keys) {
                        lockedSeats.add(key.substring(prefix.length()));
                    }
                }
                return lockedSeats;
            } catch (Exception e) {
                log.warn("Redis error on getLockedSeats, falling back to in-memory: {}", e.getMessage());
                isRedisAvailable = false;
            }
        }

        evictExpiredInMemoryLocks();
        for (Map.Entry<String, LockEntry> entry : inMemoryLocks.entrySet()) {
            if (entry.getKey().startsWith(prefix) && !entry.getValue().isExpired()) {
                lockedSeats.add(entry.getKey().substring(prefix.length()));
            }
        }
        return lockedSeats;
    }

    @Override
    public boolean isSeatLocked(String eventUuid, String seat) {
        String key = buildKey(eventUuid, seat);
        if (isRedisAvailable) {
            try {
                Boolean hasKey = redisTemplate.hasKey(key);
                return Boolean.TRUE.equals(hasKey);
            } catch (Exception ignored) {
                isRedisAvailable = false;
            }
        }

        LockEntry entry = inMemoryLocks.get(key);
        return entry != null && !entry.isExpired();
    }

    private void evictExpiredInMemoryLocks() {
        inMemoryLocks.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
