package com.am.marketdata.service.calendar;

import com.am.marketdata.common.calendar.MarketCalendarSource;
import com.am.marketdata.common.calendar.MarketHolidayDay;
import com.am.marketdata.service.model.MarketCalendarDayDocument;
import com.am.marketdata.service.model.MarketCalendarSyncMetaDocument;
import com.am.marketdata.service.repo.MarketCalendarRepository;
import com.am.marketdata.service.repo.MarketCalendarSyncMetaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketCalendarSyncService {

    private final MarketCalendarSource marketCalendarSource;
    private final MarketCalendarRepository calendarRepository;
    private final MarketCalendarSyncMetaRepository syncMetaRepository;

    private final Map<String, List<MarketCalendarDayDocument>> yearCache = new ConcurrentHashMap<>();

    public int sync(String exchange, MarketCalendarSyncTrigger trigger) {
        String ex = normalizeExchange(exchange);
        log.info("Market calendar sync starting exchange={} trigger={} source={}",
                ex, trigger, marketCalendarSource.sourceId());
        try {
            List<MarketHolidayDay> holidays = marketCalendarSource.fetchHolidays(ex);
            Instant now = Instant.now();
            List<MarketCalendarDayDocument> docs = new ArrayList<>();
            for (MarketHolidayDay day : holidays) {
                docs.add(MarketCalendarDayDocument.builder()
                        .id(MarketCalendarDayDocument.idFor(day.getExchange(), day.getDate()))
                        .date(day.getDate())
                        .exchange(day.getExchange())
                        .description(day.getDescription())
                        .holidayType(day.getHolidayType().name())
                        .closed(day.isClosed())
                        .sessionStart(day.getSessionStart())
                        .sessionEnd(day.getSessionEnd())
                        .source(marketCalendarSource.sourceId())
                        .syncedAt(now)
                        .build());
            }
            if (!docs.isEmpty()) {
                calendarRepository.saveAll(docs);
            }
            invalidateCache(ex);
            saveMeta(ex, now, null, trigger);
            log.info("Market calendar sync complete exchange={} days={} trigger={}",
                    ex, docs.size(), trigger);
            return docs.size();
        } catch (RuntimeException e) {
            saveMeta(ex, null, e.getMessage(), trigger);
            throw e;
        }
    }

    public List<MarketCalendarDayDocument> loadYear(String exchange, int year) {
        String ex = normalizeExchange(exchange);
        String key = cacheKey(ex, year);
        List<MarketCalendarDayDocument> cached = yearCache.get(key);
        if (cached != null) {
            return cached;
        }
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        List<MarketCalendarDayDocument> fromDb =
                calendarRepository.findByExchangeAndDateBetween(ex, start, end);
        List<MarketCalendarDayDocument> immutable = List.copyOf(fromDb);
        yearCache.put(key, immutable);
        return immutable;
    }

    public void putYearCache(String exchange, int year, List<MarketCalendarDayDocument> days) {
        yearCache.put(cacheKey(normalizeExchange(exchange), year), List.copyOf(days));
    }

    public void invalidateCache(String exchange) {
        String prefix = normalizeExchange(exchange) + ":";
        yearCache.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public boolean hasYearData(String exchange, int year) {
        String ex = normalizeExchange(exchange);
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return calendarRepository.countByExchangeAndDateBetween(ex, start, end) > 0;
    }

    public MarketCalendarSyncMetaDocument getMeta(String exchange) {
        return syncMetaRepository.findById(normalizeExchange(exchange)).orElse(null);
    }

    private void saveMeta(
            String exchange, Instant syncedAt, String error, MarketCalendarSyncTrigger trigger) {
        MarketCalendarSyncMetaDocument existing =
                syncMetaRepository.findById(exchange).orElse(null);
        MarketCalendarSyncMetaDocument.MarketCalendarSyncMetaDocumentBuilder builder =
                MarketCalendarSyncMetaDocument.builder()
                        .id(exchange)
                        .source(marketCalendarSource.sourceId())
                        .lastSyncTrigger(trigger.name())
                        .lastError(error);
        if (syncedAt != null) {
            builder.lastFullSyncAt(syncedAt);
        } else if (existing != null) {
            builder.lastFullSyncAt(existing.getLastFullSyncAt());
        }
        syncMetaRepository.save(builder.build());
    }

    static String normalizeExchange(String exchange) {
        if (exchange == null || exchange.isBlank()) {
            return "NSE";
        }
        return exchange.trim().toUpperCase();
    }

    private static String cacheKey(String exchange, int year) {
        return exchange + ":" + year;
    }
}
