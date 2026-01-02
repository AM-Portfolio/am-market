package com.am.marketdata.redis.service;

import com.am.marketdata.common.model.TimeFrame;
import static com.am.marketdata.common.constants.TimeIntervalConstants.*;
import com.am.marketdata.redis.cache.StockRedisCache;
import com.am.marketdata.redis.model.OHLCV;
import com.am.marketdata.redis.model.StockBars;
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
 * Service for managing stock price data in Redis cache.
 * Provides higher-level operations and additional functionality on top of the
 * StockRedisCache.
 * Delegates to specialized services for historical data, symbol-specific
 * operations, and bar calculation.
 */
@Service
@RequiredArgsConstructor
public class StockCacheService {

    private final AppLogger log = AppLogger.getLogger();

    private final StockRedisCache stockRedisCache;
    private final BarCalculator barCalculator;
    private final StockCacheHistoricalService historicalService;
    private final StockCacheSymbolService symbolService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Cache intraday bars for a symbol
     */
    public boolean cacheIntradayBars(String symbol, String interval, List<OHLCV> bars) {
        // Create StockBars object for the symbol service
        String dateStr = LocalDate.now().format(DATE_FORMATTER);
        StockBars stockBars = StockBars.builder()
                .symbol(symbol)
                .interval(interval)
                .startDate(dateStr)
                .bars(bars)
                .build();
        return symbolService.cacheIntradayBars(List.of(stockBars));
    }

    /**
     * Cache a historical bar for a symbol and date
     */
    public boolean cacheHistoricalBar(String symbol, String date, OHLCV bar, TimeFrame timeFrame) {
        // Create StockBars object for the historical service
        StockBars stockBars = StockBars.builder()
                .symbol(symbol)
                .interval(timeFrame.getApiValue())
                .startDate(date)
                .bars(List.of(bar))
                .build();
        return historicalService.cacheHistoricalBar(List.of(stockBars));
    }

    /**
     * Get bars for a symbol with cache statistics tracking
     */
    public StockBars getBarsWithStats(String symbol, String interval, String date) {
        return symbolService.getBarsWithStats(symbol, interval, date);
    }

    /**
     * Get today's intraday bars for a symbol
     */
    public StockBars getTodayIntradayBars(String symbol, String interval) {
        return symbolService.getTodayIntradayBars(symbol, interval);
    }

    /**
     * Clear intraday data for a specific date
     */
    public long clearIntradayDataForDate(String date) {
        return stockRedisCache.clearIntradayDataForDate(date);
    }

    /**
     * Clear today's intraday data
     */
    public long clearTodayIntradayData() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        return clearIntradayDataForDate(today);
    }

    /**
     * Get combined cache hit statistics from all services
     */
    public Map<String, Integer> getCacheHitStats() {
        Map<String, Integer> combinedStats = new ConcurrentHashMap<>();
        combinedStats.putAll(historicalService.getCacheHitStats());
        combinedStats.putAll(symbolService.getCacheHitStats());
        return combinedStats;
    }

    /**
     * Get combined cache miss statistics from all services
     */
    public Map<String, Integer> getCacheMissStats() {
        Map<String, Integer> combinedStats = new ConcurrentHashMap<>();
        combinedStats.putAll(historicalService.getCacheMissStats());
        combinedStats.putAll(symbolService.getCacheMissStats());
        return combinedStats;
    }

    /**
     * Reset cache statistics in all services
     */
    public void resetCacheStats() {
        historicalService.resetCacheStats();
        symbolService.resetCacheStats();
    }

    /**
     * Get bars for multiple symbols with cache statistics tracking
     */
    public Map<String, StockBars> getMultiSymbolBarsWithStats(List<String> symbols, String interval, String date) {
        return symbolService.getMultiSymbolBarsWithStats(symbols, interval, date);
    }

    /**
     * Get today's bars for multiple symbols
     */
    public Map<String, StockBars> getTodayMultiSymbolBars(List<String> symbols, String interval) {
        return symbolService.getTodayMultiSymbolBars(symbols, interval);
    }

    /**
     * Get historical bars for multiple symbols within a date range
     * 
     * @param symbols       List of symbols to retrieve
     * @param startDate     Start date (yyyy-MM-dd)
     * @param endDate       End date (yyyy-MM-dd)
     * @param interval      Time interval
     * @param isIndexSymbol If true, check index cache first before falling back to
     *                      stock cache
     * @return Map of symbol to list of StockBars
     */
    public Map<String, List<StockBars>> getHistoricalBarsWithStats(List<String> symbols, String startDate,
            String endDate, String interval, boolean isIndexSymbol) {

        // If this is an index symbol request and we only have one symbol, check index
        // cache first
        if (isIndexSymbol && symbols.size() == 1) {
            String indexSymbol = symbols.get(0);
            String cacheKey = String.format("index:historical:%s:%s:%s:%s",
                    indexSymbol, interval, startDate, endDate);

            log.info("[INDEX_CACHE]", String.format(
                    "Checking index cache for symbol: %s, interval: %s, dateRange: %s to %s",
                    indexSymbol, interval, startDate, endDate));

            // Try to get from index cache
            Map<String, String> indexCacheData = stockRedisCache.getIndexHistoricalData(cacheKey);

            if (indexCacheData != null && !indexCacheData.isEmpty()) {
                log.info("[INDEX_CACHE]", String.format(
                        "Index cache HIT for %s with %d constituent symbols",
                        indexSymbol, indexCacheData.size()));

                // Convert cached data back to StockBars format
                // For now, we'll fall through to regular cache since the index cache
                // stores HistoricalData format, not StockBars format
                // This is a design consideration - we may need to align formats
                log.debug("[INDEX_CACHE]",
                        "Index cache data found but format conversion needed, falling back to stock cache");
            } else {
                log.info("[INDEX_CACHE]", String.format(
                        "Index cache MISS for %s, falling back to stock cache",
                        indexSymbol));
            }
        }

        // Fall back to regular historical service (stock-level cache)
        return historicalService.getHistoricalBarsWithStats(symbols, startDate, endDate, interval);
    }

    /**
     * Get historical bars for multiple symbols within a date range (backward
     * compatibility)
     */
    public Map<String, List<StockBars>> getHistoricalBarsWithStats(List<String> symbols, String startDate,
            String endDate, String interval) {
        return getHistoricalBarsWithStats(symbols, startDate, endDate, interval, false);
    }

    /**
     * Process raw price data into interval-based bars and cache them
     * 
     * @param symbol    The stock symbol
     * @param rawPrices List of raw price points with timestamp, price, and volume
     * @param date      The date for which to calculate bars
     * @return Map of interval to success status
     */
    public Map<String, Boolean> processAndCacheRawPriceData(String symbol, List<OHLCV> rawPrices, LocalDate date) {
        if (rawPrices == null || rawPrices.isEmpty()) {
            log.warn("processAndCacheRawPriceData", "No raw price data provided for symbol: " + symbol);
            return Map.of();
        }

        Map<String, Boolean> result = new HashMap<>();

        try {
            // Process and cache intraday bars
            for (String interval : INTRADAY_INTERVALS) {
                List<OHLCV> bars = barCalculator.calculateBars(rawPrices, interval, date);
                if (!bars.isEmpty()) {
                    // Create StockBars object for the symbol service
                    StockBars stockBars = StockBars.builder()
                            .symbol(symbol)
                            .interval(interval)
                            .startDate(date.format(DATE_FORMATTER))
                            .bars(bars)
                            .build();
                    boolean success = symbolService.cacheIntradayBars(List.of(stockBars));
                    result.put(interval, success);

                    log.debug("processAndCacheRawPriceData", String.format(
                            "Processed and cached %d %s bars for %s on %s with Redis key 'stock:intraday:%s:%s:%s'",
                            bars.size(), interval, symbol, date, symbol.toUpperCase(), interval,
                            date.format(DATE_FORMATTER)));
                }
            }

            // Process and cache daily bar
            // List<OHLCV> dailyBars = barCalculator.calculateBars(rawPrices,
            // INTERVAL_1_DAY, date);
            // if (!dailyBars.isEmpty()) {
            // // Create StockBars object for the historical service
            // StockBars stockBars = StockBars.builder()
            // .symbol(symbol)
            // .interval(INTERVAL_1_DAY)
            // .startDate(date.format(DATE_FORMATTER))
            // .bars(List.of(dailyBars.get(0)))
            // .build();
            // boolean success = historicalService.cacheHistoricalBar(List.of(stockBars));
            // result.put(INTERVAL_1_DAY, success);
            // log.debug("Processed and cached daily bar for {} on {} with Redis key
            // 'stock:historical:{}:{}:{}'",
            // symbol, date, symbol.toUpperCase(), INTERVAL_1_DAY,
            // date.format(DATE_FORMATTER));
            // }

        } catch (Exception e) {
            log.error("processAndCacheRawPriceData",
                    "Error processing raw price data for " + symbol + ": " + e.getMessage());
        }

        return result;
    }

    /**
     * Process raw price data for multiple symbols and cache them
     * 
     * @param symbolPrices Map of symbol to raw price points
     * @param date         The date for which to calculate bars
     * @return Map of symbol to interval results
     */
    public Map<String, Map<String, Boolean>> processAndCacheMultiSymbolData(
            Map<String, List<OHLCV>> symbolPrices, LocalDate date) {

        Map<String, Map<String, Boolean>> result = new HashMap<>();

        for (Map.Entry<String, List<OHLCV>> entry : symbolPrices.entrySet()) {
            String symbol = entry.getKey();
            List<OHLCV> prices = entry.getValue();

            Map<String, Boolean> symbolResult = processAndCacheRawPriceData(symbol, prices, date);
            result.put(symbol, symbolResult);
        }

        return result;
    }

    public static OHLCV createPricePoint(LocalDateTime timestamp, double open, double high, double low, double close,
            long volume, Double lastPrice) {
        return OHLCV.builder()
                .time(timestamp)
                .open(open)
                .high(high)
                .low(low)
                .close(close)
                .volume(volume)
                .lastPrice(lastPrice)
                .build();
    }

    public void setActiveProvider(String providerName) {
        stockRedisCache.setActiveProvider(providerName);
    }

    public String getActiveProvider() {
        return stockRedisCache.getActiveProvider();
    }

    /**
     * Cache index-level historical data
     */
    public void cacheIndexHistoricalData(String cacheKey, Map<String, String> hashData) {
        stockRedisCache.cacheIndexHistoricalData(cacheKey, hashData);
    }

    /**
     * Get index-level historical data
     */
    public Map<String, String> getIndexHistoricalData(String cacheKey) {
        return stockRedisCache.getIndexHistoricalData(cacheKey);
    }
}
