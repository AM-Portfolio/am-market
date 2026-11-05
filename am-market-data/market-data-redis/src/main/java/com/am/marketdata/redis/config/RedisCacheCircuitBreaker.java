package com.am.marketdata.redis.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Small local circuit breaker for best-effort Redis cache operations.
 *
 * <p>Redis is an acceleration layer for market data, not the source of truth.
 * When Redis is unavailable, repeated requests must fail fast and continue to
 * the database/provider path instead of opening a new Redis connection attempt
 * on every request. After the cool-down, one probe is allowed to test recovery.
 */
@Component
@Slf4j
public class RedisCacheCircuitBreaker {

    @Value("${redis.cache.circuit.failure-threshold:3}")
    private int failureThreshold;

    @Value("${redis.cache.circuit.open-seconds:10}")
    private long openSeconds;

    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicLong openUntilMillis = new AtomicLong();
    private final AtomicBoolean recoveryProbeInFlight = new AtomicBoolean();

    /**
     * Returns whether a Redis operation may be attempted.
     *
     * <p>During the open window all operations fail fast. Once the window ends,
     * only one caller is allowed to perform the recovery probe.</p>
     */
    public boolean allowRequest() {
        long now = System.currentTimeMillis();
        long openUntil = openUntilMillis.get();
        if (openUntil <= now) {
            if (openUntil > 0 && !recoveryProbeInFlight.compareAndSet(false, true)) {
                return false;
            }
            return true;
        }
        return false;
    }

    public void recordSuccess() {
        consecutiveFailures.set(0);
        openUntilMillis.set(0);
        recoveryProbeInFlight.set(false);
    }

    public void recordFailure(String operation, Throwable error) {
        recoveryProbeInFlight.set(false);
        int failures = consecutiveFailures.incrementAndGet();
        int threshold = failureThreshold > 0 ? failureThreshold : 3;
        if (failures >= threshold) {
            long durationSeconds = openSeconds > 0 ? openSeconds : 10;
            long until = System.currentTimeMillis() + durationSeconds * 1000L;
            openUntilMillis.set(until);
            log.warn("[REDIS_CIRCUIT] Opened after {} consecutive failures during {} for {} seconds: {}",
                    failures, operation, durationSeconds, error != null ? error.getMessage() : "unknown error");
        } else {
            log.warn("[REDIS_CIRCUIT] Redis {} failed ({}/{}): {}",
                    operation, failures, threshold, error != null ? error.getMessage() : "unknown error");
        }
    }

    public boolean isOpen() {
        return openUntilMillis.get() > System.currentTimeMillis();
    }
}
