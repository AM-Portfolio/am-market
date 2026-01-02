package com.am.marketdata.redis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a collection of OHLCV bars for a specific stock, interval, and date.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockBars {
    private String symbol;
    private String interval;
    private String startDate;
    private String endDate;
    private List<OHLCV> bars;
}
