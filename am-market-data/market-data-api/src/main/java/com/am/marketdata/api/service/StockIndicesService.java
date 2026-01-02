package com.am.marketdata.api.service;

import com.am.common.investment.model.stockindice.StockIndicesMarketData;
import com.am.common.investment.service.StockIndicesMarketDataService;
import com.am.marketdata.scraper.service.MarketDataProcessingService;
import lombok.RequiredArgsConstructor;

import com.am.marketdata.common.log.AppLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class StockIndicesService {

    // Auto-detects class name "StockIndicesService"
    private final AppLogger log = AppLogger.getLogger();

    private final MarketDataProcessingService marketDataProcessingService;
    private final StockIndicesMarketDataService stockIndicesMarketDataService;
    private final MarketDataFetchService marketDataCacheService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @Value("${market.data.cache.enabled:true}")
    private boolean cacheEnabled;

    public List<StockIndicesMarketData> getLatestIndicesData(List<String> indexSymbols) {
        return getLatestIndicesData(indexSymbols, false);
    }

    public List<StockIndicesMarketData> getLatestIndicesData(List<String> indexSymbols, boolean forceRefresh) {
        String methodName = "getLatestIndicesData";
        try {
            // 1. Try to get from cache first
            List<StockIndicesMarketData> cachedResult = checkCache(indexSymbols, forceRefresh, methodName);
            if (!cachedResult.isEmpty()) {
                return cachedResult;
            }

            // 2. If cache miss, check database for existing data and identify what's
            // missing
            List<StockIndicesMarketData> finalResults = new ArrayList<>();
            List<String> symbolsToProcess = new ArrayList<>();
            checkDatabase(indexSymbols, forceRefresh, finalResults, symbolsToProcess, methodName);

            // 4. Log summary
            if (finalResults.isEmpty()) {
                // 3. Fetch fresh data for missing symbols from API/Scraper
                if (!symbolsToProcess.isEmpty()) {
                    fetchFreshData(symbolsToProcess, finalResults, methodName);
                }
            } else {
                log.info(methodName, String.format("Retrieved data for %d/%d symbols (fresh/db)", finalResults.size(),
                        indexSymbols.size()));
            }

            return finalResults;

        } catch (Exception e) {
            log.error(methodName, "Error processing stock indices request", e);
            return new ArrayList<>();
        }
    }

    private List<StockIndicesMarketData> checkCache(List<String> indexSymbols, boolean forceRefresh,
            String methodName) {
        if (cacheEnabled && !forceRefresh) {
            Set<StockIndicesMarketData> cachedData = marketDataCacheService
                    .getStockIndicesData(new HashSet<>(indexSymbols), false);
            if (cachedData != null && !cachedData.isEmpty()) {
                log.info(methodName,
                        String.format("Retrieved %d indices from cache (cached=%s)", cachedData.size(), true));
                return new ArrayList<>(cachedData);
            } else {
                log.info(methodName, "Retrieved 0 indices from cache, falling back to db/fresh fetch");
            }
        }
        return new ArrayList<>();
    }

    private void checkDatabase(List<String> indexSymbols, boolean forceRefresh,
            List<StockIndicesMarketData> finalResults, List<String> symbolsToProcess, String methodName) {
        if (forceRefresh) {
            symbolsToProcess.addAll(indexSymbols);
        } else {
            try {
                // Batch retrieval
                List<StockIndicesMarketData> docs = stockIndicesMarketDataService
                        .findByIndexSymbols(indexSymbols.stream().collect(Collectors.toSet()));
                Set<String> foundSymbols = new HashSet<>();

                // FIX: Add found documents to finalResults
                docs.forEach(doc -> {
                    if (doc != null && doc.getIndexSymbol() != null) {
                        finalResults.add(doc); // Add to results
                        foundSymbols.add(doc.getIndexSymbol()); // Track found
                        log.info(methodName, "Found data for " + doc.getIndexSymbol() + " in database");
                    }
                });

                // Identify missing symbols
                for (String symbol : indexSymbols) {
                    if (!foundSymbols.contains(symbol)) {
                        log.info(methodName, "Symbol " + symbol + " not found in database. Queuing for fresh fetch.");
                        symbolsToProcess.add(symbol);
                    }
                }

            } catch (Exception e) {
                log.error(methodName, "Error reading from database", e);
                // On DB error, treat all as missing
                symbolsToProcess.addAll(indexSymbols);
            }
        }
    }

    private void fetchFreshData(List<String> symbolsToProcess, List<StockIndicesMarketData> finalResults,
            String methodName) {
        log.info(methodName, "Fetching fresh data for " + symbolsToProcess.size() + " symbols: " + symbolsToProcess);

        // Fetch in parallel
        List<CompletableFuture<Boolean>> futures = symbolsToProcess.stream()
                .map(symbol -> marketDataProcessingService.fetchAndProcessStockIndices(symbol)
                        .exceptionally(e -> {
                            log.error(methodName, "Error fetching data for symbol: " + symbol, e);
                            return false;
                        }))
                .collect(Collectors.toList());

        // Wait for completion
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        try {
            // Add a small delay to ensure data is persisted
            TimeUnit.SECONDS.sleep(1);

            // FIX: Retrieve freshly persisted data from database
            List<StockIndicesMarketData> freshData = stockIndicesMarketDataService
                    .findByIndexSymbols(symbolsToProcess.stream().collect(Collectors.toSet()));
            finalResults.addAll(freshData);

            log.info(methodName, "Retrieved " + freshData.size() + " freshly fetched indices from database");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public StockIndicesMarketData getLatestIndexData(String indexSymbol) {
        return getLatestIndexData(indexSymbol, false);
    }

    public StockIndicesMarketData getLatestIndexData(String indexSymbol, boolean forceRefresh) {
        String methodName = "getLatestIndexData";
        try {
            // Check if we should use cache
            if (cacheEnabled && !forceRefresh) {
                StockIndicesMarketData cachedData = marketDataCacheService.getStockIndexData(indexSymbol, false);
                if (cachedData != null) {
                    log.info(methodName,
                            String.format("Retrieved index data for %s from cache (cached=%s)", indexSymbol, "true"));
                    return cachedData;
                }
            }

            // If cache miss or disabled, get fresh data
            List<StockIndicesMarketData> data = getLatestIndicesData(List.of(indexSymbol), forceRefresh);
            return data.isEmpty() ? null : data.get(0);
        } catch (Exception e) {
            log.error(methodName, "Error while fetching stock index data for symbol: " + indexSymbol, e);
            throw new RuntimeException("Failed to fetch stock index data", e);
        }
    }
}
