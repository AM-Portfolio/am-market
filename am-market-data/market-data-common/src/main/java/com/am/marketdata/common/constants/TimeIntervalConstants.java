package com.am.marketdata.common.constants;

import java.util.Set;

/**
 * Constants for time intervals used throughout the market data application.
 * Centralizes interval definitions to ensure consistency across modules.
 */
public final class TimeIntervalConstants {

    // Intraday intervals
    public static final String INTERVAL_1_MINUTE = "1m";
    public static final String INTERVAL_5_MINUTE = "5m";
    public static final String INTERVAL_10_MINUTE = "10m";
    public static final String INTERVAL_15_MINUTE = "15m";
    public static final String INTERVAL_30_MINUTE = "30m";
    public static final String INTERVAL_1_HOUR = "1H";
    public static final String INTERVAL_4_HOUR = "4H";
    
    // Historical intervals
    public static final String INTERVAL_1_DAY = "1D";
    public static final String INTERVAL_1_WEEK = "1W";
    public static final String INTERVAL_1_MONTH = "1M";
    public static final String INTERVAL_1_YEAR = "1Y";
    
    // Sets of valid intervals
    public static final Set<String> INTRADAY_INTERVALS = Set.of(
            INTERVAL_5_MINUTE, 
            INTERVAL_10_MINUTE, 
            INTERVAL_15_MINUTE, 
            INTERVAL_30_MINUTE, 
            INTERVAL_1_HOUR, 
            INTERVAL_4_HOUR
    );
    
    public static final Set<String> HISTORICAL_INTERVALS = Set.of(
            INTERVAL_1_DAY, 
            INTERVAL_1_WEEK, 
            INTERVAL_1_MONTH, 
            INTERVAL_1_YEAR
    );
    
    // Combined set of all valid intervals
    public static final Set<String> VALID_INTERVALS = Set.of(
            INTERVAL_5_MINUTE, 
            INTERVAL_10_MINUTE, 
            INTERVAL_15_MINUTE, 
            INTERVAL_30_MINUTE, 
            INTERVAL_1_HOUR, 
            INTERVAL_4_HOUR,
            INTERVAL_1_DAY, 
            INTERVAL_1_WEEK, 
            INTERVAL_1_MONTH, 
            INTERVAL_1_YEAR
    );
    
    // Private constructor to prevent instantiation
    private TimeIntervalConstants() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }
}
