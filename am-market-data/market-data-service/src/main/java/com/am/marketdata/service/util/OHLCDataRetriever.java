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

        // Call the cache service DIRECTLY (Redis-only).
        // IMPORTANT: Do NOT use persistenceService.getOHLCData(false) here.
        // That method first reads Redis, then queries InfluxDB for any remaining symbols —
        // and if the InfluxDB query times out (~23s), it catches the exception and returns
        // emptyMap(), losing all the Redis results we already had. The DATABASE step below
        // already handles the InfluxDB lookup for whatever Redis misses.
        Map<String, OHLCQuote> cachedData = persistenceService
                .getMarketDataCacheService()
                .getOHLCFromCache(allSymbols, timeFrame);

        if (cachedData != null && !cachedData.isEmpty()) {
            log.info("[CACHE] Found {} OHLC quotes in cache for timeFrame {}", cachedData.size(), tfValue);

            // Build a set of clean cached symbol names (stripped of any exchange prefix and trimmed).
            // The cache returns clean names like "MARUTI" but remainingSymbols may contain symbols
            // with leading spaces (" MARUTI") from comma-separated parsing, or with exchange prefixes
            // ("NSE_EQ:MARUTI"). A direct Set.remove("MARUTI") would silently fail for " MARUTI",
            // leaving all symbols "remaining" and causing the provider to be called for 100 symbols
            // instead of just the 4 that are truly missing from Redis.
            Set<String> cleanCacheHits = cachedData.keySet().stream()
                    .map(s -> s.replace("NSE_EQ:", "").replace("NSE:", "").trim().toUpperCase())
                    .collect(java.util.stream.Collectors.toSet());

            // Remove the ORIGINAL-format keys from remainingSymbols by comparing their clean form.
            // Using an iterator to safely remove while iterating.
            java.util.Iterator<String> iter = remainingSymbols.iterator();
            while (iter.hasNext()) {
                String key = iter.next();
                String cleanKey = key.replace("NSE_EQ:", "").replace("NSE:", "").trim().toUpperCase();
                if (cleanCacheHits.contains(cleanKey)) {
                    iter.remove(); // safely removes the ORIGINAL key (e.g., " MARUTI")
                }
            }

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
            dbData.keySet().forEach(symbol -> remainingSymbols.remove(symbol.replace("NSE_EQ:", "").replace("NSE:", "")));

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
                log.info("[PROVIDER] {} Successfully fetched {} OHLC quotes from provider with timeFrame {}",
                        provider.getProviderName(), providerData.size(), tfValue);
                
                // Map the results back to the original requested symbols.
                // Upstox sometimes maps symbols internally to different trading symbols (e.g. AXISGOLD -> GOLDAXIS).
                // If we cache GOLDAXIS, the next request for AXISGOLD will miss the cache.
                Map<String, OHLCQuote> mappedData = new HashMap<>();
                for (String reqSymbol : symbols) {
                    String cleanReq = reqSymbol.replace("NSE_EQ:", "").replace("NSE:", "").trim().toUpperCase();
                    
                    // Look for matches in the provider keys
                    boolean matched = false;
                    for (Map.Entry<String, OHLCQuote> entry : providerData.entrySet()) {
                        String cleanProv = entry.getKey().replace("NSE_EQ:", "").replace("NSE:", "").trim().toUpperCase();
                        
                        // Handle known mappings:
                        // 1. Exact match (e.g. RELIANCE == RELIANCE)
                        // 2. Contains mapping (e.g. AXISGOLD matches GOLDAXIS, AXISNIFTY matches NIFTYAXIS, SEQUENT matches VIYASH due to name change)
                        if (cleanReq.equals(cleanProv) || 
                            (cleanReq.equals("AXISGOLD") && cleanProv.equals("GOLDAXIS")) ||
                            (cleanReq.equals("AXISNIFTY") && cleanProv.equals("NIFTYAXIS")) ||
                            (cleanReq.equals("SEQUENT") && cleanProv.equals("VIYASH"))) {
                            
                            mappedData.put(reqSymbol, entry.getValue());
                            matched = true;
                            log.info("[PROVIDER_MAP] Mapped provider symbol {} back to requested symbol {}", entry.getKey(), reqSymbol);
                            break;
                        }
                    }
                    if (!matched) {
                        // Fallback: if provider returned it under the original name directly
                        if (providerData.containsKey(reqSymbol)) {
                            mappedData.put(reqSymbol, providerData.get(reqSymbol));
                        }
                    }
                }
                
                // Also carry forward anything else that was returned but didn't match the mapping loop
                for (Map.Entry<String, OHLCQuote> entry : providerData.entrySet()) {
                    if (!mappedData.containsKey(entry.getKey())) {
                        mappedData.put(entry.getKey(), entry.getValue());
                    }
                }
                
                return mappedData;
            } else {
                log.info("[PROVIDER] {} No OHLC data returned from provider for timeFrame {}",
                        provider.getProviderName(), tfValue);
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

            if (producer != null) {
                // Use producer to send to Kafka (Fire and Forget)
                producer.sendOHLCData(data, this.timeFrame, targetProviderName);
                log.info(methodName, "[PROVIDER_ASYNC] Successfully sent ingestion event to Kafka");
            } else {
                log.warn(methodName, "[PROVIDER_ASYNC] Skipping Kafka ingestion (Kafka disabled/Producer null)");
            }
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

            // Producer is optional
            // if (producer == null) {
            // // If producer not set, throw or log?
            // // Currently we enforce it as it's critical for async flow
            // throw new IllegalStateException("MarketDataProducer must be provided");
            // }

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
