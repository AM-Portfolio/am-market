package com.am.common.investment.persistence.influx.measurement;

public final class MarketIndexMeasurementFields {
    private MarketIndexMeasurementFields() {
        // Private constructor to prevent instantiation
    }

    // Market Data Fields
    public static final String MARKET_DATA_OPEN = "market_data_open";
    public static final String MARKET_DATA_PREVIOUS_CLOSE = "market_data_previous_close";
    public static final String MARKET_DATA_HIGH = "market_data_high";
    public static final String MARKET_DATA_LOW = "market_data_low";
    public static final String MARKET_DATA_CLOSE = "market_data_close";
    public static final String MARKET_DATA_PERCENTAGE_CHANGE = "market_data_percentage_change";

    // Fundamental Fields
    public static final String FUNDAMENTAL_PRICE_EARNING = "fundamental_price_earning";
    public static final String FUNDAMENTAL_PRICE_BOOK = "fundamental_price_book";
    public static final String FUNDAMENTAL_DIVIDEND_YIELD = "fundamental_dividend_yield";

    // Market Breadth Fields
    public static final String MARKET_BREADTH_ADVANCES = "market_breadth_advances";
    public static final String MARKET_BREADTH_DECLINES = "market_breadth_declines";
    public static final String MARKET_BREADTH_UNCHANGED = "market_breadth_unchanged";

    // Historical Comparison Fields
    public static final String HISTORICAL_COMPARISON_VALUE = "historical_comparison_value";
    public static final String HISTORICAL_COMPARISON_PER_CHANGE_365D = "historical_comparison_per_change_365d";
    public static final String HISTORICAL_COMPARISON_DATE_365D_AGO = "historical_comparison_date_365d_ago";
    public static final String HISTORICAL_COMPARISON_PER_CHANGE_30D = "historical_comparison_per_change_30d";
    public static final String HISTORICAL_COMPARISON_DATE_30D_AGO = "historical_comparison_date_30d_ago";
    public static final String HISTORICAL_COMPARISON_PREVIOUS_DAY = "historical_comparison_previous_day";
    public static final String HISTORICAL_COMPARISON_ONE_WEEK_AGO = "historical_comparison_one_week_ago";
    public static final String HISTORICAL_COMPARISON_ONE_MONTH_AGO = "historical_comparison_one_month_ago";
    public static final String HISTORICAL_COMPARISON_ONE_YEAR_AGO = "historical_comparison_one_year_ago";
}
