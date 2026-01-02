package com.am.marketdata.redis.cache;

import static com.am.marketdata.common.constants.TimeIntervalConstants.HISTORICAL_INTERVALS;
import static com.am.marketdata.common.constants.TimeIntervalConstants.INTRADAY_INTERVALS;

import com.am.marketdata.redis.model.OHLCV;
import com.am.marketdata.redis.model.StockBars;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import com.am.marketdata.common.log.AppLogger;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.Objects;

/**
 * Redis-based cache for stock price data, supporting both intraday and
 * historical data.
 * Implements key design, TTL strategy, and helper functions for managing stock
 * price data.
 */
@Component
@RequiredArgsConstructor
public class StockRedisCache {

    private final AppLogger log = AppLogger.getLogger();

    private final RedisTemplate<String, String> redisTemplate;

    private final ObjectMapper redisObjectMapper;

    @Value("${redis.cache.ttl.historical:86400}") // Default: 24 hours
    private long historicalTtlSeconds;

    @Value("${redis.cache.ttl.intraday.past:14400}") // Default: 4 hours
    private long intradayPastTtlSeconds;

    @Value("${redis.cache.ttl.intraday.future:86400}") // Default: 24 hours
    private long intradayFutureTtlSeconds;

    @Value("${redis.cache.ttl.intraday.buffer:14400}") // Default: 4 hours after end of day
    private long intradayBufferSeconds;

    // Key prefixes
    private static final String INTRADAY_PREFIX = "stock:intraday";
    private static final String HISTORICAL_PREFIX = "stock:historical";

    /**
     * Saves a list of StockBars containing intraday data with appropriate TTL.
     * 
     * @param stockBarsList List of StockBars objects containing intraday data
     * @return true if saved successfully, false otherwise
     */
    /**
     * Saves a list of StockBars containing intraday data with appropriate TTL using Pipelining.
     * 
     * @param stockBarsList List of StockBars objects containing intraday data
     * @return true if saved successfully, false otherwise
     */
    public boolean saveIntradayBars(List<StockBars> stockBarsList) {
        if (stockBarsList == null || stockBarsList.isEmpty()) {
            log.warn("saveIntradayBars", "Empty or null StockBars list provided for intraday data");
            return false;
        }

        try {
            redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                for (StockBars stockBars : stockBarsList) {
                    String symbol = stockBars.getSymbol();
                    String interval = stockBars.getInterval();
                    String date = stockBars.getStartDate();
                    List<OHLCV> bars = stockBars.getBars();

                    try {
                        validateInterval(interval);
                        // validateDate(date); // Skip validation inside loop for performance, or keep it? Better to catch inside.

                        String key = generateKey(INTRADAY_PREFIX, symbol, interval, date);
                        String json = redisObjectMapper.writeValueAsString(bars);
                        long ttlSeconds = calculateIntradayTtl(date);

                        // Low-level connection access requires bytes
                        byte[] keyBytes = redisTemplate.getStringSerializer().serialize(key);
                        byte[] valueBytes = redisTemplate.getStringSerializer().serialize(json);

                        connection.setEx(keyBytes, ttlSeconds, valueBytes);

                    } catch (Exception e) {
                        log.error("saveIntradayBars", "Error preparing batch for " + symbol + ": " + e.getMessage());
                    }
                }
                return null;
            });
            
            return true;
        } catch (Exception e) {
            log.error("saveIntradayBars", "Batch save failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Saves intraday bars for a specific symbol, interval, and date with
     * appropriate TTL.
     * 
     * @param symbol   Stock symbol (e.g., "AAPL")
     * @param interval Time interval (e.g., "5m", "15m", "1h")
     * @param date     Date in YYYY-MM-DD format
     * @param bars     List of OHLCV bars
     * @return true if saved successfully, false otherwise
     */
    public boolean saveIntradayBars(String symbol, String interval, String date, List<OHLCV> bars) {
        validateInterval(interval);
        validateDate(date);

        String key = generateKey(INTRADAY_PREFIX, symbol, interval, date);

        try {
            // Store only the bars list instead of the entire StockBars object
            String json = redisObjectMapper.writeValueAsString(bars);

            // Calculate TTL: end of day + 4 hours
            long ttlSeconds = calculateIntradayTtl(date);

            redisTemplate.opsForValue().set(key, json, ttlSeconds, TimeUnit.SECONDS);
            log.debug("saveIntradayBars",
                    String.format("Saved %d intraday bars for %s, interval: %s, date: %s, TTL: %d seconds",
                            bars.size(), symbol, interval, date, ttlSeconds));
            return true;
        } catch (JsonProcessingException e) {
            log.error("saveIntradayBars", "Failed to serialize stock bars for " + symbol + ": " + e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("saveIntradayBars", "Error saving intraday bars for " + symbol + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Saves a list of StockBars containing historical data with appropriate TTL.
     * 
     * @param stockBarsList List of StockBars objects containing historical data
     * @return true if saved successfully, false otherwise
     */
    /**
     * Saves a list of StockBars containing historical data with appropriate TTL using Pipelining.
     * 
     * @param stockBarsList List of StockBars objects containing historical data
     * @return true if saved successfully, false otherwise
     */
    public boolean saveHistoricalBar(List<StockBars> stockBarsList) {
        if (stockBarsList == null || stockBarsList.isEmpty()) {
            log.warn("saveHistoricalBar", "Empty or null StockBars list provided for historical data");
            return false;
        }

        try {
            redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                for (StockBars stockBars : stockBarsList) {
                    String symbol = stockBars.getSymbol();
                    String interval = stockBars.getInterval(); // Should be 1d usually
                    List<OHLCV> bars = stockBars.getBars();

                    if (bars == null || bars.isEmpty()) continue;

                    for (OHLCV bar : bars) {
                        try {
                            String barDate = bar.getTime().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
                            String key = generateKey(HISTORICAL_PREFIX, symbol, interval, barDate);
                            
                            String json = redisObjectMapper.writeValueAsString(bar);
                            long ttlSeconds = 86400; // 24 Hours

                            byte[] keyBytes = redisTemplate.getStringSerializer().serialize(key);
                            byte[] valueBytes = redisTemplate.getStringSerializer().serialize(json);

                            connection.setEx(keyBytes, ttlSeconds, valueBytes);
                        } catch (Exception e) {
                            log.error("saveHistoricalBar", "Error processing bar for " + symbol + ": " + e.getMessage());
                        }
                    }
                }
                return null;
            });

            log.debug("saveHistoricalBar", "Executed pipeline for " + stockBarsList.size() + " stock bar sets");
            return true;
        } catch (Exception e) {
            log.error("saveHistoricalBar", "Batch save failed", e);
            return false;
        }
    }

    /**
     * Saves a historical (daily) bar for a specific symbol and date with 24-hour
     * TTL.
     * 
     * @param symbol Stock symbol (e.g., "AAPL")
     * @param date   Date in YYYY-MM-DD format
     * @param bar    OHLCV bar for the day
     * @return true if saved successfully, false otherwise
     */
    public boolean saveHistoricalBar(String symbol, String date, OHLCV bar, String interval) {
        validateDate(date);

        String key = generateKey(HISTORICAL_PREFIX, symbol, interval, date);

        try {
            // Store only the bar data instead of the entire StockBars object
            String json = redisObjectMapper.writeValueAsString(bar);

            // Historical data TTL: 24 hours (86400 seconds)
            long ttlSeconds = 86400;

            redisTemplate.opsForValue().set(key, json, ttlSeconds, TimeUnit.SECONDS);
            log.debug("saveHistoricalBar",
                    String.format("Saved historical bar for %s, date: %s, TTL: %d seconds", symbol, date, ttlSeconds));
            return true;
        } catch (JsonProcessingException e) {
            log.error("saveHistoricalBar", "Failed to serialize historical bar for " + symbol + ": " + e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("saveHistoricalBar", "Error saving historical bar for " + symbol + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves bars for a specific symbol, interval, and date.
     * 
     * @param symbol   Stock symbol (e.g., "AAPL")
     * @param interval Time interval (e.g., "5m", "15m", "1h", "1d")
     * @param date     Date in YYYY-MM-DD format
     * @return StockBars object containing the bars, or null if not found or error
     */
    public StockBars getBars(String symbol, String interval, String date) {
        validateInterval(interval);
        validateDate(date);

        String prefix = HISTORICAL_INTERVALS.contains(interval) ? HISTORICAL_PREFIX : INTRADAY_PREFIX;
        String key = generateKey(prefix, symbol, interval, date);

        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                log.debug("getBars",
                        String.format("No data found for %s, interval: %s, date: %s", symbol, interval, date));
                return null;
            }

            // Reconstruct StockBars from stored bar data
            if (HISTORICAL_INTERVALS.contains(interval)) {
                // For historical data, we stored a single OHLCV
                OHLCV bar = redisObjectMapper.readValue(json, OHLCV.class);
                return StockBars.builder()
                        .symbol(symbol)
                        .interval(interval)
                        .startDate(date)
                        .endDate(date)
                        .bars(Collections.singletonList(bar))
                        .build();
            } else {
                // For intraday data, we stored a List<OHLCV>
                List<OHLCV> bars = redisObjectMapper.readValue(json,
                        new TypeReference<List<OHLCV>>() {
                        });
                return StockBars.builder()
                        .symbol(symbol)
                        .interval(interval)
                        .startDate(date)
                        .endDate(date)
                        .bars(bars)
                        .build();
            }
        } catch (JsonProcessingException e) {
            log.error("getBars", "Failed to deserialize stock bars for " + symbol + ": " + e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("getBars", "Error retrieving bars for " + symbol + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Clears all intraday data for a specific date.
     * 
     * @param date Date in YYYY-MM-DD format
     * @return Number of keys deleted
     */
    public long clearIntradayDataForDate(String date) {
        validateDate(date);

        String pattern = INTRADAY_PREFIX + ":*:*:" + date;
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys == null || keys.isEmpty()) {
            log.debug("clearIntradayDataForDate", "No intraday data found for date: {}", date);
            return 0;
        }

        Long deletedCount = redisTemplate.delete(keys);
        log.debug("clearIntradayDataForDate", "Deleted {} intraday keys for date: {}", deletedCount, date);

        return deletedCount != null ? deletedCount : 0;
    }

    /**
     * Retrieves bars for multiple symbols with the same interval and date in a
     * single operation.
     * 
     * @param symbols  List of stock symbols (e.g., ["AAPL", "MSFT", "GOOG"])
     * @param interval Time interval (e.g., "5m", "15m", "1h", "1d")
     * @param date     Date in YYYY-MM-DD format
     * @return Map of symbol to StockBars objects
     */
    public Map<String, StockBars> getMultiSymbolBars(List<String> symbols, String interval, String date) {
        validateInterval(interval);
        validateDate(date);

        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyMap();
        }

        // Determine prefix based on date and interval:
        // - TODAY's data (live market data) → always INTRADAY regardless of interval
        // - PAST dates with daily+ intervals → HISTORICAL
        // - PAST dates with sub-day intervals → INTRADAY
        LocalDate today = LocalDate.now();
        LocalDate queryDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        boolean isToday = queryDate.equals(today);

        String prefix;
        if (isToday) {
            // TODAY's live data is always intraday
            prefix = INTRADAY_PREFIX;
        } else {
            // Past data: use historical for daily+ intervals, intraday for sub-day
            prefix = HISTORICAL_INTERVALS.contains(interval) ? HISTORICAL_PREFIX : INTRADAY_PREFIX;
        }

        List<String> keys = new ArrayList<>(symbols.size());

        // Generate all keys to fetch
        for (String symbol : symbols) {
            keys.add(generateKey(prefix, symbol, interval, date));
        }

        // Log first 3 keys for debugging
        log.debug("getMultiSymbolBars", "[REDIS_TRACE] Fetching {} keys from Redis. First 3 keys: {}",
                keys.size(), keys.subList(0, Math.min(3, keys.size())));

        // Fetch all values in a single Redis operation
        List<String> jsonValues = redisTemplate.opsForValue().multiGet(keys);

        if (jsonValues == null) {
            log.warn("getMultiSymbolBars", "[REDIS_TRACE] Redis multiGet returned NULL for {} keys", keys.size());
            return Collections.emptyMap();
        }

        // Count how many values were found
        long foundCount = jsonValues.stream().filter(Objects::nonNull).count();
        log.debug("getMultiSymbolBars", "[REDIS_TRACE] Redis returned {} non-null values out of {} keys",
                foundCount, keys.size());

        Map<String, StockBars> result = new HashMap<>();

        // Process results
        for (int i = 0; i < symbols.size(); i++) {
            String json = jsonValues.get(i);
            if (json != null) {
                try {
                    String symbol = symbols.get(i);

                    log.debug("getMultiSymbolBars",
                            "[REDIS_PARSE] Processing symbol {} ({}/{}), JSON length: {}",
                            symbol, i + 1, symbols.size(), json.length());

                    // Reconstruct StockBars - use PREFIX (date-aware) NOT interval type
                    if (prefix.equals(HISTORICAL_PREFIX)) {
                        // For historical data, we stored a single OHLCV
                        OHLCV bar = redisObjectMapper.readValue(json, OHLCV.class);
                        StockBars stockBars = StockBars.builder()
                                .symbol(symbol)
                                .interval(interval)
                                .startDate(date)
                                .endDate(date)
                                .bars(Collections.singletonList(bar))
                                .build();
                        result.put(symbol, stockBars);
                        log.debug("getMultiSymbolBars", "[REDIS_PARSE] Successfully parsed HISTORICAL data for {}",
                                symbol);
                    } else {
                        // For intraday data, we stored a List<OHLCV>
                        List<OHLCV> bars = redisObjectMapper.readValue(json,
                                new TypeReference<List<OHLCV>>() {
                                });
                        StockBars stockBars = StockBars.builder()
                                .symbol(symbol)
                                .interval(interval)
                                .startDate(date)
                                .endDate(date)
                                .bars(bars)
                                .build();
                        result.put(symbol, stockBars);
                        log.debug("getMultiSymbolBars",
                                "[REDIS_PARSE] Successfully parsed INTRADAY data for {} with {} bars",
                                symbol, bars.size());

                    }
                } catch (JsonProcessingException e) {
                    log.error("getMultiSymbolBars",
                            "[REDIS_PARSE_ERROR] JSON deserialization failed for " + symbols.get(i) +
                                    ". Error: " + e.getMessage() +
                                    ". JSON preview: "
                                    + (json != null ? json.substring(0, Math.min(200, json.length())) : "null"));
                } catch (Exception e) {
                    log.error("getMultiSymbolBars",
                            "[REDIS_PARSE_ERROR] Unexpected error for " + symbols.get(i) + ": " + e.getMessage(), e);
                }
            }
        }

        return result;
    }

    /**
     * Retrieves historical bars for multiple symbols for a date range.
     * 
     * @param symbols   List of stock symbols (e.g., ["AAPL", "MSFT", "GOOG"])
     * @param startDate Start date in YYYY-MM-DD format (inclusive)
     * @param endDate   End date in YYYY-MM-DD format (inclusive)
     * @return Map of symbol to list of StockBars objects for each date in range
     */
    public Map<String, List<StockBars>> getMultiSymbolHistoricalBars(List<String> symbols, String startDate,
            String endDate, String interval) {
        validateDate(startDate);
        validateDate(endDate);

        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            // Parse dates and generate all dates in the range
            LocalDate start = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate end = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE);

            if (end.isBefore(start)) {
                throw new IllegalArgumentException("End date cannot be before start date");
            }

            List<String> dateRange = new ArrayList<>();
            LocalDate current = start;
            while (!current.isAfter(end)) {
                dateRange.add(current.format(DateTimeFormatter.ISO_LOCAL_DATE));
                current = current.plusDays(1);
            }

            // Initialize result map
            Map<String, List<StockBars>> result = new HashMap<>();
            for (String symbol : symbols) {
                result.put(symbol, new ArrayList<>());
            }

            // For each date, get all symbols' data
            for (String date : dateRange) {
                Map<String, StockBars> dailyData = getMultiSymbolBars(symbols, interval, date);

                // Add each symbol's data to its list
                for (Map.Entry<String, StockBars> entry : dailyData.entrySet()) {
                    result.get(entry.getKey()).add(entry.getValue());
                }
            }

            return result;

        } catch (DateTimeParseException e) {
            log.error("getMultiSymbolHistoricalBars", "Invalid date format: " + e.getMessage());
            throw new IllegalArgumentException("Invalid date format. Use YYYY-MM-DD");
        }
    }

    /**
     * Generates a Redis key in the format:
     * stock:<type>:<symbol>:<interval>:<YYYY-MM-DD>
     */
    private String generateKey(String prefix, String symbol, String interval, String date) {
        return String.format("%s:%s:%s:%s", prefix, symbol.toUpperCase(), interval, date);
    }

    /**
     * Calculates TTL for intraday data: end of day + 4 hours.
     * 
     * @param dateStr Date in YYYY-MM-DD format
     * @return TTL in seconds
     */
    private long calculateIntradayTtl(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        LocalDate now = LocalDate.now();

        // If the date is in the past, use configured past TTL
        if (date.isBefore(now)) {
            return intradayPastTtlSeconds;
        }

        // If the date is today, calculate seconds until end of day + buffer
        if (date.isEqual(now)) {
            LocalTime currentTime = LocalTime.now();
            int secondsUntilMidnight = 24 * 60 * 60 - currentTime.toSecondOfDay();
            return secondsUntilMidnight + intradayBufferSeconds; // End of day + buffer
        }

        // If the date is in the future, use configured future TTL
        return intradayFutureTtlSeconds;
    }

    /**
     * Validates if the provided interval is valid (either intraday or historical).
     * 
     * @param interval Time interval to validate
     * @throws IllegalArgumentException if interval is invalid
     */
    private void validateInterval(String interval) {
        // Convert to uppercase for case-insensitive comparison
        String normalizedInterval = interval.toUpperCase();

        // Check if the normalized interval matches any valid interval
        // (case-insensitive)
        boolean isValid = INTRADAY_INTERVALS.stream()
                .anyMatch(valid -> valid.equalsIgnoreCase(normalizedInterval)) ||
                HISTORICAL_INTERVALS.stream()
                        .anyMatch(valid -> valid.equalsIgnoreCase(normalizedInterval));

        if (!isValid) {
            throw new IllegalArgumentException("Invalid interval: " + interval +
                    ". Must be one of: " + String.join(", ", INTRADAY_INTERVALS) +
                    " or " + HISTORICAL_INTERVALS);
        }
    }

    /**
     * Validates if the provided date string is in the correct format (YYYY-MM-DD).
     * 
     * @param date Date string to validate
     * @throws IllegalArgumentException if date format is invalid
     */
    private void validateDate(String date) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
            LocalDate.parse(date, formatter);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + date +
                    ". Must be in YYYY-MM-DD format");
        }
    }

    /**
     * Sets the active market data provider.
     * 
     * @param providerName The name of the provider (e.g., "zerodha", "upstox")
     */
    public void setActiveProvider(String providerName) {
        if (providerName == null || providerName.trim().isEmpty()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set("market-data:config:active-provider", providerName);
            log.info("setActiveProvider", "Set active provider to: " + providerName);
        } catch (Exception e) {
            log.error("setActiveProvider", "Error setting active provider: " + e.getMessage());
        }
    }

    /**
     * Gets the active market data provider.
     * 
     * @return The provider name, or null if not set.
     */
    public String getActiveProvider() {
        try {
            return redisTemplate.opsForValue().get("market-data:config:active-provider");
        } catch (Exception e) {
            log.error("getActiveProvider", "Error getting active provider: " + e.getMessage());
            return null;
        }
    }

    /**
     * Cache index-level historical data as a Redis hash
     * 
     * @param cacheKey The Redis key for the index data
     * @param hashData Map of constituent symbol to serialized HistoricalData
     */
    public void cacheIndexHistoricalData(String cacheKey, Map<String, String> hashData) {
        try {
            if (hashData == null || hashData.isEmpty()) {
                log.warn("[INDEX_CACHE]", "No hash data to cache for key: " + cacheKey);
                return;
            }

            // Store as Redis hash
            redisTemplate.opsForHash().putAll(cacheKey, hashData);

            // Set TTL to 24 hours for index cache
            redisTemplate.expire(cacheKey, 24, TimeUnit.HOURS);

            log.info("[INDEX_CACHE]", String.format("Cached index data with key: %s (%d constituents)",
                    cacheKey, hashData.size()));
        } catch (Exception e) {
            log.error("[INDEX_CACHE]", "Error caching index historical data: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieve index-level historical data from Redis hash
     * 
     * @param cacheKey The Redis key for the index data
     * @return Map of constituent symbol to serialized HistoricalData, or null if
     *         not found
     */
    public Map<String, String> getIndexHistoricalData(String cacheKey) {
        try {
            Map<Object, Object> rawData = redisTemplate.opsForHash().entries(cacheKey);

            if (rawData == null || rawData.isEmpty()) {
                log.debug("[INDEX_CACHE]", "No index data found for key: " + cacheKey);
                return null;
            }

            // Convert Map<Object, Object> to Map<String, String>
            Map<String, String> result = new HashMap<>();
            for (Map.Entry<Object, Object> entry : rawData.entrySet()) {
                result.put(entry.getKey().toString(), entry.getValue().toString());
            }

            log.info("[INDEX_CACHE]", String.format("Retrieved index data from cache: %s (%d constituents)",
                    cacheKey, result.size()));
            return result;
        } catch (Exception e) {
            log.error("[INDEX_CACHE]", "Error retrieving index historical data: " + e.getMessage(), e);
            return null;
        }
    }
}
