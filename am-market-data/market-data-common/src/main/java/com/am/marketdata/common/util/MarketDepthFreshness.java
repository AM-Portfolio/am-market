package com.am.marketdata.common.util;

import java.time.Instant;

/**
 * Applies the same freshness rule to provider and cached market-depth snapshots.
 */
public final class MarketDepthFreshness {

    private static final long ALLOWED_CLOCK_SKEW_MILLIS = 2_000;

    private MarketDepthFreshness() {
    }

    public static boolean isFresh(Instant quoteTimestamp, long maxAgeMillis) {
        if (quoteTimestamp == null || maxAgeMillis <= 0) {
            return false;
        }

        long ageMillis = System.currentTimeMillis() - quoteTimestamp.toEpochMilli();
        // Provider clocks can be slightly ahead of ours; accept that small skew but still reject old depth.
        return ageMillis >= -ALLOWED_CLOCK_SKEW_MILLIS && ageMillis <= maxAgeMillis;
    }
}
