package com.am.marketdata.service.util;

import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.marketdata.common.MarketDataProvider;

import com.am.marketdata.service.MarketDataPersistenceService;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for retrieving market data from different sources
 */
@Slf4j
@Service
public class MarketDataRetrievalUtil {

    // Static constants for retry parameters
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final int DEFAULT_RETRY_DELAY_MS = 1000;

    @Value("${market.data.max.retries:3}")
    private int maxRetries;

    @Value("${market.data.retry.delay.ms:1000}")
    private int retryDelayMs;

    @Value("${market.data.max.age.minutes:15}")
    private int maxAgeMinutes;

    /**
     * Retrieve OHLC data from cache
     *
     * @param persistenceService The persistence service to use
     * @param tradingSymbols     List of trading symbols
     * @param remainingSymbols   Set of symbols that still need to be retrieved
     *                           (will be modified)
     * @return Map of symbol to OHLC quote
     */
    public Map<String, OHLCQuote> retrieveFromCache(
            MarketDataPersistenceService persistenceService,
            List<String> tradingSymbols,
            Set<String> remainingSymbols, TimeFrame timeFrame) {

        log.info("[DATA_SOURCE] Attempting to fetch OHLC data from cache for {} symbols",
                remainingSymbols.size());

        Map<String, OHLCQuote> cachedData = persistenceService.getOHLCData(tradingSymbols, timeFrame, false);

        if (cachedData != null && !cachedData.isEmpty()) {
            log.info("[DATA_SOURCE] Found {} OHLC quotes in cache", cachedData.size());

            // Remove found symbols from the remaining set
            cachedData.keySet().forEach(symbol -> remainingSymbols.remove(symbol.replace("NSE:", "")));

            log.info("[DATA_SOURCE] {} symbols remaining after cache lookup", remainingSymbols.size());
        } else {
            log.info("[DATA_SOURCE] No OHLC data found in cache");
        }

        return cachedData != null ? cachedData : Collections.emptyMap();
    }

    /**
     * Retrieve OHLC data from database
     *
     * @param persistenceService The persistence service to use
     * @param remainingSymbols   Set of symbols that still need to be retrieved
     *                           (will be modified)
     * @return Map of symbol to OHLC quote
     */
    public Map<String, OHLCQuote> retrieveFromDatabase(
            MarketDataPersistenceService persistenceService,
            Set<String> remainingSymbols,
            TimeFrame timeFrame) {

        if (remainingSymbols.isEmpty()) {
            return Collections.emptyMap();
        }

        log.info("[DATA_SOURCE] Attempting to fetch OHLC data from database for {} symbols",
                remainingSymbols.size());

        List<String> remainingSymbolsList = new ArrayList<>(remainingSymbols);

        // Force refresh is true here because we want to bypass cache and go directly to
        // database
        Map<String, OHLCQuote> dbData = persistenceService.getOHLCData(remainingSymbolsList, timeFrame, true);

        if (dbData != null && !dbData.isEmpty()) {
            log.info("[DATA_SOURCE] Found {} OHLC quotes in database", dbData.size());

            // Remove found symbols from the remaining set
            dbData.keySet().forEach(symbol -> remainingSymbols.remove(symbol.replace("NSE:", "")));

            log.info("[DATA_SOURCE] {} symbols remaining after database lookup", remainingSymbols.size());
        } else {
            log.info("[DATA_SOURCE] No OHLC data found in database");
        }

        return dbData != null ? dbData : Collections.emptyMap();
    }

    /**
     * Retrieve OHLC data from provider
     *
     * @param provider  The market data provider
     * @param symbols   List of symbols to retrieve
     * @param timeFrame TimeFrame for the data
     * @return Map of symbol to OHLC quote
     */
    @SneakyThrows
    public Map<String, OHLCQuote> retrieveFromProvider(
            MarketDataProvider provider,
            List<String> symbols,
            TimeFrame timeFrame) {

        if (symbols.isEmpty()) {
            return Collections.emptyMap();
        }

        log.info("[DATA_SOURCE] Fetching OHLC data from provider for {} symbols", symbols.size());

        Map<String, OHLCQuote> providerData = retryOnFailure(() -> provider.getOHLC(symbols, timeFrame), "getOHLC");

        if (providerData != null && !providerData.isEmpty()) {
            log.info("[DATA_SOURCE] Successfully fetched {} OHLC quotes from provider", providerData.size());
        } else {
            log.warn("[DATA_SOURCE] No OHLC data returned from provider");
        }

        return providerData != null ? providerData : Collections.emptyMap();
    }

    /**
     * Save OHLC data to persistence asynchronously
     *
     * @param persistenceService The persistence service to use
     * @param data               The data to save
     */
    public void saveDataAsync(MarketDataPersistenceService persistenceService, Map<String, OHLCQuote> data) {
        if (data == null || data.isEmpty()) {
            return;
        }

        try {
            persistenceService.saveOHLCData(data);
            log.debug("Initiated async save of {} OHLC quotes", data.size());
        } catch (Exception e) {
            log.error("Error initiating async save of OHLC data: {}", e.getMessage(), e);
        }
    }

    /**
     * Validate trading symbols
     *
     * @param tradingSymbols List of trading symbols
     * @throws IllegalArgumentException if the symbols are invalid
     */
    public void validateSymbols(List<String> tradingSymbols) {
        if (tradingSymbols == null) {
            throw new IllegalArgumentException("Trading symbols cannot be null");
        }
    }

    /**
     * Execute a callable with retry logic using default retry parameters
     *
     * @param callable      The callable to execute
     * @param operationName The name of the operation (for logging)
     * @param <T>           The return type
     * @return The result of the callable
     * @throws Exception If all retries fail
     */
    public <T> T retryOnFailure(Callable<T> callable, String operationName) throws Exception {
        return retryOnFailure(callable, operationName, DEFAULT_MAX_RETRIES, DEFAULT_RETRY_DELAY_MS);
    }

    /**
     * Execute a callable with retry logic using specified retry parameters
     *
     * @param callable      The callable to execute
     * @param operationName The name of the operation (for logging)
     * @param maxRetries    Maximum number of retry attempts
     * @param retryDelayMs  Base delay in milliseconds between retries
     * @param <T>           The return type
     * @return The result of the callable
     * @throws Exception If all retries fail
     */
    public <T> T retryOnFailure(Callable<T> callable, String operationName, int maxRetries, int retryDelayMs)
            throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                T result = callable.call();
                if (attempt > 1) {
                    log.info("Operation {} succeeded after {} attempts", operationName, attempt);
                }
                return result;
            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {} for operation {} failed: {}", attempt, operationName, e.getMessage());

                if (attempt < maxRetries) {
                    try {
                        // Exponential backoff
                        long delay = retryDelayMs * (long) Math.pow(2, attempt - 1);
                        log.debug("Waiting {}ms before retry attempt {}", delay, attempt + 1);
                        TimeUnit.MILLISECONDS.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                }
            }
        }

        log.error("Operation {} failed after {} attempts", operationName, maxRetries);
        throw new RuntimeException("Operation failed after " + maxRetries + " attempts", lastException);
    }
}
