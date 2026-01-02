package com.am.marketdata.redis.util;

import static com.am.marketdata.common.constants.TimeIntervalConstants.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * Utility class for generating standardized Redis cache keys.
 * Centralizes key generation logic to ensure consistency across the application.
 */
public class CacheKeyGenerator {

    private static final String INTRADAY_PREFIX = "stock:intraday";
    private static final String HISTORICAL_PREFIX = "stock:historical";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Generate a Redis key for intraday data
     *
     * @param symbol The market symbol
     * @param interval The time interval
     * @param date The date for the data
     * @return Formatted Redis key
     */
    public static String generateIntradayKey(String symbol, String interval, LocalDate date) {
        return String.format("%s:%s:%s:%s", 
                INTRADAY_PREFIX, symbol.toUpperCase(), interval, date.format(DATE_FORMATTER));
    }

    /**
     * Generate a Redis key for historical data
     *
     * @param symbol The market symbol
     * @param interval The time interval
     * @param date The date for the data
     * @return Formatted Redis key
     */
    public static String generateHistoricalKey(String symbol, String interval, LocalDate date) {
        return String.format("%s:%s:%s:%s", 
                HISTORICAL_PREFIX, symbol.toUpperCase(), interval, date.format(DATE_FORMATTER));
    }
    
    /**
     * Get the list of intraday intervals
     * 
     * @return List of intraday intervals
     */
    public static Set<String> getIntradayIntervals() {
        return INTRADAY_INTERVALS;
    }
    
    /**
     * Get the list of historical intervals
     * 
     * @return List of historical intervals
     */
    public static Set<String> getHistoricalIntervals() {
        return HISTORICAL_INTERVALS;
    }
    
    /**
     * Determine if an interval is for historical data
     * 
     * @param interval The interval to check
     * @return true if the interval is for historical data
     */
    public static boolean isHistoricalInterval(String interval) {
        return HISTORICAL_INTERVALS.contains(interval);
    }
    
    /**
     * Determine if an interval is for intraday data
     * 
     * @param interval The interval to check
     * @return true if the interval is for intraday data
     */
    public static boolean isIntradayInterval(String interval) {
        return INTRADAY_INTERVALS.contains(interval);
    }
}
