package com.am.marketdata.common.service;

import com.am.common.investment.model.historical.HistoricalData;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for market data ingestion operations.
 * Decouples Kafka layer from the implementation service layer.
 */
public interface MarketDataIngestionService {

    /**
     * Save OHLC data asynchronously.
     *
     * @param data Map of symbol to OHLCQuote
     * @return CompletableFuture<Void> indicating completion
     */
    CompletableFuture<Void> saveOHLCData(Map<String, OHLCQuote> data);

    /**
     * Save Historical data asynchronously.
     *
     * @param symbol    The trading symbol
     * @param timeFrame The time frame of the data
     * @param data      The historical data object
     * @return CompletableFuture<Void> indicating completion
     */
    CompletableFuture<Void> saveHistoricalData(String symbol, TimeFrame timeFrame, HistoricalData data);
}
