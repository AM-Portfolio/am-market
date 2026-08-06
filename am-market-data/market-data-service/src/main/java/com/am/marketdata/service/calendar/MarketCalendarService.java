package com.am.marketdata.service.calendar;

import com.am.marketdata.service.model.MarketCalendarDayDocument;
import com.am.marketdata.service.model.MarketCalendarSyncMetaDocument;
import com.am.marketdata.service.repo.MarketCalendarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketCalendarService {

    private static final Duration NEGATIVE_CACHE_TTL = Duration.ofSeconds(60);
    private static final Duration STALE_AFTER = Duration.ofDays(7);
    private static final Set<LocalDate> HARDCODED_HOLIDAYS = Set.of(
            LocalDate.of(2026, 1, 26),
            LocalDate.of(2026, 3, 7),
            LocalDate.of(2026, 8, 15),
            LocalDate.of(2026, 10, 2),
            LocalDate.of(2026, 12, 25));

    private final MarketCalendarSyncService syncService;
    private final MarketCalendarRepository calendarRepository;
    private final Clock clock;

    @Value("${market-data.market.hours.start:09:15}")
    private String marketHoursStart;

    @Value("${market-data.market.hours.end:15:30}")
    private String marketHoursEnd;

    @Value("${market-data.market.timezone:Asia/Kolkata}")
    private String timezone;

    private final Map<String, ReentrantLock> lazyLocks = new ConcurrentHashMap<>();
    private final Map<String, Instant> negativeCacheUntil = new ConcurrentHashMap<>();

    public CalendarQueryResult getHolidays(String exchange, int year) {
        String ex = MarketCalendarSyncService.normalizeExchange(exchange);
        ensureYearLoaded(ex, year);
        List<MarketCalendarDayDocument> days = syncService.loadYear(ex, year);
        return CalendarQueryResult.of(days, buildMeta(ex, days.isEmpty()));
    }

    public CalendarQueryResult getHoliday(String exchange, LocalDate date) {
        String ex = MarketCalendarSyncService.normalizeExchange(exchange);
        ensureYearLoaded(ex, date.getYear());
        Optional<MarketCalendarDayDocument> day = calendarRepository.findByExchangeAndDate(ex, date);
        if (day.isEmpty()) {
            List<MarketCalendarDayDocument> yearDays = syncService.loadYear(ex, date.getYear());
            day = yearDays.stream().filter(d -> date.equals(d.getDate())).findFirst();
        }
        return CalendarQueryResult.of(
                day.map(List::of).orElse(List.of()),
                buildMeta(ex, day.isEmpty() && !syncService.hasYearData(ex, date.getYear())));
    }

    public SessionTiming getTimings(String exchange, LocalDate date) {
        String ex = MarketCalendarSyncService.normalizeExchange(exchange);
        ensureYearLoaded(ex, date.getYear());
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return SessionTiming.closed(ex, date, "WEEKEND", buildMeta(ex, false));
        }
        Optional<MarketCalendarDayDocument> override =
                calendarRepository.findByExchangeAndDate(ex, date);
        if (override.isPresent()) {
            MarketCalendarDayDocument doc = override.get();
            if (doc.isClosed()) {
                return SessionTiming.closed(ex, date, "HOLIDAY", buildMeta(ex, false));
            }
            return SessionTiming.open(
                    ex,
                    date,
                    doc.getSessionStart() != null ? doc.getSessionStart() : defaultStart(),
                    doc.getSessionEnd() != null ? doc.getSessionEnd() : defaultEnd(),
                    "CALENDAR",
                    buildMeta(ex, false));
        }
        return SessionTiming.open(
                ex, date, defaultStart(), defaultEnd(), "CONFIG_DEFAULT", buildMeta(ex, false));
    }

    public MarketStatusView getStatus(String exchange) {
        String ex = MarketCalendarSyncService.normalizeExchange(exchange);
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(ZoneId.of(timezone)));
        LocalDate today = now.toLocalDate();
        LocalTime time = now.toLocalTime();
        ensureYearLoaded(ex, today.getYear());

        DayOfWeek dow = today.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return MarketStatusView.closed(ex, now, "WEEKEND", defaultStart(), defaultEnd(), buildMeta(ex, false));
        }

        boolean hasData = syncService.hasYearData(ex, today.getYear());
        if (!hasData) {
            if (HARDCODED_HOLIDAYS.contains(today)) {
                return MarketStatusView.closed(
                        ex, now, "HOLIDAY_FALLBACK", null, null, buildMeta(ex, true));
            }
            LocalTime start = defaultStart();
            LocalTime end = defaultEnd();
            boolean open = !time.isBefore(start) && !time.isAfter(end);
            return open
                    ? MarketStatusView.open(ex, now, start, end, buildMeta(ex, true))
                    : MarketStatusView.closed(ex, now, "OUTSIDE_SESSION", start, end, buildMeta(ex, true));
        }

        Optional<MarketCalendarDayDocument> day =
                calendarRepository.findByExchangeAndDate(ex, today);
        if (day.isPresent() && day.get().isClosed()) {
            return MarketStatusView.closed(
                    ex, now, "HOLIDAY", null, null, buildMeta(ex, false));
        }

        LocalTime start = day.map(MarketCalendarDayDocument::getSessionStart).orElse(null);
        LocalTime end = day.map(MarketCalendarDayDocument::getSessionEnd).orElse(null);
        if (start == null) {
            start = defaultStart();
        }
        if (end == null) {
            end = defaultEnd();
        }
        boolean open = !time.isBefore(start) && !time.isAfter(end);
        return open
                ? MarketStatusView.open(ex, now, start, end, buildMeta(ex, false))
                : MarketStatusView.closed(ex, now, "OUTSIDE_SESSION", start, end, buildMeta(ex, false));
    }

    public boolean isMarketOpen(String exchange) {
        return getStatus(exchange).open();
    }

    private void ensureYearLoaded(String exchange, int year) {
        if (syncService.hasYearData(exchange, year)) {
            syncService.loadYear(exchange, year);
            return;
        }
        String key = exchange + ":" + year;
        Instant blockedUntil = negativeCacheUntil.get(key);
        if (blockedUntil != null && Instant.now(clock).isBefore(blockedUntil)) {
            log.debug("Skipping lazy calendar sync due to negative cache key={}", key);
            return;
        }
        ReentrantLock lock = lazyLocks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            if (syncService.hasYearData(exchange, year)) {
                syncService.loadYear(exchange, year);
                return;
            }
            Instant stillBlocked = negativeCacheUntil.get(key);
            if (stillBlocked != null && Instant.now(clock).isBefore(stillBlocked)) {
                return;
            }
            syncService.sync(exchange, MarketCalendarSyncTrigger.LAZY_READ);
            negativeCacheUntil.remove(key);
        } catch (RuntimeException e) {
            log.warn("Lazy market calendar sync failed exchange={} year={}: {}",
                    exchange, year, e.getMessage());
            negativeCacheUntil.put(key, Instant.now(clock).plus(NEGATIVE_CACHE_TTL));
        } finally {
            lock.unlock();
        }
    }

    private CalendarMeta buildMeta(String exchange, boolean emptyFallback) {
        MarketCalendarSyncMetaDocument meta = syncService.getMeta(exchange);
        Instant lastSync = meta != null ? meta.getLastFullSyncAt() : null;
        boolean stale = emptyFallback
                || lastSync == null
                || Duration.between(lastSync, Instant.now(clock)).compareTo(STALE_AFTER) > 0;
        String source = emptyFallback
                ? "FALLBACK"
                : (meta != null && meta.getSource() != null ? meta.getSource() : "UNKNOWN");
        return new CalendarMeta(stale, source, lastSync);
    }

    private LocalTime defaultStart() {
        return LocalTime.parse(marketHoursStart);
    }

    private LocalTime defaultEnd() {
        return LocalTime.parse(marketHoursEnd);
    }

    public record CalendarMeta(boolean stale, String source, Instant lastFullSyncAt) {}

    public record CalendarQueryResult(List<MarketCalendarDayDocument> days, CalendarMeta meta) {
        static CalendarQueryResult of(List<MarketCalendarDayDocument> days, CalendarMeta meta) {
            return new CalendarQueryResult(days, meta);
        }
    }

    public record SessionTiming(
            String exchange,
            LocalDate date,
            boolean open,
            LocalTime sessionStart,
            LocalTime sessionEnd,
            String reason,
            CalendarMeta meta) {
        static SessionTiming closed(
                String exchange, LocalDate date, String reason, CalendarMeta meta) {
            return new SessionTiming(exchange, date, false, null, null, reason, meta);
        }

        static SessionTiming open(
                String exchange,
                LocalDate date,
                LocalTime start,
                LocalTime end,
                String reason,
                CalendarMeta meta) {
            return new SessionTiming(exchange, date, true, start, end, reason, meta);
        }
    }

    public record MarketStatusView(
            String exchange,
            boolean open,
            String reason,
            Instant asOf,
            LocalTime sessionStart,
            LocalTime sessionEnd,
            CalendarMeta meta) {
        static MarketStatusView open(
                String exchange,
                ZonedDateTime now,
                LocalTime start,
                LocalTime end,
                CalendarMeta meta) {
            return new MarketStatusView(
                    exchange, true, "OPEN", now.toInstant(), start, end, meta);
        }

        static MarketStatusView closed(
                String exchange,
                ZonedDateTime now,
                String reason,
                LocalTime start,
                LocalTime end,
                CalendarMeta meta) {
            return new MarketStatusView(
                    exchange, false, reason, now.toInstant(), start, end, meta);
        }
    }
}
