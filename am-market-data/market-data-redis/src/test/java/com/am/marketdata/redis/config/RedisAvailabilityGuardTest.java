package com.am.marketdata.redis.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RedisAvailabilityGuardTest {

    @Test
    void opensOnFailureAndClosesAfterSuccessfulProbe() {
        RedisAvailabilityGuard guard = new RedisAvailabilityGuard(30_000);

        assertTrue(guard.allowRequest());

        guard.recordFailure(new IllegalStateException("authentication failed"));
        assertFalse(guard.allowRequest());

        guard.recordSuccess();
        assertTrue(guard.allowRequest());
    }
}
