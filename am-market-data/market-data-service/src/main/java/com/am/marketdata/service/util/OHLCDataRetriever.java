package com.am.marketdata.service.util;

import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.service.MarketDataPersistenceService;
import com.marketdata.common.MarketDataProvider;
import com.am.marketdata.provider.common.MarketDataProviderFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Concrete implementation of AbstractMarketDataRetriever for OHLC data.
 * Handles retrieval of OHLC quotes from cache, database, and provider.
 */
@Slf4j
public class OHLCDataRetriever extends AbstractMarketDataRetriever<String, OHLCQuote> {

    @Getter
    @Setter
    private TimeFrame timeFrame = TimeFrame.DAY; // Default to 5-minute timeframe

    private final com.am.marketdata.service.kafka.producer.MarketDataProducer producer;

    private OHLCDataRetriever(
            MarketDataPersistenceService persistenceService,
            MarketDataProviderFactory providerFactory,
            List<DataSourceType> retrievalOrder,
            boolean cacheResults,
            String targetProviderName,
            com.am.marketdata.service.kafka.producer.MarketDataProducer producer) {
        super(persistenceService, providerFactory, retrievalOrder, cacheResults, targetProviderName);
        this.producer = producer;
    }

    /**
     * Retrieve OHLC data from cache
     *
     * @param allSymbols       All symbols being requested
     * @param remainingSymbols Set of symbols that still need to be retrieved (will
     *                         be modified)
     * @param timeFrame        The time frame for the OHLC data
     * @return Map of symbol to OHLC quote
     */
    @Override
    protected Map<String, OHLCQuote> retrieveFromCache(List<String> allSymbols, Set<String> remainingSymbols,
            TimeFrame timeFrame) {
        String tfValue = timeFrame != null ? timeFrame.getApiValue() : "1D";
        log.info("[CACHE] Attempting to fetch OHLC data from cache for {} symbols with timeFrame {}",
                remainingSymbols.size(), tfValue);

        // Pass timeFrame to persistence service if it supports it
        Map<String, OHLCQuote> cachedData = persistenceService.getOHLCData(allSymbols, timeFrame, false);

        if (cachedData != null && !cachedData.isEmpty()) {
            log.info("[CACHE] Found {} OHLC quotes in cache for timeFrame {}", cachedData.size(), tfValue);

            // Remove found symbols from the remaining set
            cachedData.keySet().forEach(symbol -> remainingSymbols.remove(symbol.replace("NSE:", "")));

            log.info("[CACHE] {} symbols remaining after cache lookup for timeFrame {}", remainingSymbols.size(),
                    tfValue);
        } else {
            log.info("[CACHE] No OHLC data found in cache for timeFrame {}", tfValue);
        }

        return cachedData != null ? cachedData : Collections.emptyMap();
    }

    /**
     * Retrieve OHLC data from database
     *
     * @param remainingSymbols Set of symbols that still need to be retrieved (will
     *                         be modified)
     * @param timeFrame        The time frame for the OHLC data
     * @return Map of symbol to OHLC quote
     */
    @Override
    protected Map<String, OHLCQuote> retrieveFromDatabase(Set<String> remainingSymbols, TimeFrame timeFrame) {
        if (remainingSymbols.isEmpty()) {
            return Collections.emptyMap();
        }

        String tfValue = timeFrame != null ? timeFrame.getApiValue() : "1D";
        log.info("[DATABASE] Attempting to fetch OHLC data from database for {} symbols with timeFrame {}",
                remainingSymbols.size(), tfValue);

        List<String> remainingSymbolsList = new ArrayList<>(remainingSymbols);

        // Force refresh is true here because we want to bypass cache and go directly to
        // database
        Map<String, OHLCQuote> dbData = persistenceService.getOHLCData(remainingSymbolsList, timeFrame, true);

        if (dbData != null && !dbData.isEmpty()) {
            log.info("[DATABASE] Found {} OHLC quotes in database for timeFrame {}", dbData.size(), tfValue);

            // Remove found symbols from the remaining set
            dbData.keySet().forEach(symbol -> remainingSymbols.remove(symbol.replace("NSE:", "")));

            log.info("[DATABASE] {} symbols remaining after database lookup for timeFrame {}", remainingSymbols.size(),
                    tfValue);
        } else {
            log.info("[DATABASE] No OHLC data found in database for timeFrame {}", tfValue);
        }

        return dbData != null ? dbData : Collections.emptyMap();
    }

    /**
     * Retrieve OHLC data from provider
     *
     * @param provider The market data provider
     * @param symbols  List of symbols to retrieve
     * @return Map of symbol to OHLC quote
     */
    @Override
    protected Map<String, OHLCQuote> retrieveFromProvider(MarketDataProvider provider, List<String> symbols) {
        if (symbols.isEmpty()) {
            return Collections.emptyMap();
        }

        String tfValue = timeFrame != null ? timeFrame.getApiValue() : "1D";
        log.info(provider.getProviderName() + " Fetching OHLC data from provider for {} symbols with timeFrame {}",
                symbols.size(), tfValue);

        try {
            // Pass the timeFrame to the provider
            Map<String, OHLCQuote> providerData = provider.getOHLC(symbols, timeFrame);

            if (providerData != null && !providerData.isEmpty()) {
                log.info(
                        provider.getProviderName()
                                + " Successfully fetched {} OHLC quotes from provider with timeFrame {}",
                        providerData.size(), tfValue);
            } else {
                log.info(provider.getProviderName() + " No OHLC data returned from provider for timeFrame {}",
                        tfValue);
            }

            return providerData != null ? providerData : Collections.emptyMap();
        } catch (Exception e) {
            log.error(provider.getProviderName() + " Error fetching OHLC data for timeFrame {}: {}",
                    tfValue, e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Save OHLC data to persistence asynchronously (both database and cache)
     *
     * @param data The data to save
     */
    @Override
    protected void saveDataAsync(Map<String, OHLCQuote> data) {
        if (data == null || data.isEmpty()) {
            log.warn("saveDataAsync", "No OHLC data to save (empty map)");
            return;
        }

        String methodName = "saveDataAsync";
        try {
            log.info(methodName, "[PROVIDER_ASYNC] Sending {} OHLC quotes to KAFKA for ingestion", data.size());

            // Use producer to send to Kafka (Fire and Forget)
            producer.sendOHLCData(data, this.timeFrame, targetProviderName);

            log.info(methodName, "[PROVIDER_ASYNC] Successfully sent ingestion event to Kafka");
        } catch (Exception e) {
            log.error(methodName, "[PROVIDER_ASYNC] FAILED to send OHLC data to KAFKA: " + e.getMessage(), e);
        }
    }

    /**
     * Update only the cache with the provided OHLC data, without saving to database
     *
     * @param data The data to update in the cache
     */
    @Override
    protected void updateCacheOnly(Map<String, OHLCQuote> data) {
        if (data == null || data.isEmpty()) {
            return;
        }

        try {
            // Use the MarketDataCacheService directly to update only the cache
            // Use this retriever's timeFrame setting
            persistenceService.getMarketDataCacheService().cacheOHLCData(data, this.timeFrame);
            log.debug("Updated cache with {} OHLC quotes for timeFrame {}", data.size(),
                    this.timeFrame != null ? this.timeFrame.getApiValue() : "default");
        } catch (Exception e) {
            log.error("Error updating cache with OHLC data: {}", e.getMessage(), e);
        }
    }

    /**
     * Builder for OHLCDataRetriever
     */
    // ... (rest of class)

    /**
     * Builder for OHLCDataRetriever
     */
    public static class Builder extends AbstractBuilder<String, OHLCQuote, Builder, OHLCDataRetriever> {
        private com.am.marketdata.service.kafka.producer.MarketDataProducer producer;

        public Builder producer(com.am.marketdata.service.kafka.producer.MarketDataProducer producer) {
            this.producer = producer;
            return this;
        }

        @Override
        public OHLCDataRetriever build() {
            if (persistenceService == null) {
                throw new IllegalStateException("PersistenceService must be provided");
            }
            if (providerFactory == null) {
                throw new IllegalStateException("ProviderFactory must be provided");
            }

            if (producer == null) {
                // If producer not set, throw or log?
                // Currently we enforce it as it's critical for async flow
                throw new IllegalStateException("MarketDataProducer must be provided");
            }

            return new OHLCDataRetriever(
                    persistenceService,
                    providerFactory,
                    retrievalOrder,
                    cacheResults != null ? cacheResults : true,
                    targetProviderName,
                    producer);
        }
    }

    /**
     * Create a new builder for OHLCDataRetriever
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}
