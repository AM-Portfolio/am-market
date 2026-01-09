package com.am.marketdata.service.util;

import com.am.marketdata.service.MarketDataPersistenceService;
import com.marketdata.common.MarketDataProvider;
import com.am.marketdata.provider.common.MarketDataProviderFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;

import com.am.marketdata.common.model.TimeFrame;

import java.util.*;

/**
 * Abstract base class for market data retrieval using the Template Method
 * pattern.
 * Provides a structured approach to retrieving market data from various sources
 * (cache, database, provider) in a configurable order.
 *
 * @param <K> The key type (usually String for symbol)
 * @param <T> The data type to retrieve (e.g., OHLCQuote, HistoricalData)
 */
@Slf4j
public abstract class AbstractMarketDataRetriever<K, T> {

    protected final MarketDataPersistenceService persistenceService;
    protected final MarketDataProviderFactory providerFactory;

    @Getter
    protected final List<DataSourceType> retrievalOrder;

    @Getter
    protected final boolean cacheResults;

    @Getter
    protected String targetProviderName;

    /**
     * Constructor for AbstractMarketDataRetriever
     *
     * @param persistenceService The persistence service for database and cache
     *                           operations
     * @param providerFactory    The factory for creating market data providers
     * @param retrievalOrder     The order in which to try different data sources
     * @param cacheResults       Whether to cache results from provider
     */
    protected AbstractMarketDataRetriever(
            MarketDataPersistenceService persistenceService,
            MarketDataProviderFactory providerFactory,
            List<DataSourceType> retrievalOrder,
            boolean cacheResults) {
        this(persistenceService, providerFactory, retrievalOrder, cacheResults, null);
    }

    /**
     * Constructor for AbstractMarketDataRetriever with target provider
     *
     * @param persistenceService The persistence service for database and cache
     *                           operations
     * @param providerFactory    The factory for creating market data providers
     * @param retrievalOrder     The order in which to try different data sources
     * @param cacheResults       Whether to cache results from provider
     * @param targetProviderName The specific provider to use (if null, uses
     *                           default/active)
     */
    protected AbstractMarketDataRetriever(
            MarketDataPersistenceService persistenceService,
            MarketDataProviderFactory providerFactory,
            List<DataSourceType> retrievalOrder,
            boolean cacheResults,
            String targetProviderName) {
        this.persistenceService = persistenceService;
        this.providerFactory = providerFactory;
        this.retrievalOrder = retrievalOrder != null && !retrievalOrder.isEmpty()
                ? retrievalOrder
                : Arrays.asList(DataSourceType.CACHE, DataSourceType.DATABASE, DataSourceType.PROVIDER);
        this.cacheResults = cacheResults;
        this.targetProviderName = targetProviderName;
    }

    /**
     * Template method for retrieving data following the configured retrieval order.
     * This method orchestrates the retrieval process by trying different data
     * sources
     * in the specified order until all requested data is found or all sources are
     * exhausted.
     *
     * @param keys         The keys (e.g., symbols) to retrieve data for
     * @param timeFrame    The time frame for the OHLC data
     * @param forceRefresh Whether to force a refresh from the provider
     * @return Map of keys to retrieved data
     */
    public Map<K, T> retrieveData(List<K> keys, TimeFrame timeFrame, boolean forceRefresh) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        log.debug("Starting data retrieval for {} keys with forceRefresh={}", keys.size(), forceRefresh);

        // If force refresh is requested, bypass DB and go directly to provider
        if (forceRefresh) {
            log.info("[FORCE_REFRESH] Skipping database check, fetching directly from provider");
            Map<K, T> result = retrieveFromProviderOnly(keys);
            return result != null ? result : Collections.emptyMap();
        }

        // Track which keys still need to be retrieved
        Set<K> remainingKeys = new HashSet<>(keys);
        Map<K, T> result = new HashMap<>();

        // Try each data source in the configured order
        for (DataSourceType sourceType : retrievalOrder) {
            if (remainingKeys.isEmpty()) {
                break;
            }

            Map<K, T> sourceData;
            switch (sourceType) {
                case CACHE:
                    sourceData = retrieveFromCache(keys, remainingKeys, timeFrame);
                    break;
                case DATABASE:
                    sourceData = retrieveFromDatabase(remainingKeys, timeFrame);
                    break;
                case PROVIDER:
                    // Provider doesn't support timeFrame, so we don't pass it
                    List<K> remainingKeysList = new ArrayList<>(remainingKeys);
                    sourceData = retrieveFromProviderWithSave(remainingKeysList);
                    break;
                default:
                    log.warn("Unknown data source type: {}", sourceType);
                    continue;
            }

            if (sourceData != null && !sourceData.isEmpty()) {
                result.putAll(sourceData);
            }
        }

        log.debug("Completed data retrieval, found data for {}/{} keys",
                result.size(), keys.size());

        return result;
    }

    /**
     * Retrieve data directly from the provider, bypassing cache and database
     *
     * @param keys The keys to retrieve data for
     * @return Map of keys to retrieved data
     */
    protected Map<K, T> retrieveFromProviderOnly(List<K> keys) {
        log.info("[FORCE_REFRESH] Bypassing cache and database, going directly to provider. Target: {}",
                targetProviderName);

        MarketDataProvider provider = providerFactory.getProvider(targetProviderName);
        Map<K, T> providerData = retrieveFromProvider(provider, keys);

        // Save to cache if configured to do so
        if (cacheResults && providerData != null && !providerData.isEmpty()) {
            saveDataAsync(providerData);
        }

        return providerData != null ? providerData : Collections.emptyMap();
    }

    /**
     * Retrieve data from provider and save to persistence if configured
     *
     * @param keys The keys to retrieve data for
     * @return Map of keys to retrieved data
     */
    private Map<K, T> retrieveFromProviderWithSave(List<K> keys) {
        MarketDataProvider provider = providerFactory.getProvider(targetProviderName);
        Map<K, T> providerData = retrieveFromProvider(provider, keys);

        // Save to cache if configured to do so
        if (cacheResults && providerData != null && !providerData.isEmpty()) {
            saveDataAsync(providerData);
        }

        return providerData;
    }

    /**
     * Retrieves data from the provider for the given keys
     *
     * @param provider The market data provider
     * @param keys     The keys to retrieve data for
     * @return Map of key to data
     */
    protected abstract Map<K, T> retrieveFromProvider(MarketDataProvider provider, List<K> keys);

    /**
     * Retrieves data directly from the provider for all keys
     * 
     * @param keys The keys to retrieve data for
     * @return Map of keys to retrieved data
     */
    protected Map<K, T> retrieveFromProvider(List<K> keys) {
        log.info("Retrieving data directly from provider for {} keys (Target: {})", keys.size(), targetProviderName);
        MarketDataProvider provider = providerFactory.getProvider(targetProviderName);
        return retrieveFromProvider(provider, keys);
    }

    /**
     * Retrieves data from the database first, then from the provider for any
     * missing keys
     * Used when forceRefresh is true to ensure we have the most up-to-date data
     * but still leverage existing database records
     * 
     * @param keys      The keys to retrieve data for
     * @param timeFrame The time frame for the OHLC data
     * @return Map of keys to retrieved data
     */
    protected Map<K, T> retrieveFromDatabaseAndProvider(List<K> keys, TimeFrame timeFrame) {
        log.info("Force refresh requested: checking database first, then provider for {} keys with timeFrame {}",
                keys.size(), timeFrame.getApiValue());

        // First check database with timeFrame
        Map<K, T> result = retrieveFromDatabase(keys, timeFrame);
        log.info("Found {} keys in database for timeFrame {}", result.size(), timeFrame.getApiValue());

        // Update cache with database results if configured to do so and we have data
        if (cacheResults && !result.isEmpty()) {
            log.info("Updating cache with {} keys from database for timeFrame {}", result.size(),
                    timeFrame.getApiValue());
            updateCacheOnly(result);
        }

        // Find keys not in database
        Set<K> remainingKeys = new HashSet<>(keys);
        remainingKeys.removeAll(result.keySet());

        if (!remainingKeys.isEmpty()) {
            log.info("Retrieving {} remaining keys from provider (timeFrame not supported by provider)",
                    remainingKeys.size());
            Map<K, T> providerData = retrieveFromProvider(new ArrayList<K>(remainingKeys));

            // Cache provider data if configured to do so
            if (cacheResults && !providerData.isEmpty()) {
                log.info("Saving {} keys from provider to database and cache for timeFrame {}", providerData.size(),
                        timeFrame.getApiValue());
                saveDataAsync(providerData);
            }

            // Add provider data to result
            result.putAll(providerData);
        }

        return result;
    }

    /**
     * Retrieve data from cache
     *
     * @param allKeys       All keys being requested
     * @param remainingKeys Set of keys that still need to be retrieved (will be
     *                      modified)
     * @param timeFrame     The time frame for the OHLC data
     * @return Map of key to data
     */
    protected abstract Map<K, T> retrieveFromCache(List<K> allKeys, Set<K> remainingKeys, TimeFrame timeFrame);

    /**
     * Retrieve data from database
     *
     * @param remainingKeys Set of keys that still need to be retrieved
     * @param timeFrame     The time frame for the OHLC data
     * @return Map of key to data
     */
    protected abstract Map<K, T> retrieveFromDatabase(Set<K> remainingKeys, TimeFrame timeFrame);

    /**
     * Retrieve data from database for a list of keys
     *
     * @param keys      List of keys to retrieve
     * @param timeFrame The time frame for the OHLC data
     * @return Map of key to data
     */
    protected Map<K, T> retrieveFromDatabase(List<K> keys, TimeFrame timeFrame) {
        return retrieveFromDatabase(new HashSet<>(keys), timeFrame);
    }

    /**
     * Save data to persistence asynchronously (both database and cache)
     *
     * @param data The data to save
     */
    protected abstract void saveDataAsync(Map<K, T> data);

    /**
     * Update only the cache with the provided data, without saving to database
     * This is useful when we already have data from the database and just want to
     * refresh the cache
     *
     * @param data The data to update in the cache
     */
    protected abstract void updateCacheOnly(Map<K, T> data);

    /**
     * Abstract builder for market data retrievers
     *
     * @param <K> The key type
     * @param <T> The data type
     * @param <B> The builder type
     * @param <R> The retriever type
     */
    public abstract static class AbstractBuilder<K, T, B extends AbstractBuilder<K, T, B, R>, R extends AbstractMarketDataRetriever<K, T>> {
        protected MarketDataPersistenceService persistenceService;
        protected MarketDataProviderFactory providerFactory;
        protected RetryTemplate retryTemplate;
        protected List<DataSourceType> retrievalOrder = Arrays.asList(
                DataSourceType.CACHE, DataSourceType.DATABASE, DataSourceType.PROVIDER);
        protected Boolean cacheResults = true;
        protected String targetProviderName;

        @SuppressWarnings("unchecked")
        public B persistenceService(MarketDataPersistenceService persistenceService) {
            this.persistenceService = persistenceService;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B providerFactory(MarketDataProviderFactory providerFactory) {
            this.providerFactory = providerFactory;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B retryTemplate(RetryTemplate retryTemplate) {
            this.retryTemplate = retryTemplate;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B retrievalOrder(List<DataSourceType> retrievalOrder) {
            this.retrievalOrder = retrievalOrder;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B cacheResults(boolean cacheResults) {
            this.cacheResults = cacheResults;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B targetProviderName(String targetProviderName) {
            this.targetProviderName = targetProviderName;
            return (B) this;
        }

        public abstract R build();
    }
}
