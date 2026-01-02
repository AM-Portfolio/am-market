package com.am.marketdata.common.model;

/**
 * Enum representing time frames/intervals for historical data.
 * Maps user-friendly values to Zerodha and Upstox API compatible values.
 * For timeframes not directly supported, client-side aggregation is assumed or
 * fallback is used.
 */
public enum TimeFrame {
    MINUTE("1m", "minute", "I1"),
    THREE_MINUTE("3m", "3minute", "I1"),
    FIVE_MINUTE("5m", "5minute", "I1"),
    TEN_MINUTE("10m", "10minute", "I1"),
    FIFTEEN_MINUTE("15m", "15minute", "I1"),
    THIRTY_MINUTE("30m", "30minute", "I30"),
    HOUR("1H", "60minute", "1d"),
    FOUR_HOUR("4H", "60minute", "1d", true, 4), // 4-hour - requires aggregation from hourly data
    DAY("1D", "60minute", "1d"),
    WEEK("1W", "60minute", "week", true, 7), // Weekly - requires aggregation from daily data
    MONTH("1M", "60minute", "month", true, 30), // Monthly - requires aggregation from daily data
    YEAR("1Y", "60minute", "year", true, 365);

    private final String userValue;
    private final String zerodhaValue;
    private final String upStockValue;
    private final boolean requiresAggregation;
    private final int aggregationFactor;

    TimeFrame(String userValue, String zerodhaValue, String upStockValue) {
        this(userValue, zerodhaValue, upStockValue, false, 1);
    }

    TimeFrame(String userValue, String zerodhaValue, String upStockValue, boolean requiresAggregation,
            int aggregationFactor) {
        this.userValue = userValue;
        this.zerodhaValue = zerodhaValue;
        this.upStockValue = upStockValue;
        this.requiresAggregation = requiresAggregation;
        this.aggregationFactor = aggregationFactor;
    }

    /**
     * Get the value used in API requests
     * 
     * @return API value
     */
    public String getApiValue() {
        return userValue;
    }

    /**
     * Get the value used for Zerodha API
     * 
     * @return Zerodha API value
     */
    public String getZerodhaValue() {
        return zerodhaValue;
    }

    /**
     * Get the value used for Upstox API
     * 
     * @return Upstox API value
     */
    public String getUpStockValue() {
        return upStockValue;
    }

    /**
     * Check if this timeframe requires client-side aggregation
     * 
     * @return true if aggregation is required
     */
    public boolean requiresAggregation() {
        return requiresAggregation;
    }

    /**
     * Get the factor by which to aggregate base timeframe data
     * For example, FOUR_HOUR aggregates 4 hourly candles
     * 
     * @return aggregation factor
     */
    public int getAggregationFactor() {
        return aggregationFactor;
    }

    /**
     * Get the base timeframe used for fetching data when aggregation is required
     * 
     * @return base TimeFrame for aggregation
     */
    public TimeFrame getBaseTimeFrame() {
        if (!requiresAggregation) {
            return this;
        }

        switch (this) {
            case FOUR_HOUR:
                return HOUR;
            case WEEK:
            case MONTH:
            case YEAR:
                return DAY;
            default:
                return this;
        }
    }

    /**
     * Find TimeFrame by API value
     * 
     * @param value API value to search for
     * @return Matching TimeFrame or null if not found
     * @throws IllegalArgumentException if value is null or no matching TimeFrame is
     *                                  found
     */
    public static TimeFrame fromApiValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("API value cannot be null");
        }

        for (TimeFrame timeFrame : TimeFrame.values()) {
            if (timeFrame.getApiValue().equalsIgnoreCase(value)) {
                return timeFrame;
            }
        }

        // Support aliases if needed (e.g. I1, I30)
        if ("I1".equalsIgnoreCase(value))
            return MINUTE;
        if ("I30".equalsIgnoreCase(value))
            return THIRTY_MINUTE;

        throw new IllegalArgumentException("No TimeFrame found for API value: " + value);
    }

    /**
     * Flexible parsing that accepts both enum names (DAY, HOUR, MINUTE) and API
     * values (1D, 1H, 5m)
     * This is useful for JSON deserialization where clients might send either
     * format
     * 
     * @param value Either enum name or API value
     * @return Matching TimeFrame
     * @throws IllegalArgumentException if value is null or no matching TimeFrame is
     *                                  found
     */
    public static TimeFrame fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("TimeFrame value cannot be null");
        }

        // First, try to match as enum name (DAY, HOUR, MINUTE, etc.)
        try {
            return TimeFrame.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Not an enum name, try API value (1D, 1H, 5m, etc.)
            return fromApiValue(value);
        }
    }

    /**
     * Convert API value to Zerodha value
     * 
     * @param apiValue API value to convert
     * @return Zerodha compatible value
     * @throws IllegalArgumentException if apiValue is null or no matching TimeFrame
     *                                  is found
     */
    public static String toZerodhaValue(String apiValue) {
        return fromApiValue(apiValue).getZerodhaValue();
    }

    /**
     * Convert API value to Upstox value
     * 
     * @param apiValue API value to convert
     * @return Upstox compatible value
     * @throws IllegalArgumentException if apiValue is null or no matching TimeFrame
     *                                  is found
     */
    public static String toUpStockValue(String apiValue) {
        return fromApiValue(apiValue).getUpStockValue();
    }
}
