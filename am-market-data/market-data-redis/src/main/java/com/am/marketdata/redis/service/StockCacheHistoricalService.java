package com.am.marketdata.redis.service;

import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.redis.cache.StockRedisCache;
import com.am.marketdata.redis.model.OHLCV;
import com.am.marketdata.redis.model.StockBars;
import com.am.marketdata.redis.util.BarCalculatorUtil;
import lombok.RequiredArgsConstructor;
import com.am.marketdata.common.log.AppLogger;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing historical stock price data in Redis cache.
 */
@Service
@RequiredArgsConstructor
public class StockCacheHistoricalService {

    private final AppLogger log = AppLogger.getLogger();

    private final StockRedisCache stockRedisCache;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    // Cache hit statistics (for monitoring purposes)
    private final Map<String, Integer> cacheHits = new ConcurrentHashMap<>();
    private final Map<String, Integer> cacheMisses = new ConcurrentHashMap<>();

    /**
     * Cache historical bar for a symbol with individual parameters
     */
    public boolean cacheHistoricalBar(String symbol, String date, OHLCV bar) {
        StockBars stockBars = StockBars.builder()
                .symbol(symbol)
                .interval("1d")
                .startDate(date)
                .bars(List.of(bar))
                .build();
        return cacheHistoricalBar(List.of(stockBars));
    }

    /**
     * Cache historical bar for a symbol using StockBars list
     */
    public boolean cacheHistoricalBar(List<StockBars> stockBarsList) {
        return stockRedisCache.saveHistoricalBar(stockBarsList);
    }

    /**
     * Get historical bars for multiple symbols within a date range
     */
    public Map<String, List<StockBars>> getHistoricalBarsWithStats(List<String> symbols, String startDate,
            String endDate, String interval) {
        // Use Redis's multi-key operation for better performance
        Map<String, List<StockBars>> result = stockRedisCache.getMultiSymbolHistoricalBars(symbols, startDate, endDate,
                interval);

        // Update cache hit/miss statistics
        try {
            LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
            LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);

            // For each symbol and date, update stats
            for (String symbol : symbols) {
                LocalDate current = start;
                while (!current.isAfter(end)) {
                    String date = current.format(DATE_FORMATTER);
                    String cacheKey = symbol + ":1d:" + date;

                    List<StockBars> symbolBars = result.get(symbol);
                    boolean hasData = symbolBars != null && symbolBars.stream()
                            .anyMatch(bars -> date.equals(bars.getStartDate()));

                    if (hasData) {
                        // Cache hit
                        cacheHits.compute(cacheKey, (k, v) -> (v == null) ? 1 : v + 1);
                    } else {
                        // Cache miss
                        cacheMisses.compute(cacheKey, (k, v) -> (v == null) ? 1 : v + 1);
                    }

                    current = current.plusDays(1);
                }
            }
        } catch (Exception e) {
            // Just log the error but don't fail the operation
            log.error("getHistoricalBarsWithStats", "Error updating cache statistics: " + e.getMessage());
        }

        return result;
    }

    /**
     * Process and cache daily bar data
     */
    public boolean processDailyBar(String symbol, List<OHLCV> rawPrices, LocalDate date) {
        if (rawPrices == null || rawPrices.isEmpty()) {
            log.warn("processDailyBar", "No raw price data provided for symbol: " + symbol);
            return false;
        }

        try {
            // Sort prices by timestamp
            rawPrices.sort(java.util.Comparator.comparing(OHLCV::getTime));

            // Create a daily bar
            OHLCV dailyBar = BarCalculatorUtil.createBar(rawPrices, date.atStartOfDay());
            if (dailyBar != null) {
                String dateStr = BarCalculatorUtil.formatDate(date);
                boolean success = stockRedisCache.saveHistoricalBar(symbol, dateStr, dailyBar,
                        TimeFrame.DAY.getApiValue());
                log.debug("Processed and cached daily bar for {} on {}", symbol, dateStr);
                return success;
            }
        } catch (Exception e) {
            log.error("processDailyBar", "Error processing daily bar for " + symbol + ": " + e.getMessage());
        }

        return false;
    }

    /**
     * Get cache hit statistics
     */
    public Map<String, Integer> getCacheHitStats() {
        return new ConcurrentHashMap<>(cacheHits);
    }

    /**
     * Get cache miss statistics
     */
    public Map<String, Integer> getCacheMissStats() {
        return new ConcurrentHashMap<>(cacheMisses);
    }

    /**
     * Reset cache statistics
     */
    public void resetCacheStats() {
        cacheHits.clear();
        cacheMisses.clear();
    }
}
