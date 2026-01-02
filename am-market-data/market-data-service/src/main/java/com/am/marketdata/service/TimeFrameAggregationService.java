package com.am.marketdata.service;

import com.am.common.investment.model.historical.HistoricalData;
import com.am.marketdata.common.model.TimeFrame;

import java.util.Date;

/**
 * Service for aggregating historical data into higher timeframes
 * that are not directly supported by data providers.
 */
public interface TimeFrameAggregationService {

    /**
     * Aggregate historical data to a higher timeframe
     * 
     * @param sourceData The source historical data in a lower timeframe
     * @param targetTimeFrame The target timeframe to aggregate to
     * @return Aggregated historical data
     */
    HistoricalData aggregateTimeFrame(HistoricalData sourceData, TimeFrame targetTimeFrame);
    
    /**
     * Check if a timeframe requires client-side aggregation
     * 
     * @param timeFrame The timeframe to check
     * @return true if the timeframe requires aggregation
     */
    boolean requiresAggregation(TimeFrame timeFrame);
    
    /**
     * Get the base timeframe needed to aggregate to the target timeframe
     * 
     * @param targetTimeFrame The target timeframe
     * @return The base timeframe needed for aggregation
     */
    TimeFrame getBaseTimeFrame(TimeFrame targetTimeFrame);
    
    /**
     * Calculate the adjusted date range needed to fetch enough data for aggregation
     * 
     * @param fromDate Original from date
     * @param toDate Original to date
     * @param targetTimeFrame Target timeframe
     * @return Adjusted date range as [fromDate, toDate]
     */
    Date[] calculateAdjustedDateRange(Date fromDate, Date toDate, TimeFrame targetTimeFrame);
}
