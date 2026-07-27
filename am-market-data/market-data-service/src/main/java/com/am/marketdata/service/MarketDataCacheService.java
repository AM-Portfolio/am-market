package com.am.marketdata.service;

import com.am.common.investment.model.historical.HistoricalData;
import com.am.common.investment.model.historical.OHLCVTPoint;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.common.util.ApplicationContextProvider;
import com.am.marketdata.redis.model.OHLCV;
import com.am.marketdata.redis.model.StockBars;
import com.am.marketdata.redis.service.StockCacheService;
import com.am.marketdata.redis.util.CacheLoggingUtil;

import com.am.marketdata.common.log.AppLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.am.marketdata.service.model.PreviousCloseDocument;
import com.am.marketdata.service.repo.PreviousCloseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.TimeUnit;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import java.util.Set;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of MarketDataCacheService for caching market data in Redis
 */
@Service("serviceModuleMarketDataCacheService")
public class MarketDataCacheService {

    private final AppLogger log = AppLogger.getLogger();
    private static final String DEFAULT_INTERVAL = "5m";

    private final StockCacheService stockCacheService;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final PreviousCloseRepository previousCloseRepository;

    public MarketDataCacheService(StockCacheService stockCacheService,
                                  ObjectMapper objectMapper,
                                  RedisTemplate<String, String> redisTemplate,
                                  @Autowired(required = false) PreviousCloseRepository previousCloseRepository) {
        this.stockCacheService = stockCacheService;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.previousCloseRepository = previousCloseRepository;
    }

    /**
     * Helper method to normalize raw symbol inputs before constructing Redis keys.
     * Ensures keys are normalized (e.g. NSE_INDEX|Nifty 50 -> NIFTY 50) so that
     * scheduler writes and websocket reads resolve to the exact same cache keys.
     */
    private String normalizeSymbol(String rawSymbol) {
        if (rawSymbol == null) {
            return null;
        }
        String symbol = rawSymbol;
        if (symbol.contains("|")) {
            symbol = symbol.substring(symbol.indexOf("|") + 1);
        }
        if (symbol.contains(":")) {
            symbol = symbol.substring(symbol.indexOf(":") + 1);
        }
        return symbol.toUpperCase().trim();
    }

    public Double getPreviousClose(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        if (normalizedSymbol == null) {
            return null;
        }
        try {
            // 1. Redis fast path
            String redisKey = "market:prev-close:" + normalizedSymbol;
            String value = redisTemplate.opsForValue().get(redisKey);
            if (value != null) {
                return Double.parseDouble(value);
            }
        } catch (Exception e) {
            log.warn("getPreviousClose", "Failed to get previous close from Redis for symbol: " + symbol + ", trying MongoDB", e);
        }

        // 2. Fallback to MongoDB persistence
        if (previousCloseRepository != null) {
            try {
                Optional<PreviousCloseDocument> docOpt = previousCloseRepository.findById(normalizedSymbol);
                if (docOpt.isPresent() && docOpt.get().getPreviousClose() != null && docOpt.get().getPreviousClose() > 0) {
                    Double dbValue = docOpt.get().getPreviousClose();
                    // Optionally backfill Redis
                    try {
                        String redisKey = "market:prev-close:" + normalizedSymbol;
                        redisTemplate.opsForValue().set(redisKey, String.valueOf(dbValue), 26, TimeUnit.HOURS);
                    } catch (Exception ignore) {}
                    return dbValue;
                }
            } catch (Exception mongoEx) {
                log.warn("getPreviousClose", "Failed to get previous close from MongoDB for symbol: " + symbol, mongoEx);
            }
        }
        return null;
    }

    public void setPreviousClose(String symbol, double previousClose) {
        if (previousClose <= 0) {
            return;
        }
        String normalizedSymbol = normalizeSymbol(symbol);
        if (normalizedSymbol == null) {
            return;
        }
        // 1. Cache to Redis
        try {
            String redisKey = "market:prev-close:" + normalizedSymbol;
            redisTemplate.opsForValue().set(redisKey, String.valueOf(previousClose), 26, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("setPreviousClose", "Failed to set previous close in Redis for symbol: " + symbol, e);
        }

        // 2. Upsert to MongoDB (single record per symbol)
        if (previousCloseRepository != null) {
            try {
                PreviousCloseDocument doc = PreviousCloseDocument.builder()
                        .symbol(normalizedSymbol)
                        .previousClose(previousClose)
                        .tradeDate(LocalDate.now(ZoneId.of("Asia/Kolkata")).toString())
                        .updatedAt(Instant.now())
                        .build();
                previousCloseRepository.save(doc);
            } catch (Exception mongoEx) {
                log.warn("setPreviousClose", "Failed to upsert previous close in MongoDB for symbol: " + symbol, mongoEx);
            }
        }
    }

    public void cacheOHLCData(Map<String, OHLCQuote> ohlcData, TimeFrame timeFrame) {
        try {
            String interval = timeFrame != null ? timeFrame.getApiValue() : "1D";
            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

            log.info("[INTERVAL_TRACE]", String.format(
                    "MarketDataCacheService.cacheOHLCData: Caching %d symbols with timeFrame: %s (enum: %s, apiValue: %s) for date: %s",
                    ohlcData.size(), timeFrame, timeFrame != null ? timeFrame.name() : "null", interval, today));

            /*
             * PERFORMANCE OPTIMIZATION: BATCH SYMBOL COLLECTION
             * ---------------------------------------------------------------------------------------------
             * WHAT PROBLEM IT SOLVES:
             * Previously, stockCacheService.cacheIntradayBars(...) was called inside this loop per symbol.
             * For 300 symbols, that triggered 300-600 individual TCP network calls, taking 1.3 minutes (80s).
             * 
             * HOW IT WORKS:
             * We accumulate all symbols into 'batchStockBars' first, then execute 1 bulk micro-pipelined
             * flush via stockCacheService.cacheIntradayBars(batchStockBars).
             */
            List<String> cachedKeys = new ArrayList<>();
            List<com.am.marketdata.redis.model.StockBars> batchStockBars = new ArrayList<>();

            for (Map.Entry<String, OHLCQuote> entry : ohlcData.entrySet()) {
                String fullSymbol = entry.getKey();
                // Remove all exchange prefixes (NSE_EQ:, NSE:, etc.)
                String symbol = fullSymbol.contains(":") ? fullSymbol.substring(fullSymbol.indexOf(":") + 1)
                        : fullSymbol;
                OHLCQuote quote = entry.getValue();

                if (quote == null || quote.getOhlc() == null) {
                    log.warn("cacheOHLCData", "Skipping symbol {} due to missing OHLC data", symbol);
                    continue;
                }

                // Create OHLCV from OHLCQuote
                OHLCV ohlcv = StockCacheService.createPricePoint(
                        LocalDateTime.now(),
                        quote.getOhlc().getOpen(),
                        quote.getOhlc().getHigh(),
                        quote.getOhlc().getLow(),
                        quote.getOhlc().getClose(),
                        0L,
                        quote.getLastPrice());

                List<OHLCV> bars = new ArrayList<>();
                bars.add(ohlcv);

                com.am.marketdata.redis.model.StockBars stockBars = com.am.marketdata.redis.model.StockBars.builder()
                        .symbol(symbol)
                        .interval(interval)
                        .startDate(today)
                        .bars(bars)
                        .build();

                batchStockBars.add(stockBars);

                // Collect Redis key for logging
                String redisKey = String.format("stock:intraday:%s:%s:%s", symbol.toUpperCase(), interval, today);
                cachedKeys.add(symbol + " -> " + redisKey);
            }

            // Execute 1 bulk micro-pipelined batch write instead of 300 individual calls
            if (!batchStockBars.isEmpty()) {
                stockCacheService.cacheIntradayBars(batchStockBars);
            }

            // Smart logging: if keys are huge (>1000), show only count in INFO and one
            // sample in DEBUG
            if (cachedKeys.size() > 1000) {
                log.info("cacheOHLCData", "Cached {} symbols with timeframe: {} for date: {} ({} key-value pairs)",
                        ohlcData.size(), interval, today, cachedKeys.size());

                // Show one sample record in DEBUG mode to know the pattern
                if (!cachedKeys.isEmpty()) {
                    log.debug("cacheOHLCData", "Sample key pattern: {}", cachedKeys.get(0));
                }
            } else {
                // Log first 3 keys as samples for smaller datasets
                List<String> sampleKeys = cachedKeys.subList(0, Math.min(3, cachedKeys.size()));
                log.info("cacheOHLCData", "Cached {} symbols with timeframe: {} for date: {}. Sample keys: {}",
                        ohlcData.size(), interval, today, sampleKeys);
            }

            // Also persist lastPrice + previousClose to market:latest-price:* so that
            // subsequent cache reads can overlay previousClose correctly.
            // The stock:intraday:* path does NOT store previousClose, so this is the only
            // durable place for it.
            try {
                cacheLatestPrices(ohlcData);
                log.debug("cacheOHLCData", "Also cached latest prices (incl. previousClose) for {} symbols", ohlcData.size());
            } catch (Exception latestEx) {
                log.warn("cacheOHLCData", "Failed to cache latest prices alongside OHLC data: {}", latestEx.getMessage());
            }
        } catch (Exception e) {
            // Use the specialized exception logging
            CacheLoggingUtil.logCacheException(log, "CACHE_OHLC", null, "Error caching OHLC data", e);
            // Don't rethrow as this is a non-critical operation
        }
    }

    public void cacheHistoricalData(String symbol, TimeFrame timeFrame, HistoricalData historicalData) {
        try {
            if (historicalData == null || historicalData.getDataPoints() == null
                    || historicalData.getDataPoints().isEmpty()) {
                log.warn("cacheHistoricalData", "No historical data to cache for symbol: " + symbol);
                return;
            }

            // Get the data points directly as OHLCVTPoint objects
            List<OHLCVTPoint> points = historicalData.getDataPoints();
            List<OHLCV> ohlcvs = points
                    .stream().map(point -> StockCacheService.createPricePoint(point.getTime(), point.getOpen(),
                            point.getHigh(), point.getLow(), point.getClose(), point.getVolume(), null))
                    .collect(Collectors.toList());

            // Cache the historical data
            if (!points.isEmpty()) {
                // Use the specialized logging utility
                CacheLoggingUtil.logHistoricalDataCaching(log, symbol, timeFrame.getApiValue(), points);

                // For daily data, use the historical bar caching
                if (timeFrame == TimeFrame.DAY || timeFrame == TimeFrame.WEEK ||
                        timeFrame == TimeFrame.MONTH || timeFrame == TimeFrame.YEAR) {

                    // Use batch caching for better performance (Redis Pipelining)
                    stockCacheService.cacheHistoricalDataBatch(symbol, timeFrame, ohlcvs);

                } else {
                    // For intraday data, use the intraday bars caching
                    stockCacheService.cacheIntradayBars(symbol, timeFrame.getApiValue(), ohlcvs);
                }
            }
        } catch (Exception e) {
            // Use the specialized exception logging
            CacheLoggingUtil.logCacheException(log, "CACHE_HISTORICAL", symbol,
                    "Error caching historical data", e);
            // Don't rethrow as this is a non-critical operation
        }
    }

    public Map<String, OHLCQuote> getOHLCFromCache(List<String> tradingSymbols, TimeFrame timeFrame) {
        try {
            // Clean symbols (remove exchange prefixes)
            List<String> cleanSymbols = tradingSymbols.stream()
                    .map(symbol -> {
                        String clean = symbol;
                        if (clean.contains("|")) {
                            clean = clean.substring(clean.indexOf("|") + 1);
                        }
                        if (clean.contains(":")) {
                            clean = clean.substring(clean.indexOf(":") + 1);
                        }
                        return clean.toUpperCase().trim();
                    })
                    .collect(Collectors.toList());

            if (cleanSymbols.isEmpty()) {
                log.debug("getOHLCFromCache", "All symbols were indices, skipping cache lookup");
                return Collections.emptyMap();
            }

            // Get today's date in the same format used for caching
            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

            // Log the cache retrieval operation
            List<String> expectedKeys = new ArrayList<>();
            for (String symbol : cleanSymbols) {
                String redisKey = String.format("stock:intraday:%s:%s:%s", symbol.toUpperCase(),
                        timeFrame.getApiValue(), today);
                expectedKeys.add(symbol + " -> " + redisKey);
            }

            log.info("[INTERVAL_TRACE]", String.format(
                    "MarketDataCacheService.getOHLCFromCache: Attempting to retrieve OHLC data from cache for %d symbols with timeFrame: %s (enum: %s, apiValue: %s) on date: %s",
                    cleanSymbols.size(), timeFrame, timeFrame != null ? timeFrame.name() : "null",
                    timeFrame.getApiValue(), today));

            log.debug("getOHLCFromCache", "Expected Redis keys: {}", expectedKeys);

            // Try to get data from cache
            Map<String, StockBars> cachedBars = stockCacheService.getTodayMultiSymbolBars(cleanSymbols,
                    timeFrame.getApiValue());

            log.info("getOHLCFromCache", "[CACHE_RESULT] Redis returned {} stocks out of {} requested",
                    cachedBars != null ? cachedBars.size() : 0, cleanSymbols.size());

            if (cachedBars == null || cachedBars.isEmpty()) {
                if (cachedBars == null || cachedBars.isEmpty()) {
                    log.debug("getOHLCFromCache", "No OHLC data found in cache for the requested symbols");
                    return Collections.emptyMap();
                }
                return Collections.emptyMap();
            }

            // Convert cached data to OHLCQuote format
            Map<String, OHLCQuote> result = new HashMap<>();
            Map<String, String> cacheHits = new HashMap<>();

            for (Map.Entry<String, StockBars> entry : cachedBars.entrySet()) {
                String symbol = entry.getKey();
                StockBars bars = entry.getValue();

                if (bars != null && bars.getBars() != null && !bars.getBars().isEmpty()) {
                    // Get the latest bar
                    OHLCV latestBar = bars.getBars().get(bars.getBars().size() - 1);

                    // Create OHLCQuote from the latest bar
                    OHLCQuote quote = createOHLCQuoteFromBar(latestBar);
                    result.put(symbol, quote);

                    // Record the cache hit for logging
                    cacheHits.put(symbol, String.format("O:%.2f,H:%.2f,L:%.2f,C:%.2f",
                            latestBar.getOpen(), latestBar.getHigh(), latestBar.getLow(), latestBar.getClose()));
                }
            }

            if (!result.isEmpty()) {
                // Log the cache hits with values
                log.info("getOHLCFromCache", "Retrieved OHLC data from cache for {} symbols", result.size());
                log.debug("getOHLCFromCache", "Retrieved values: {}", cacheHits);

                // Overlay lastPrice and previousClose from Redis Path 2 (market:latest-price:*)
                // These are written by cacheLatestPrices() on every WebSocket tick.
                // The intraday bars (Path 1) do not store previousClose, so it defaults to 0.0.
                // This overlay fixes both: fresh lastPrice on reload and non-zero previousClose.
                overlayLatestPrices(result);
            }

            return result;
        } catch (Exception e) {
            // Use the specialized exception logging
            CacheLoggingUtil.logCacheException(log, "GET_OHLC_CACHE", String.join(", ", tradingSymbols),
                    "Error retrieving OHLC data from cache", e);
            return Collections.emptyMap();
        }
    }

    /**
     * Overlays the latest prices and previous close from the fast Redis cache onto the provided OHLC quotes.
     * This is useful when fetching data from sources that might not have the absolute latest price or previous close.
     * 
     * @param result The map of OHLCQuotes to overlay with latest prices
     */
    public void overlayLatestPrices(Map<String, OHLCQuote> result) {
        if (result == null || result.isEmpty()) {
            return;
        }

        // OPTIMIZATION: Combine 500 individual Redis GET calls into 1 single Redis MGET (multiGet) call.
        // This eliminates 500 network roundtrips and cleans up Grafana Tempo traces.
        List<Map.Entry<String, OHLCQuote>> entries = new ArrayList<>(result.entrySet());
        List<String> keys = new ArrayList<>(entries.size());

        for (Map.Entry<String, OHLCQuote> entry : entries) {
            String symbol = entry.getKey();
            if (symbol.contains("|")) {
                symbol = symbol.substring(symbol.indexOf("|") + 1);
            }
            if (symbol.contains(":")) {
                symbol = symbol.substring(symbol.indexOf(":") + 1);
            }
            symbol = symbol.toUpperCase().trim();
            keys.add("market:latest-price:" + symbol);
        }

        try {
            // Batch retrieve all latest price JSONs in 1 single Redis MGET command
            List<String> jsonList = redisTemplate.opsForValue().multiGet(keys);

            if (jsonList != null) {
                for (int i = 0; i < entries.size(); i++) {
                    String json = jsonList.get(i);
                    if (json != null) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> latestData = objectMapper.readValue(json, Map.class);
                            double latestPrice = ((Number) latestData.getOrDefault("lastPrice", 0.0)).doubleValue();
                            double prevClose = ((Number) latestData.getOrDefault("previousClose", 0.0)).doubleValue();
                            OHLCQuote quote = entries.get(i).getValue();
                            if (latestPrice > 0) {
                                quote.setLastPrice(latestPrice);
                            }
                            if (prevClose > 0) {
                                quote.setPreviousClose(prevClose);
                            }
                        } catch (Exception parseEx) {
                            log.warn("overlayLatestPrices", "Failed to parse Redis latest-price JSON for symbol {}: {}",
                                    entries.get(i).getKey(), parseEx.getMessage());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("overlayLatestPrices", "Batch overlay failed for {} symbols: {}", keys.size(), ex.getMessage());
        }

        // FALLBACK: Query MongoDB for any symbols where previousClose is still 0.0
        if (previousCloseRepository != null) {
            List<String> missingSymbols = new ArrayList<>();
            for (Map.Entry<String, OHLCQuote> entry : result.entrySet()) {
                if (entry.getValue() != null && entry.getValue().getPreviousClose() == 0.0) {
                    String norm = normalizeSymbol(entry.getKey());
                    if (norm != null) {
                        missingSymbols.add(norm);
                    }
                }
            }

            if (!missingSymbols.isEmpty()) {
                try {
                    List<PreviousCloseDocument> mongoDocs = previousCloseRepository.findBySymbolIn(missingSymbols);
                    if (mongoDocs != null && !mongoDocs.isEmpty()) {
                        Map<String, Double> dbCloseMap = mongoDocs.stream()
                                .filter(d -> d.getPreviousClose() != null && d.getPreviousClose() > 0)
                                .collect(Collectors.toMap(PreviousCloseDocument::getSymbol, PreviousCloseDocument::getPreviousClose, (v1, v2) -> v1));

                        for (Map.Entry<String, OHLCQuote> entry : result.entrySet()) {
                            OHLCQuote quote = entry.getValue();
                            if (quote != null && quote.getPreviousClose() == 0.0) {
                                String norm = normalizeSymbol(entry.getKey());
                                Double dbClose = dbCloseMap.get(norm);
                                if (dbClose != null && dbClose > 0) {
                                    quote.setPreviousClose(dbClose);
                                }
                            }
                        }
                    }
                } catch (Exception mongoEx) {
                    log.warn("overlayLatestPrices", "Failed to fetch previousClose from MongoDB fallback: " + mongoEx.getMessage());
                }
            }
        }
    }
    /**
     * Batch retrieval of historical data from cache for multiple symbols
     * 
     * @param symbols       List of symbols to retrieve
     * @param timeFrame     The timeframe for the data
     * @param fromDate      Start date in ISO format (yyyy-MM-dd)
     * @param toDate        End date in ISO format (yyyy-MM-dd)
     * @param isIndexSymbol Whether the symbols are index symbols (for index cache
     *                      checking)
     * @return Map of symbol to HistoricalData for all symbols found in cache
     */
    public Map<String, HistoricalData> getHistoricalDataFromCacheBatch(List<String> symbols, TimeFrame timeFrame,
            String fromDate, String toDate, boolean isIndexSymbol) {
        try {
            if (symbols == null || symbols.isEmpty()) {
                return Collections.emptyMap();
            }

            log.info("[BATCH_CACHE]", String.format(
                    "Attempting to retrieve historical data from cache for %d symbols with timeFrame: %s (apiValue: %s), from: %s, to: %s",
                    symbols.size(), timeFrame, timeFrame != null ? timeFrame.getApiValue() : "null", fromDate,
                    toDate));

            Map<String, HistoricalData> result = new HashMap<>();

            // For daily/weekly/monthly/yearly data
            if (timeFrame == TimeFrame.DAY || timeFrame == TimeFrame.WEEK || timeFrame == TimeFrame.MONTH
                    || timeFrame == TimeFrame.YEAR) {

                // Use the date range method to get all data in a single call
                Map<String, List<StockBars>> batchBars = stockCacheService.getHistoricalBarsWithStats(symbols,
                        fromDate, toDate, timeFrame.getApiValue(), isIndexSymbol);

                if (batchBars != null && !batchBars.isEmpty()) {
                    // Process each symbol's data
                    for (Map.Entry<String, List<StockBars>> entry : batchBars.entrySet()) {
                        String symbol = entry.getKey();
                        List<StockBars> stockBarsList = entry.getValue();

                        if (stockBarsList != null && !stockBarsList.isEmpty()) {
                            // Create HistoricalData for this symbol
                            HistoricalData historicalData = new HistoricalData();
                            historicalData.setTradingSymbol(symbol);
                            List<OHLCVTPoint> dataPoints = new ArrayList<>();

                            // Add all bars to the data points
                            for (StockBars stockBars : stockBarsList) {
                                if (stockBars.getBars() != null && !stockBars.getBars().isEmpty()) {
                                    for (OHLCV bar : stockBars.getBars()) {
                                        OHLCVTPoint point = OHLCVTPoint.builder()
                                                .time(bar.getTime())
                                                .open(bar.getOpen())
                                                .high(bar.getHigh())
                                                .low(bar.getLow())
                                                .close(bar.getClose())
                                                .volume(bar.getVolume())
                                                .build();
                                        dataPoints.add(point);
                                    }
                                }
                            }

                            if (!dataPoints.isEmpty()) {
                                historicalData.setDataPoints(dataPoints);
                                result.put(symbol, historicalData);
                            }
                        }
                    }
                }
            } else {
                // For intraday data
                // Batch retrieve intraday bars for all symbols
                Map<String, StockBars> batchBars = stockCacheService.getMultiSymbolBarsWithStats(symbols,
                        timeFrame.getApiValue(), fromDate);

                if (batchBars != null && !batchBars.isEmpty()) {
                    for (Map.Entry<String, StockBars> entry : batchBars.entrySet()) {
                        String symbol = entry.getKey();
                        StockBars stockBars = entry.getValue();

                        if (stockBars != null && stockBars.getBars() != null && !stockBars.getBars().isEmpty()) {
                            result.put(symbol, convertToHistoricalData(symbol, stockBars.getBars()));
                        }
                    }
                }
            }

            if (!result.isEmpty()) {
                log.info("[BATCH_CACHE]",
                        String.format("Retrieved historical data from cache for %d/%d symbols with timeFrame: %s",
                                result.size(), symbols.size(), timeFrame.getApiValue()));
            } else {
                log.debug("[BATCH_CACHE]",
                        String.format("No historical data found in cache for any of the %d symbols with timeFrame: %s",
                                symbols.size(), timeFrame.getApiValue()));
            }

            return result;
        } catch (Exception e) {
            CacheLoggingUtil.logCacheException(log, "BATCH_GET_HISTORICAL_CACHE", String.join(", ", symbols),
                    "Error retrieving historical data from cache in batch", e);
            return Collections.emptyMap();
        }
    }

    /**
     * Cache aggregated historical data at index level
     * 
     * @param indexSymbol     The index symbol (e.g., "NIFTY 50")
     * @param timeFrame       The timeframe for the data
     * @param fromDate        Start date in ISO format (yyyy-MM-dd)
     * @param toDate          End date in ISO format (yyyy-MM-dd)
     * @param constituentData Map of constituent symbol to HistoricalData
     */
    public void cacheIndexHistoricalData(String indexSymbol, TimeFrame timeFrame, String fromDate, String toDate,
            Map<String, HistoricalData> constituentData) {
        try {
            if (constituentData == null || constituentData.isEmpty()) {
                log.warn("[INDEX_CACHE]", "No constituent data to cache for index: " + indexSymbol);
                return;
            }

            String cacheKey = String.format("index:historical:%s:%s:%s:%s",
                    indexSymbol.toUpperCase(), timeFrame.getApiValue(), fromDate, toDate);

            // Convert the map to a format suitable for Redis storage
            // We'll store it as a hash with each constituent as a field
            Map<String, String> hashData = new HashMap<>();
            for (Map.Entry<String, HistoricalData> entry : constituentData.entrySet()) {
                String symbol = entry.getKey();
                HistoricalData data = entry.getValue();

                // Serialize the HistoricalData to JSON string
                try {
                    String jsonData = serializeHistoricalData(data);
                    hashData.put(symbol, jsonData);
                } catch (Exception e) {
                    log.warn("[INDEX_CACHE]", "Failed to serialize data for symbol: " + symbol, e);
                }
            }

            if (!hashData.isEmpty()) {
                stockCacheService.cacheIndexHistoricalData(cacheKey, hashData);
                log.info("[INDEX_CACHE]",
                        String.format("Cached index historical data for %s with %d constituents (key: %s)",
                                indexSymbol, constituentData.size(), cacheKey));
            }
        } catch (Exception e) {
            CacheLoggingUtil.logCacheException(log, "CACHE_INDEX_HISTORICAL", indexSymbol,
                    "Error caching index historical data", e);
        }
    }

    /**
     * Retrieve cached index-level historical data
     * 
     * @param indexSymbol The index symbol (e.g., "NIFTY 50")
     * @param timeFrame   The timeframe for the data
     * @param fromDate    Start date in ISO format (yyyy-MM-dd)
     * @param toDate      End date in ISO format (yyyy-MM-dd)
     * @return Map of constituent symbol to HistoricalData if found, null otherwise
     */
    public Map<String, HistoricalData> getIndexHistoricalDataFromCache(String indexSymbol, TimeFrame timeFrame,
            String fromDate, String toDate) {
        try {
            String cacheKey = String.format("index:historical:%s:%s:%s:%s",
                    indexSymbol.toUpperCase(), timeFrame.getApiValue(), fromDate, toDate);

            log.debug("[INDEX_CACHE]", String.format(
                    "Attempting to retrieve index historical data from cache for %s (key: %s)",
                    indexSymbol, cacheKey));

            Map<String, String> hashData = stockCacheService.getIndexHistoricalData(cacheKey);

            if (hashData != null && !hashData.isEmpty()) {
                Map<String, HistoricalData> result = new HashMap<>();

                for (Map.Entry<String, String> entry : hashData.entrySet()) {
                    String symbol = entry.getKey();
                    String jsonData = entry.getValue();

                    try {
                        HistoricalData data = deserializeHistoricalData(jsonData);
                        result.put(symbol, data);
                    } catch (Exception e) {
                        log.warn("[INDEX_CACHE]", "Failed to deserialize data for symbol: " + symbol, e);
                    }
                }

                if (!result.isEmpty()) {
                    log.info("[INDEX_CACHE]",
                            String.format("Retrieved index historical data from cache for %s with %d constituents",
                                    indexSymbol, result.size()));
                    return result;
                }
            }

            log.debug("[INDEX_CACHE]", "No index historical data found in cache for: " + indexSymbol);
            return null;
        } catch (Exception e) {
            CacheLoggingUtil.logCacheException(log, "GET_INDEX_HISTORICAL_CACHE", indexSymbol,
                    "Error retrieving index historical data from cache", e);
            return null;
        }
    }

    /**
     * Serialize HistoricalData to JSON string using Jackson
     */
    private String serializeHistoricalData(HistoricalData data) throws Exception {
        return objectMapper.writeValueAsString(data);
    }

    /**
     * Deserialize JSON string to HistoricalData using Jackson
     */
    private HistoricalData deserializeHistoricalData(String jsonData) throws Exception {
        return objectMapper.readValue(jsonData, HistoricalData.class);
    }

    public HistoricalData getHistoricalDataFromCache(String symbol, TimeFrame timeFrame, String fromDate,
            String toDate) {
        try {
            // Log the cache retrieval attempt
            // Log the cache retrieval attempt
            log.debug("[INTERVAL_TRACE]", String.format(
                    "MarketDataCacheService.getHistoricalDataFromCache: Attempting to retrieve historical data from cache for symbol: %s, timeFrame: %s (enum: %s, apiValue: %s), from: %s, to: %s",
                    symbol, timeFrame, timeFrame != null ? timeFrame.name() : "null",
                    timeFrame != null ? timeFrame.getApiValue() : "null", fromDate, toDate));

            // Parse dates
            LocalDate from = LocalDate.parse(fromDate, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate to = LocalDate.parse(toDate, DateTimeFormatter.ISO_LOCAL_DATE);

            // For daily data
            if (timeFrame == TimeFrame.DAY || timeFrame == TimeFrame.WEEK || timeFrame == TimeFrame.MONTH
                    || timeFrame == TimeFrame.YEAR) {
                // Get historical bars for each day in the range
                List<OHLCV> points = new ArrayList<>();
                Map<String, String> cacheHits = new HashMap<>();

                // Iterate through each day in the range
                LocalDate current = from;
                while (!current.isAfter(to)) {
                    String dateStr = current.format(DateTimeFormatter.ISO_LOCAL_DATE);
                    String cacheKey = String.format("stock:historical:%s:%s:%s",
                            symbol.toUpperCase(), timeFrame.getApiValue(), dateStr);

                    StockBars stockBars = stockCacheService.getBarsWithStats(symbol, timeFrame.getApiValue(), dateStr);
                    List<OHLCV> bars = (stockBars != null) ? stockBars.getBars() : null;
                    OHLCV bar = null;
                    if (bars != null && !bars.isEmpty()) {
                        bar = bars.get(0);

                        // Record the cache hit for logging
                        cacheHits.put(cacheKey, String.format("O:%.2f,H:%.2f,L:%.2f,C:%.2f",
                                bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose()));
                    }

                    if (bar != null) {
                        points.add(bar);
                    }

                    current = current.plusDays(1);
                }

                if (!points.isEmpty()) {
                    // Always log just the count in INFO
                    log.info("getHistoricalDataFromCache",
                            String.format(
                                    "Retrieved %d historical data points from cache for symbol: %s (%d cache hits)",
                                    points.size(), symbol, cacheHits.size()));

                    // Log detailed values in DEBUG mode
                    if (!cacheHits.isEmpty()) {
                        log.debug("getHistoricalDataFromCache",
                                String.format("Retrieved values: %s", cacheHits));
                    }

                    return convertToHistoricalData(symbol, points);
                }
            } else {
                // For intraday data
                // Get intraday bars for the specified interval
                String dateStr = from.format(DateTimeFormatter.ISO_LOCAL_DATE);
                String cacheKey = String.format("stock:intraday:%s:%s:%s",
                        symbol.toUpperCase(), timeFrame.getApiValue(), dateStr);

                StockBars stockBars = stockCacheService.getBarsWithStats(symbol, timeFrame.getApiValue(), dateStr);
                List<OHLCV> bars = (stockBars != null) ? stockBars.getBars() : null;

                if (bars != null && !bars.isEmpty()) {
                    // Log the cache hit
                    log.info("getHistoricalDataFromCache",
                            String.format("Retrieved %d intraday data points from cache for symbol: %s with key: %s",
                                    bars.size(), symbol, cacheKey));

                    // Log detailed data at debug level
                    for (OHLCV bar : bars) {
                        log.debug("getHistoricalDataFromCache", String.format(
                                "Retrieved data point: time=%s, open=%.2f, high=%.2f, low=%.2f, close=%.2f, volume=%d",
                                bar.getTime(), bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose(),
                                bar.getVolume()));
                    }

                    return convertToHistoricalData(symbol, bars);
                }
            }

            log.debug("getHistoricalDataFromCache",
                    String.format("No historical data found in cache for symbol: %s with timeFrame: %s", symbol,
                            timeFrame.getApiValue()));
            return null;
        } catch (Exception e) {
            // Use the specialized exception logging
            CacheLoggingUtil.logCacheException(log, "GET_HISTORICAL_CACHE", symbol,
                    "Error retrieving historical data from cache", e);
            return null;
        }
    }

    /**
     * Convert OHLCV list to HistoricalData
     *
     * @param symbol The trading symbol
     * @param points List of OHLCV objects
     * @return HistoricalData object
     */
    private HistoricalData convertToHistoricalData(String symbol, List<OHLCV> points) {
        HistoricalData historicalData = new HistoricalData();
        historicalData.setTradingSymbol(symbol);

        List<OHLCVTPoint> ohlcvtPoints = points.stream().map(point -> OHLCVTPoint.builder()
                .time(point.getTime())
                .open(point.getOpen())
                .high(point.getHigh())
                .low(point.getLow())
                .close(point.getClose())
                .volume(point.getVolume())
                .build()).collect(Collectors.toList());
        // Set the OHLCV list directly as dataPoints
        historicalData.setDataPoints(ohlcvtPoints);

        // No need for additional logging here as the calling methods already log the
        // details

        return historicalData;
    }

    /**
     * Create an OHLCQuote object from an OHLCV
     *
     * @param bar The OHLCV bar
     * @return OHLCQuote object
     */
    private OHLCQuote createOHLCQuoteFromBar(OHLCV bar) {
        // Create a new OHLCQuote object
        OHLCQuote quote = new OHLCQuote();

        // Create and set the OHLC object
        OHLCQuote.OHLC ohlc = new OHLCQuote.OHLC();
        ohlc.setOpen(bar.getOpen());
        ohlc.setHigh(bar.getHigh());
        ohlc.setLow(bar.getLow());
        ohlc.setClose(bar.getClose());

        // Set the OHLC and last price in the quote
        quote.setOhlc(ohlc);
        quote.setLastPrice(bar.getLastPrice()); // Set last price to close price

        // Log at debug level
        log.debug("createOHLCQuoteFromBar",
                String.format("Converted OHLC data point: time=%s, O=%.2f, H=%.2f, L=%.2f, C=%.2f",
                        bar.getTime(), bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose()));

        return quote;
    }

    /**
     * Get quotes for a list of symbols with timeframe support
     * 
     * @param symbols       List of trading symbols
     * @param isIndexSymbol Whether the symbols are index symbols
     * @param timeFrame     The timeframe for the quotes
     * @param forceRefresh  Whether to force refresh from provider
     * @return Map containing quotes or error information
     */
    public Map<String, Object> getQuotes(Set<String> symbols, boolean isIndexSymbol, TimeFrame timeFrame,
            boolean forceRefresh) {
        try {
            // Log the request
            log.info("getQuotes", String.format("Getting quotes for %d symbols with timeFrame: %s, forceRefresh: %s",
                    symbols.size(), timeFrame.getApiValue(), forceRefresh));

            // Convert Set<String> to List<String>
            List<String> symbolList = new ArrayList<>(symbols);

            // Try to get data from cache first if not forcing refresh
            if (!forceRefresh) {
                Map<String, OHLCQuote> cachedData = getOHLCFromCache(symbolList, timeFrame);
                if (!cachedData.isEmpty()) {
                    log.info("getQuotes", String.format("Retrieved quotes from cache for %d symbols with timeFrame: %s",
                            cachedData.size(), timeFrame.getApiValue()));

                    // Format the response
                    Map<String, Object> response = new HashMap<>();
                    response.put("quotes", cachedData);
                    response.put("source", "cache");
                    return response;
                }
            }

            // If we get here, we need to fetch from the provider
            log.info("getQuotes", String.format("Fetching quotes from provider for %d symbols with timeFrame: %s",
                    symbols.size(), timeFrame.getApiValue()));

            // Call the MarketDataService to get quotes from provider
            MarketDataService marketDataService = ApplicationContextProvider.getBean(MarketDataService.class);
            Map<String, OHLCQuote> providerData = marketDataService.getOHLC(symbolList, timeFrame, true, null);

            if (providerData.isEmpty()) {
                log.warn("getQuotes",
                        "No quotes data returned from provider for timeFrame: " + timeFrame.getApiValue());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("ERROR", Map.of(
                        "error", "NO_DATA",
                        "message", "No quotes data available for the requested symbols and timeframe"));
                return errorResponse;
            }

            // Cache the data for future use
            cacheOHLCData(providerData, timeFrame);

            // Format the response
            Map<String, Object> response = new HashMap<>();
            response.put("quotes", providerData);
            response.put("source", "provider");

            return response;
        } catch (Exception e) {
            // Log the error
            CacheLoggingUtil.logCacheException(log, "GET_QUOTES", String.join(", ", symbols),
                    "Error retrieving quotes", e);

            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("ERROR", Map.of(
                    "error", "PROVIDER_ERROR",
                    "message", e.getMessage()));
            return errorResponse;
        }
    }

    public void setActiveProvider(String providerName) {
        stockCacheService.setActiveProvider(providerName);
    }

    public String getActiveProvider() {
        return stockCacheService.getActiveProvider();
    }

    /**
     * Cache the latest streaming prices strictly in Redis with a 6-hour TTL.
     * Normalized format is used.
     */
    public void cacheLatestPrices(Map<String, OHLCQuote> quotes) {
        if (quotes == null || quotes.isEmpty()) {
            return;
        }

        try {
            for (Map.Entry<String, OHLCQuote> entry : quotes.entrySet()) {
                String rawSymbol = entry.getKey();
                // Normalize symbol: e.g. NSE_INDEX|Nifty 50 -> NIFTY 50
                String symbol = rawSymbol;
                if (symbol.contains("|")) {
                    symbol = symbol.substring(symbol.indexOf("|") + 1);
                }
                if (symbol.contains(":")) {
                    symbol = symbol.substring(symbol.indexOf(":") + 1);
                }
                symbol = symbol.toUpperCase().trim();

                OHLCQuote quote = entry.getValue();
                if (quote == null) {
                    continue;
                }

                double lastPrice = quote.getLastPrice();
                double open = quote.getOhlc() != null ? quote.getOhlc().getOpen() : 0.0;
                double high = quote.getOhlc() != null ? quote.getOhlc().getHigh() : 0.0;
                double low = quote.getOhlc() != null ? quote.getOhlc().getLow() : 0.0;
                double previousClose = quote.getPreviousClose();
                double change = lastPrice - previousClose;
                double changePercent = previousClose != 0 ? (change / previousClose) * 100.0 : 0.0;

                Map<String, Object> cacheData = new HashMap<>();
                cacheData.put("symbol", symbol);
                cacheData.put("lastPrice", lastPrice);
                cacheData.put("open", open);
                cacheData.put("high", high);
                cacheData.put("low", low);
                cacheData.put("previousClose", previousClose);
                cacheData.put("change", change);
                cacheData.put("changePercent", changePercent);
                cacheData.put("updatedAt", System.currentTimeMillis());
                cacheData.put("source", "UPSTOX_WS");

                String json = objectMapper.writeValueAsString(cacheData);
                String key = "market:latest-price:" + symbol;

                // 6 hours TTL during market day fallback
                redisTemplate.opsForValue().set(key, json, 6, TimeUnit.HOURS);
            }
        } catch (Exception e) {
            log.error("cacheLatestPrices", "Error caching latest prices to Redis", e);
        }
    }

    /**
     * Retrieve the latest streaming prices strictly from Redis.
     */
    public Map<String, OHLCQuote> getLatestPrices(Set<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, OHLCQuote> result = new HashMap<>();
        try {
            for (String symbol : symbols) {
                String cleanSymbol = symbol;
                if (cleanSymbol.contains("|")) {
                    cleanSymbol = cleanSymbol.substring(cleanSymbol.indexOf("|") + 1);
                }
                if (cleanSymbol.contains(":")) {
                    cleanSymbol = cleanSymbol.substring(cleanSymbol.indexOf(":") + 1);
                }
                cleanSymbol = cleanSymbol.toUpperCase().trim();

                String key = "market:latest-price:" + cleanSymbol;
                String json = redisTemplate.opsForValue().get(key);
                if (json != null) {
                    Map<String, Object> map = objectMapper.readValue(json, Map.class);

                    // Ignore Redis prices older than 15 minutes (900,000 ms)
                    long updatedAt = ((Number) map.getOrDefault("updatedAt", 0L)).longValue();
                    long ageMs = System.currentTimeMillis() - updatedAt;
                    if (ageMs > 15 * 60 * 1000) {
                        log.warn("getLatestPrices", "Redis price for " + cleanSymbol + " is stale (age: " + (ageMs / 1000) + "s). Ignoring.");
                        continue;
                    }

                    double lastPrice = ((Number) map.getOrDefault("lastPrice", 0.0)).doubleValue();
                    double open = ((Number) map.getOrDefault("open", 0.0)).doubleValue();
                    double high = ((Number) map.getOrDefault("high", 0.0)).doubleValue();
                    double low = ((Number) map.getOrDefault("low", 0.0)).doubleValue();
                    double previousClose = ((Number) map.getOrDefault("previousClose", 0.0)).doubleValue();

                    OHLCQuote.OHLC ohlc = new OHLCQuote.OHLC();
                    ohlc.setOpen(open);
                    ohlc.setHigh(high);
                    ohlc.setLow(low);
                    ohlc.setClose(lastPrice);

                    OHLCQuote quote = new OHLCQuote();
                    quote.setLastPrice(lastPrice);
                    quote.setOhlc(ohlc);
                    quote.setPreviousClose(previousClose);

                    result.put(symbol, quote);
                }
            }
        } catch (Exception e) {
            log.error("getLatestPrices", "Error fetching latest prices from Redis", e);
        }
        return result;
    }
}
