package com.am.marketdata.redis.config;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import lombok.extern.slf4j.Slf4j;

/**
 * Shared per-process circuit for Redis cache operations.
 *
 * <p>Redis is an accelerator for market data, not the primary source of truth.
 * When authentication, DNS, or the network fails, repeatedly opening new
 * connections from every request adds avoidable latency. This guard briefly
 * bypasses Redis and lets existing database/provider fallbacks run. The next
 * request after the cooldown probes Redis again and closes the circuit on
 * success.</p>
 */
@Slf4j
public final class RedisAvailabilityGuard {

    private final long failureCooldownMs;
    private final AtomicLong bypassUntilMs = new AtomicLong(0L);
    private final AtomicBoolean failureLogged = new AtomicBoolean(false);

    public RedisAvailabilityGuard(long failureCooldownMs) {
        this.failureCooldownMs = Math.max(1_000L, failureCooldownMs);
    }

    public boolean allowRequest() {
        long now = System.currentTimeMillis();
        long bypassUntil = bypassUntilMs.get();
        if (bypassUntil > now) {
            return false;
        }

        if (bypassUntil != 0L && bypassUntilMs.compareAndSet(bypassUntil, 0L)) {
            failureLogged.set(false);
        }
        return true;
    }

    public void recordSuccess() {
        long previous = bypassUntilMs.getAndSet(0L);
        failureLogged.set(false);
        if (previous > System.currentTimeMillis()) {
            log.info("Redis cache circuit closed after a successful probe");
        }
    }

    public void recordFailure(Throwable error) {
        long until = System.currentTimeMillis() + failureCooldownMs;
        bypassUntilMs.updateAndGet(current -> Math.max(current, until));
        if (failureLogged.compareAndSet(false, true)) {
            String reason = error == null ? "unknown Redis failure" : String.valueOf(error.getMessage());
            log.warn("Redis cache circuit opened for {} ms; cache calls will bypass Redis temporarily. Reason: {}",
                    failureCooldownMs, reason);
        }
    }
}
