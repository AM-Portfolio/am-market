package com.am.common.investment.persistence.influx.measurement;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.Data;

import java.time.Instant;

@Data
@Measurement(name = "market_index")
public class MarketIndexIndicesMeasurement {
    @Column(tag = true)
    private String key;

    @Column(tag = true)
    private String index;

    @Column(tag = true)
    private String indexSymbol;

    @Column(timestamp = true)
    private Instant time;

    // Market Data Fields
    @Column(name = "market_data_open")
    private Double marketDataOpen;

    @Column(name = "market_data_previous_close")
    private Double marketDataPreviousClose;

    @Column(name = "market_data_high")
    private Double marketDataHigh;

    @Column(name = "market_data_low")
    private Double marketDataLow;

    @Column(name = "market_data_close")
    private Double marketDataClose;

    @Column(name = "market_data_percentage_change")
    private Double marketDataPercentageChange;

    // Fundamental Fields
    @Column(name = "fundamental_price_earning")
    private Double fundamentalPriceEarning;

    @Column(name = "fundamental_price_book")
    private Double fundamentalPriceBook;

    @Column(name = "fundamental_dividend_yield")
    private Double fundamentalDividendYield;

    // Market Breadth Fields
    @Column(name = "market_breadth_advances")
    private Long marketBreadthAdvances;

    @Column(name = "market_breadth_declines")
    private Long marketBreadthDeclines;

    @Column(name = "market_breadth_unchanged")
    private Long marketBreadthUnchanged;

    // Historical Comparison Fields
    @Column(name = "historical_comparison_value")
    private Double historicalComparisonValue;

    @Column(name = "historical_comparison_per_change_365d")
    private Double historicalComparisonPerChange365d;

    @Column(name = "historical_comparison_date_365d_ago")
    private String historicalComparisonDate365dAgo;

    @Column(name = "historical_comparison_per_change_30d")
    private Double historicalComparisonPerChange30d;

    @Column(name = "historical_comparison_date_30d_ago")
    private String historicalComparisonDate30dAgo;

    @Column(name = "historical_comparison_previous_day")
    private Double historicalComparisonPreviousDay;

    @Column(name = "historical_comparison_one_week_ago")
    private Double historicalComparisonOneWeekAgo;

    @Column(name = "historical_comparison_one_month_ago")
    private Double historicalComparisonOneMonthAgo;

    @Column(name = "historical_comparison_one_year_ago")
    private Double historicalComparisonOneYearAgo;
}
