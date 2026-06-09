package com.am.marketdata.api.service;

import com.am.common.investment.model.stockindice.StockIndicesMarketData;
import com.am.common.investment.service.StockIndicesMarketDataService;
import com.am.marketdata.scraper.service.MarketDataProcessingService;
import com.am.marketdata.service.MarketDataCacheService;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.common.investment.model.events.StockInsidicesEventData.IndexMetadata;
import java.util.Map;
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

    private final java.util.concurrent.ConcurrentHashMap<String, Long> lastMongoSaveTimeMap = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long MONGO_SAVE_COOLDOWN_MS = 5 * 60 * 1000; // 5 minutes

    // Auto-detects class name "StockIndicesService"
    private final AppLogger log = AppLogger.getLogger();

    private final MarketDataProcessingService marketDataProcessingService;
    private final StockIndicesMarketDataService stockIndicesMarketDataService;
    private final MarketDataFetchService marketDataCacheService;
    private final MarketDataCacheService redisCacheService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @Value("${market.data.cache.enabled:true}")
    private boolean cacheEnabled;

    public List<StockIndicesMarketData> getLatestIndicesData(List<String> indexSymbols) {
        return getLatestIndicesData(indexSymbols, false);
    }

    public List<StockIndicesMarketData> getLatestIndicesData(List<String> indexSymbols, boolean forceRefresh) {
        String methodName = "getLatestIndicesData";
        try {
            List<StockIndicesMarketData> finalResults = new ArrayList<>();
            List<String> symbolsToProcess = new ArrayList<>();

            // 1. Load from MongoDB Database
            checkDatabase(indexSymbols, forceRefresh, finalResults, symbolsToProcess, methodName);

            // 2. Scrape only if MongoDB document is completely missing (constituent document repair)
            if (!symbolsToProcess.isEmpty()) {
                if (forceRefresh) {
                    log.info(methodName, "Force refresh requested. Triggering fresh fetch/scraper.");
                    fetchFreshData(symbolsToProcess, finalResults, methodName);
                } else {
                    log.info(methodName, "Constituent documents missing for some symbols. Triggering repair scraper.");
                    fetchFreshData(symbolsToProcess, finalResults, methodName);
                }
            }

            // 3. Enrich with Redis latest streaming prices or live prices API
            Set<String> requestedSymbols = new HashSet<>(indexSymbols);
            Map<String, OHLCQuote> latestPrices = redisCacheService.getLatestPrices(requestedSymbols);

            if (latestPrices == null) {
                latestPrices = new java.util.HashMap<>();
            }

            // Fallback for missing symbols to marketDataCacheService.getLivePrices
            Set<String> missingSymbols = new HashSet<>();
            for (String sym : requestedSymbols) {
                if (!latestPrices.containsKey(sym) || latestPrices.get(sym).getLastPrice() == 0.0) {
                    missingSymbols.add(sym);
                }
            }

            if (!missingSymbols.isEmpty()) {
                try {
                    Map<String, Object> liveResponse = marketDataCacheService.getLivePrices(missingSymbols, true, forceRefresh);
                    if (liveResponse != null && liveResponse.get("prices") instanceof List) {
                        List<?> pricesList = (List<?>) liveResponse.get("prices");
                        for (Object obj : pricesList) {
                            if (obj instanceof com.am.common.investment.model.equity.EquityPrice) {
                                com.am.common.investment.model.equity.EquityPrice ep = (com.am.common.investment.model.equity.EquityPrice) obj;
                                if (ep.getSymbol() != null && ep.getLastPrice() != null) {
                                    String matchingSymbol = null;
                                    for (String reqSym : requestedSymbols) {
                                        if (reqSym.equalsIgnoreCase(ep.getSymbol())) {
                                            matchingSymbol = reqSym;
                                            break;
                                        }
                                    }
                                    if (matchingSymbol != null) {
                                        OHLCQuote quote = latestPrices.getOrDefault(matchingSymbol, new OHLCQuote());
                                        quote.setLastPrice(ep.getLastPrice());
                                        if (ep.getOhlcv() != null) {
                                            OHLCQuote.OHLC ohlc = new OHLCQuote.OHLC();
                                            ohlc.setOpen(ep.getOhlcv().getOpen());
                                            ohlc.setHigh(ep.getOhlcv().getHigh());
                                            ohlc.setLow(ep.getOhlcv().getLow());
                                            ohlc.setClose(ep.getOhlcv().getClose());
                                            quote.setOhlc(ohlc);
                                        }
                                        latestPrices.put(matchingSymbol, quote);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error(methodName, "Error fetching fallback live prices for indices", e);
                }
            }

            if (!latestPrices.isEmpty()) {
                List<StockIndicesMarketData> docsToSave = new ArrayList<>();
                // Use UTC for all timestamp comparisons (updatedAt is stored as UTC on server)
                java.time.LocalDateTime now = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
                long nowMs = System.currentTimeMillis();

                for (StockIndicesMarketData data : finalResults) {
                    String symbol = data.getIndexSymbol();
                    OHLCQuote priceQuote = latestPrices.get(symbol);
                    if (priceQuote != null && priceQuote.getLastPrice() != 0.0) {
                        IndexMetadata meta = data.getMetadata();
                        if (meta == null) {
                            meta = new IndexMetadata();
                            data.setMetadata(meta);
                        }

                        double lastPrice = priceQuote.getLastPrice();
                        double open = priceQuote.getOhlc() != null ? priceQuote.getOhlc().getOpen() : 0.0;
                        double high = priceQuote.getOhlc() != null ? priceQuote.getOhlc().getHigh() : 0.0;
                        double low = priceQuote.getOhlc() != null ? priceQuote.getOhlc().getLow() : 0.0;
                        
                        double previousClose = priceQuote.getPreviousClose();
                        if (previousClose == 0.0 && meta.getPreviousClose() != null) {
                            previousClose = meta.getPreviousClose();
                        }
                        
                        double change = lastPrice - previousClose;
                        double changePercent = previousClose != 0 ? (change / previousClose) * 100.0 : 0.0;

                        meta.setLast(lastPrice);
                        if (open != 0.0) meta.setOpen(open);
                        if (high != 0.0) meta.setHigh(high);
                        if (low != 0.0) meta.setLow(low);
                        meta.setPreviousClose(previousClose);
                        meta.setChange(change);
                        meta.setPercChange(changePercent);
                        meta.setTimeVal(String.valueOf(nowMs));

                        // 1. Check local JVM memory cooldown to prevent race conditions
                        long lastSaveTime = lastMongoSaveTimeMap.getOrDefault(symbol, 0L);
                        boolean shouldSave = (nowMs - lastSaveTime) >= MONGO_SAVE_COOLDOWN_MS;

                        if (shouldSave) {
                            // 2. Check document's actual database timestamp for timezone-safe validation
                            com.am.common.investment.model.stockindice.AuditData audit = data.getAudit();
                            if (audit == null) {
                                audit = new com.am.common.investment.model.stockindice.AuditData();
                                audit.setCreatedAt(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
                                data.setAudit(audit);
                            } else if (audit.getUpdatedAt() != null) {
                                // updatedAt is stored as UTC; compare against UTC
                                long minutesOld = java.time.Duration.between(audit.getUpdatedAt(), java.time.LocalDateTime.now(java.time.ZoneOffset.UTC)).toMinutes();
                                if (minutesOld < 5) {
                                    shouldSave = false; // Document was updated by another pod/process recently
                                }
                            }
                        }

                        if (shouldSave) {
                            // Update both the JVM cache and the MongoDB document timestamp
                            lastMongoSaveTimeMap.put(symbol, nowMs);
                            if (data.getAudit() != null) {
                                data.getAudit().setUpdatedAt(now);
                            }
                            docsToSave.add(data);
                        }

                        log.debug(methodName, "Enriched index " + symbol + " from latest prices. Price=" + lastPrice);
                    }
                }

                // Asynchronously save the debounced index documents back to MongoDB
                if (!docsToSave.isEmpty()) {
                    CompletableFuture.runAsync(() -> {
                        try {
                            for (StockIndicesMarketData doc : docsToSave) {
                                stockIndicesMarketDataService.save(doc);
                            }
                            log.info("getLatestIndicesData", "Asynchronously persisted " + docsToSave.size() + " debounced indices back to MongoDB.");
                        } catch (Exception ex) {
                            log.error("getLatestIndicesData", "Failed to save enriched indices to MongoDB", ex);
                        }
                    });
                }
            }

            // 4. Sanitize stale data before returning to prevent showing wrong prices
            // NOTE: updatedAt is stored as UTC (server default). Compare only against UTC to avoid timezone skew.
            long nowMsFinal = System.currentTimeMillis();
            for (StockIndicesMarketData data : finalResults) {
                com.am.common.investment.model.stockindice.AuditData audit = data.getAudit();
                boolean isStale = true;
                if (audit != null && audit.getUpdatedAt() != null) {
                    java.time.LocalDateTime updatedAt = audit.getUpdatedAt();
                    // Both sides must use UTC to avoid a phantom 5h30m staleness from IST vs UTC mismatch.
                    long minutesOld = java.time.Duration.between(updatedAt, java.time.LocalDateTime.now(java.time.ZoneOffset.UTC)).toMinutes();
                    
                    if (minutesOld <= 15) {
                        isStale = false;
                    } else {
                        // Check if outside market hours (IST) and less than 3 days old (weekend)
                        java.time.LocalTime istTime = java.time.LocalTime.now(java.time.ZoneId.of("Asia/Kolkata"));
                        boolean isMarketHours = istTime.isAfter(java.time.LocalTime.of(9, 10)) && istTime.isBefore(java.time.LocalTime.of(15, 45));
                        if (!isMarketHours && minutesOld < 3 * 24 * 60) {
                            isStale = false;
                        }
                    }
                }
                
                // If it is STILL stale, Redis is empty and DB is outdated.
                // Clear the prices to gracefully show "N/A" on the UI instead of wrong values.
                if (isStale && data.getMetadata() != null) {
                    data.getMetadata().setLast(0.0);
                    data.getMetadata().setChange(0.0);
                    data.getMetadata().setPercChange(0.0);
                }
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

                docs.forEach(doc -> {
                    if (doc != null && doc.getIndexSymbol() != null) {
                        boolean isStale = false;
                        if (doc.getAudit() != null && doc.getAudit().getUpdatedAt() != null) {
                            java.time.LocalDateTime updatedAt = doc.getAudit().getUpdatedAt();
                            // updatedAt is stored as UTC (server default). Compare against UTC to avoid IST offset skew.
                            long minutesOld = java.time.Duration.between(updatedAt, java.time.LocalDateTime.now(java.time.ZoneOffset.UTC)).toMinutes();
                            if (minutesOld > 15) {
                                isStale = true;
                            }
                        } else {
                            isStale = true;
                        }

                        if (isStale) {
                            java.time.LocalTime istTime = java.time.LocalTime.now(java.time.ZoneId.of("Asia/Kolkata"));
                            boolean isMarketHours = istTime.isAfter(java.time.LocalTime.of(9, 10)) && istTime.isBefore(java.time.LocalTime.of(15, 45));
                            if (!isMarketHours) {
                                isStale = false; // Outside market hours, consider it fresh
                            }
                        }

                        // Add to results regardless of staleness to avoid blocking. 
                        // Enrichment will provide latest prices from Redis anyway.
                        finalResults.add(doc);
                        foundSymbols.add(doc.getIndexSymbol());

                        if (isStale) {
                            log.info(methodName, "Document for " + doc.getIndexSymbol() + " is stale during market hours, but will return it with Redis enrichment to avoid blocking.");
                            // If it's very stale, we might still want to trigger a background update, 
                            // but for now let's just prioritize response time.
                        } else {
                            log.info(methodName, "Found fresh data for " + doc.getIndexSymbol() + " in database");
                        }
                    }
                });

                // Identify missing symbols
                for (String symbol : indexSymbols) {
                    if (!foundSymbols.contains(symbol)) {
                        log.info(methodName, "Symbol " + symbol + " not found or stale in database. Queuing for fresh fetch.");
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

        // Wait for completion with a timeout guard to prevent API hanging if a scraper fails
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(4, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            log.warn(methodName, "Scraping fresh index data timed out after 4 seconds, continuing with cached/seeded records");
        } catch (Exception e) {
            log.error(methodName, "Error during parallel index data fetch", e);
        }

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
