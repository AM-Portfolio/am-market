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
import org.springframework.stereotype.Service;

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

    public MarketDataCacheService(StockCacheService stockCacheService, ObjectMapper objectMapper) {
        this.stockCacheService = stockCacheService;
        this.objectMapper = objectMapper;
    }

    public void cacheOHLCData(Map<String, OHLCQuote> ohlcData, TimeFrame timeFrame) {
        try {
            String interval = timeFrame != null ? timeFrame.getApiValue() : "1D";
            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

            log.info("[INTERVAL_TRACE]", String.format(
                    "MarketDataCacheService.cacheOHLCData: Caching %d symbols with timeFrame: %s (enum: %s, apiValue: %s) for date: %s",
                    ohlcData.size(), timeFrame, timeFrame != null ? timeFrame.name() : "null", interval, today));

            // Convert OHLC quotes to OHLCV objects and cache per symbol with timeframe
            List<String> cachedKeys = new ArrayList<>();
            for (Map.Entry<String, OHLCQuote> entry : ohlcData.entrySet()) {
                String fullSymbol = entry.getKey();
                // Remove all exchange prefixes (NSE_EQ:, NSE:, etc.)
                String symbol = fullSymbol.contains(":") ? fullSymbol.substring(fullSymbol.indexOf(":") + 1)
                        : fullSymbol;
                OHLCQuote quote = entry.getValue();

                // Create OHLCV from OHLCQuote
                OHLCV ohlcv = StockCacheService.createPricePoint(
                        LocalDateTime.now(),
                        quote.getOhlc().getOpen(),
                        quote.getOhlc().getHigh(),
                        quote.getOhlc().getClose(),
                        quote.getOhlc().getClose(),
                        0L,
                        quote.getLastPrice());

                // Cache as intraday (today's live market data)
                // Retrieval logic will determine prefix based on date
                List<OHLCV> bars = new ArrayList<>();
                bars.add(ohlcv);
                stockCacheService.cacheIntradayBars(symbol, interval, bars);

                // Collect Redis key
                String redisKey = String.format("stock:intraday:%s:%s:%s", symbol.toUpperCase(), interval, today);
                cachedKeys.add(symbol + " -> " + redisKey);
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
            // Clean symbols (remove NSE: prefix if present AND filter out index symbols)
            // Clean symbols (remove NSE: prefix if present)
            List<String> cleanSymbols = tradingSymbols.stream()
                    .map(symbol -> symbol.replace("NSE:", "").replace("NSE_EQ:", ""))
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
                    // Smart logging: if cache hits are huge (>1000), show only count in INFO and
                    // one sample in DEBUG
                    if (cacheHits.size() > 1000) {
                        log.info("getHistoricalDataFromCache",
                                String.format(
                                        "Retrieved %d historical data points from cache for symbol: %s (%d cache hits)",
                                        points.size(), symbol, cacheHits.size()));

                        // Show one sample record in DEBUG mode to know the pattern
                        if (!cacheHits.isEmpty()) {
                            Map.Entry<String, String> firstEntry = cacheHits.entrySet().iterator().next();
                            log.debug("getHistoricalDataFromCache",
                                    String.format("Sample cache hit: %s -> %s", firstEntry.getKey(),
                                            firstEntry.getValue()));
                        }
                    } else {
                        // Log all cache hits for smaller datasets
                        log.info("getHistoricalDataFromCache",
                                String.format(
                                        "Retrieved %d historical data points from cache for symbol: %s with values: %s",
                                        points.size(), symbol, cacheHits));
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
}
