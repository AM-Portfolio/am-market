package com.marketdata.common;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.am.common.investment.model.historical.HistoricalData;
import com.zerodhatech.models.Instrument;
import com.zerodhatech.models.LTPQuote;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;

/**
 * Common interface for market data providers (Zerodha, Upstox, etc.)
 * This abstraction allows switching between different providers through
 * configuration
 */
public interface MarketDataProvider {

    /**
     * Initialize the provider
     */
    void initialize();

    /**
     * Clean up resources
     */
    void cleanup();

    /**
     * Set access token for authentication
     * 
     * @param accessToken Access token
     */
    void setAccessToken(String accessToken);

    /**
     * Get login URL for authentication
     * 
     * @return Login URL
     */
    String getLoginUrl();

    /**
     * Generate session from request token
     * 
     * @param requestToken Request token
     * @return Session information
     */
    Object generateSession(String requestToken);

    /**
     * Get quotes for symbols
     * 
     * @param symbols Array of symbols
     * @return Map of symbol to quote data
     */
    Map<String, Object> getQuotes(String[] symbols);

    /**
     * Get OHLC data for symbols
     * 
     * @param symbols   Array of symbols
     * @param timeFrame TimeFrame for the data
     * @return Map of symbol to OHLC data
     */
    Map<String, OHLCQuote> getOHLC(List<String> symbols, TimeFrame timeFrame);

    /**
     * Get last traded price for symbols
     * 
     * @param symbols Array of symbols
     * @return Map of symbol to LTP data
     */
    Map<String, LTPQuote> getLTP(String[] symbols);

    /**
     * Get historical data for a symbol
     * 
     * @param symbol           Symbol identifier
     * @param from             From date
     * @param to               To date
     * @param interval         Interval (minute, day, etc.)
     * @param continuous       Continuous flag
     * @param additionalParams Additional provider-specific parameters
     * @return Historical data
     */
    HistoricalData getHistoricalData(String symbol, Date from, Date to, TimeFrame interval,
            boolean continuous, Map<String, Object> additionalParams);

    /**
     * Initialize ticker for real-time data
     * 
     * @param symbolIds    List of symbol identifiers
     * @param tickListener Callback for tick data
     * @return Ticker instance
     */
    Object initializeTicker(List<String> symbolIds, Object tickListener);

    /**
     * Check if ticker is connected
     * 
     * @return true if connected
     */
    boolean isTickerConnected();

    /**
     * Get all available symbols
     * 
     * @return List of symbols
     */
    List<Instrument> getAllInstruments();

    /**
     * Get symbols for a specific exchange
     * 
     * @param exchange Exchange name
     * @return List of symbols
     */
    List<Object> getSymbolsForExchange(String exchange);

    /**
     * Execute operation asynchronously
     * 
     * @param operation Operation to execute
     * @param <T>       Return type
     * @return CompletableFuture with result
     */
    <T> CompletableFuture<T> executeAsync(ProviderOperation<T> operation);

    /**
     * Logout and invalidate session
     * 
     * @return true if successful
     */
    boolean logout();

    /**
     * Get provider name
     * 
     * @return Provider name (e.g., "zerodha", "upstox")
     */
    String getProviderName();

    /**
     * Functional interface for provider operations
     * 
     * @param <T> Return type
     */
    @FunctionalInterface
    interface ProviderOperation<T> {
        T execute() throws Exception;
    }
}
