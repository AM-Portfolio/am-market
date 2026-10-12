package com.am.marketdata.common.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TestMarketDepthFreshness {

    @Test
    void acceptsRecentProviderTimestampSlightlyAheadOfServerClock() {
        Instant slightlyFuture = Instant.now().plusMillis(500);

        assertTrue(MarketDepthFreshness.isFresh(slightlyFuture, 60_000));
    }

    @Test
    void rejectsTimestampBeyondAllowedClockSkew() {
        Instant tooFarAhead = Instant.now().plusMillis(2_500);

        assertFalse(MarketDepthFreshness.isFresh(tooFarAhead, 60_000));
    }

    @Test
    void rejectsDepthOlderThanConfiguredMaximumAge() {
        Instant stale = Instant.now().minusSeconds(61);

        assertFalse(MarketDepthFreshness.isFresh(stale, 60_000));
    }

    @Test
    void rejectsMissingTimestampOrDisabledAgeLimit() {
        assertFalse(MarketDepthFreshness.isFresh(null, 60_000));
        assertFalse(MarketDepthFreshness.isFresh(Instant.now(), 0));
    }
}
