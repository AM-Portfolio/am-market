package com.am.marketdata.service;

import com.am.marketdata.service.calendar.MarketCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketHoursService {

    private final MarketCalendarService marketCalendarService;

    // PERFORMANCE OPTIMIZATION: In-Memory Request Caching
    // Reduces redundant MongoDB calendar queries during a single HTTP request lifecycle.
    // 15 seconds TTL is short enough to detect market closing transitions almost instantly
    // while dropping duplicate database roundtrips down to 0.
    private static final long CACHE_TTL_MS = 15_000; // 15 seconds

    private final AtomicLong lastOpenCheckTime = new AtomicLong(0);
    private final AtomicBoolean cachedIsOpen = new AtomicBoolean(false);

    private final AtomicLong lastSessionCheckTime = new AtomicLong(0);
    private final AtomicBoolean cachedIsSessionDay = new AtomicBoolean(true);

    public boolean isMarketOpen() {
        long now = System.currentTimeMillis();
        if (now - lastOpenCheckTime.get() < CACHE_TTL_MS) {
            return cachedIsOpen.get();
        }

        try {
            boolean isOpen = marketCalendarService.isMarketOpen("NSE");
            cachedIsOpen.set(isOpen);
            lastOpenCheckTime.set(now);
            return isOpen;
        } catch (Exception e) {
            log.error("Error checking market hours: {}", e.getMessage());
            return false;
        }
    }

    /** True on an NSE cash session date (Mon–Fri, not a holiday). False on weekend/holiday. */
    public boolean isCashSessionDay() {
        long now = System.currentTimeMillis();
        if (now - lastSessionCheckTime.get() < CACHE_TTL_MS) {
            return cachedIsSessionDay.get();
        }

        try {
            java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));
            boolean isSession = marketCalendarService.getTimings("NSE", today).open();
            cachedIsSessionDay.set(isSession);
            lastSessionCheckTime.set(now);
            return isSession;
        } catch (Exception e) {
            log.warn("Could not resolve session day: {}", e.getMessage());
            return true;
        }
    }
}
