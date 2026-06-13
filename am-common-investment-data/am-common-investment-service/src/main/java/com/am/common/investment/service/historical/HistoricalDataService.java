package com.am.common.investment.service.historical;

import com.am.common.investment.model.historical.HistoricalData;

import java.time.Instant;
import java.util.Optional;

/**
 * Service interface for retrieving historical financial data
 * with support for different time intervals and instruments.
 */
public interface HistoricalDataService {
    
    /**
     * Retrieves historical price data for a given trading symbol within a specified time range and interval.
     */
    Optional<HistoricalData> getHistoricalData(String tradingSymbol, Instant fromDate, Instant toDate, String interval, boolean isIndexSymbol);
    
    default Optional<HistoricalData> getHistoricalData(String tradingSymbol, Instant fromDate, Instant toDate, String interval) {
        return getHistoricalData(tradingSymbol, fromDate, toDate, interval, false);
    }
    
    /**
     * Retrieves the latest available historical data for a trading symbol with a specified lookback period and interval.
     *
     * @param tradingSymbol The trading symbol or identifier for the instrument
     * @param lookbackPeriod The number of periods to look back
     * @param interval The time interval for data points
     * @return Optional containing the historical data if found, empty otherwise
     */
    Optional<HistoricalData> getRecentHistoricalData(String tradingSymbol, int lookbackPeriod, String interval);

    /**
     * Saves historical data.
     */
    Optional<HistoricalData> saveHistoricalData(HistoricalData historicalData);
}
