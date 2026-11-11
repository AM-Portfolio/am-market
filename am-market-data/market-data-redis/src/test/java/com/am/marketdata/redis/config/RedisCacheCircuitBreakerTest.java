package com.am.marketdata.redis.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RedisCacheCircuitBreakerTest {

    @Test
    void skippedRecoveryProbeAllowsTheNextCallerToTryRedis() {
        RedisCacheCircuitBreaker breaker = new RedisCacheCircuitBreaker();
        ReflectionTestUtils.setField(breaker, "failureThreshold", 1);
        breaker.recordFailure("test", new IllegalStateException("unavailable"));

        AtomicLong openUntil = (AtomicLong) ReflectionTestUtils.getField(breaker, "openUntilMillis");
        openUntil.set(System.currentTimeMillis() - 1);

        assertTrue(breaker.allowRequest());
        assertFalse(breaker.allowRequest());
        breaker.recordSkipped();
        assertTrue(breaker.allowRequest());
        breaker.recordSuccess();
        assertTrue(breaker.allowRequest());
    }

    @Test
    void timedOutRecoveryProbeSelfHealsAndAllowsNextCaller() {
        RedisCacheCircuitBreaker breaker = new RedisCacheCircuitBreaker();
        ReflectionTestUtils.setField(breaker, "failureThreshold", 1);
        breaker.recordFailure("test", new IllegalStateException("unavailable"));

        AtomicLong openUntil = (AtomicLong) ReflectionTestUtils.getField(breaker, "openUntilMillis");
        openUntil.set(System.currentTimeMillis() - 1);

        assertTrue(breaker.allowRequest());
        // Second caller is rejected while probe is in flight
        assertFalse(breaker.allowRequest());

        // Simulate probe taking longer than 15s timeout
        AtomicLong probeStartedAt = (AtomicLong) ReflectionTestUtils.getField(breaker, "probeStartedAtMillis");
        probeStartedAt.set(System.currentTimeMillis() - 16_000L);

        // Next caller detects timed out probe, recovers lock, and is granted probe access immediately
        assertTrue(breaker.allowRequest());
    }
}
