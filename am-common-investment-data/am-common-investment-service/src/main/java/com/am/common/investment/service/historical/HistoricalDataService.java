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
     *
     * @param tradingSymbol The trading symbol or identifier for the instrument
     * @param fromDate The start date for the historical data (inclusive)
     * @param toDate The end date for the historical data (inclusive)
     * @param interval The time interval for data points (e.g., "1d", "1h", "15m")
     * @return Optional containing the historical data if found, empty otherwise
     */
    Optional<HistoricalData> getHistoricalData(String tradingSymbol, Instant fromDate, Instant toDate, String interval);
    
    /**
     * Retrieves the latest available historical data for a trading symbol with a specified lookback period and interval.
     *
     * @param tradingSymbol The trading symbol or identifier for the instrument
     * @param lookbackPeriod The number of periods to look back (e.g., 30 for 30 days if interval is "1d")
     * @param interval The time interval for data points (e.g., "1d", "1h", "15m")
     * @return Optional containing the historical data if found, empty otherwise
     */
    Optional<HistoricalData> getRecentHistoricalData(String tradingSymbol, int lookbackPeriod, String interval);

    /**
     * Retrieves the latest available historical data for a trading symbol with a specified lookback period and interval.
     *
     * @param tradingSymbol The trading symbol or identifier for the instrument
     * @param lookbackPeriod The number of periods to look back (e.g., 30 for 30 days if interval is "1d")
     * @param interval The time interval for data points (e.g., "1d", "1h", "15m")
     * @return Optional containing the historical data if found, empty otherwise
     */
    Optional<HistoricalData> saveHistoricalData(HistoricalData historicalData);
}
