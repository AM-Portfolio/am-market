package com.am.marketdata.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Set;

@Slf4j
@Service
public class MarketHoursService {

    @Value("${market-data.market.hours.start:09:15}")
    private String marketHoursStart;

    @Value("${market-data.market.hours.end:15:30}")
    private String marketHoursEnd;

    @Value("${market-data.market.timezone:Asia/Kolkata}")
    private String timezone;

    // Hardcoded holidays for now, ideally strictly from config or DB
    // 2026 Holidays
    private final Set<LocalDate> holidays = Set.of(
            LocalDate.of(2026, 1, 26), // Republic Day
            LocalDate.of(2026, 3, 7), // Holi (Example)
            LocalDate.of(2026, 8, 15), // Independence Day
            LocalDate.of(2026, 10, 2), // Gandhi Jayanti
            LocalDate.of(2026, 12, 25) // Christmas
    );

    public boolean isMarketOpen() {
        try {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of(timezone));
            LocalDate today = now.toLocalDate();
            DayOfWeek dayOfWeek = now.getDayOfWeek();

            // 1. Check Weekend
            if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                log.debug("Market status: CLOSED (Weekend: {})", dayOfWeek);
                return false;
            }

            // 2. Check Holidays
            if (holidays.contains(today)) {
                log.debug("Market status: CLOSED (Holiday: {})", today);
                return false;
            }

            // 3. Check Time
            LocalTime time = now.toLocalTime();
            LocalTime start = LocalTime.parse(marketHoursStart);
            LocalTime end = LocalTime.parse(marketHoursEnd);

            boolean isOpen = !time.isBefore(start) && !time.isAfter(end);

            if (log.isDebugEnabled()) {
                log.debug("Market status: {} (Time: {}, Range: {}-{})", isOpen ? "OPEN" : "CLOSED", time, start, end);
            }
            return isOpen;

        } catch (Exception e) {
            log.error("Error checking market hours: {}", e.getMessage());
            // Fail safe - assume closed to prevent error loops, or open?
            // Safer to assume closed for streaming purposes to trigger fallback
            return false;
        }
    }
}
