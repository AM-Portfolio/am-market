package com.am.marketdata.service;

import com.am.common.investment.model.historical.HistoricalData;
import com.am.common.investment.model.historical.OHLCVTPoint;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.common.util.ApplicationContextProvider;
import com.am.marketdata.common.util.MarketDepthFreshness;
import com.am.marketdata.redis.model.OHLCV;
import com.am.marketdata.redis.model.StockBars;
import com.am.marketdata.redis.service.StockCacheService;
import com.am.marketdata.service.util.OfficialClosePolicy;
import com.am.marketdata.redis.util.CacheLoggingUtil;

import com.am.marketdata.common.log.AppLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.am.marketdata.service.model.PreviousCloseDocument;
import com.am.marketdata.service.repo.PreviousCloseRepository;
import com.am.marketdata.service.calendar.MarketCalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executor;
import java.util.concurrent.CompletableFuture;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    private static final int HISTORICAL_REDIS_PIPELINE_SYMBOL_BATCH_SIZE = 50;

    private final StockCacheService stockCacheService;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final PreviousCloseRepository previousCloseRepository;
    private final MarketHoursService marketHoursService;
    private final MarketCalendarService marketCalendarService;

    @Value("${market.data.depth.max.age.seconds:60}")
    private long marketDepthMaxAgeSeconds = 60;

    @Value("${market.data.max.age.minutes:15}")
    private long marketQuoteMaxAgeMinutes = 15;

    // Dedicated thread pool for non-blocking background cache backfilling
    @Autowired(required = false)
    @org.springframework.beans.factory.annotation.Qualifier("cacheBackfillExecutor")
    private Executor cacheBackfillExecutor;

    // Configurable toggle: set MARKET_CACHE_ASYNC_ENABLED=false to revert to synchronous backfill if needed
    @Value("${market.cache.async-backfill.enabled:true}")
    private boolean asyncBackfillEnabled = true;

    @Autowired
    public MarketDataCacheService(StockCacheService stockCacheService,
                                  ObjectMapper objectMapper,
                                  RedisTemplate<String, String> redisTemplate,
                                  @Autowired(required = false) PreviousCloseRepository previousCloseRepository,
                                  @Autowired(required = false) MarketHoursService marketHoursService,
                                  @Autowired(required = false) MarketCalendarService marketCalendarService) {
        this.stockCacheService = stockCacheService;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.previousCloseRepository = previousCloseRepository;
        this.marketHoursService = marketHoursService;
        this.marketCalendarService = marketCalendarService;
    }

    public MarketDataCacheService(StockCacheService stockCacheService,
                                  ObjectMapper objectMapper,
                                  RedisTemplate<String, String> redisTemplate,
                                  PreviousCloseRepository previousCloseRepository,
                                  MarketHoursService marketHoursService) {
        this(stockCacheService, objectMapper, redisTemplate, previousCloseRepository, marketHoursService, null);
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
        String exchange = symbol.contains(":") ? symbol.substring(0, symbol.indexOf(':'))
                : (symbol.contains("|") ? symbol.substring(0, symbol.indexOf('|')) : "NSE");
        LocalDate expectedQuoteDate = expectedQuoteSessionDate(exchange);
        try {
            List<String> closeMetadata = redisTemplate.opsForValue().multiGet(List.of(
                    "market:prev-close:" + normalizedSymbol,
                    "market:prev-close-date:" + normalizedSymbol));
            String closeValue = closeMetadata != null && !closeMetadata.isEmpty() ? closeMetadata.get(0) : null;
            String closeDateValue = closeMetadata != null && closeMetadata.size() > 1 ? closeMetadata.get(1) : null;
            if (closeValue != null && expectedQuoteDate != null && expectedQuoteDate.toString().equals(closeDateValue)) {
                double parsed = Double.parseDouble(closeValue);
                if (Double.isFinite(parsed) && parsed > 0.0) return parsed;
            }

            // Fall back to the dated quote snapshot when scalar metadata is absent.
            String snapshot = redisTemplate.opsForValue().get("market:latest-price:" + normalizedSymbol);
            if (snapshot != null) {
                Map<?, ?> values = objectMapper.readValue(snapshot, Map.class);
                Double previousClose = readPositiveNumber(values.get("previousClose"));
                LocalDate quoteDate = readDate(values.get("quoteTradeDate"));
                long updatedAt = readLong(values.get("updatedAt"));
                boolean sessionMatches = quoteDate != null && expectedQuoteDate != null && quoteDate.equals(expectedQuoteDate);
                boolean recentUndatedSnapshot = quoteDate == null && isRecent(updatedAt, marketQuoteMaxAgeMinutes);
                if (previousClose != null && (sessionMatches || recentUndatedSnapshot)) {
                    return previousClose;
                }
            }
        } catch (Exception e) {
            log.warn("getPreviousClose", "Could not read a dated Redis quote snapshot for symbol: " + symbol, e);
        }

        // 2. Fallback to MongoDB persistence
        if (previousCloseRepository != null) {
            try {
                Optional<PreviousCloseDocument> docOpt = previousCloseRepository.findById(normalizedSymbol);
                if (expectedQuoteDate != null && docOpt.isPresent() && docOpt.get().getPreviousClose() != null
                        && docOpt.get().getPreviousClose() > 0
                        && expectedQuoteDate.toString().equals(docOpt.get().getTradeDate())) {
                    Double dbValue = docOpt.get().getPreviousClose();
                    return dbValue;
                }
            } catch (Exception mongoEx) {
                log.warn("getPreviousClose", "Failed to get previous close from MongoDB for symbol: " + symbol, mongoEx);
            }
        }
        return null;
    }

    private LocalDate expectedQuoteSessionDate(String exchange) {
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        LocalDate today = LocalDate.now(zone);
        if (marketCalendarService == null) {
            return today;
        }
        try {
            // Walk calendar sessions, not weekdays: holidays and special closures matter.
            for (int daysBack = 0; daysBack <= 10; daysBack++) {
                LocalDate candidate = today.minusDays(daysBack);
                MarketCalendarService.SessionTiming timing = marketCalendarService.getTimings(exchange, candidate);
                if (timing != null && timing.open()) {
                    if (daysBack == 0 && timing.sessionStart() != null
                            && ZonedDateTime.now(zone).toLocalTime().isBefore(timing.sessionStart())) {
                        continue;
                    }
                    return candidate;
                }
            }
        } catch (Exception calendarException) {
            log.warn("expectedQuoteSessionDate", "Calendar lookup failed exchange={} date={}; rejecting dated close lookup", exchange, today);
            return null;
        }
        log.warn("expectedQuoteSessionDate", "No open session found exchange={} around date={}", exchange, today);
        return null;
    }

    private static Double readPositiveNumber(Object value) {
        if (value instanceof Number number && Double.isFinite(number.doubleValue()) && number.doubleValue() > 0.0) {
            return number.doubleValue();
        }
        return null;
    }

    private static LocalDate readDate(Object value) {
        try {
            return value instanceof String text && !text.isBlank() ? LocalDate.parse(text) : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static long readLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static boolean isRecent(long updatedAtMillis, long maxAgeMinutes) {
        if (updatedAtMillis <= 0 || maxAgeMinutes <= 0) return false;
        return isRecentMillis(updatedAtMillis, TimeUnit.MINUTES.toMillis(maxAgeMinutes));
    }

    private static boolean isRecentMillis(long timestampMillis, long maxAgeMillis) {
        if (timestampMillis <= 0 || maxAgeMillis <= 0) return false;
        long ageMillis = System.currentTimeMillis() - timestampMillis;
        return ageMillis >= 0 && ageMillis <= maxAgeMillis;
    }

    public void setPreviousClose(String symbol, double previousClose) {
        if (previousClose <= 0) {
            return;
        }
        String normalizedSymbol = normalizeSymbol(symbol);
        if (normalizedSymbol == null) {
            return;
        }
        String exchange = symbol.contains(":") ? symbol.substring(0, symbol.indexOf(':'))
                : (symbol.contains("|") ? symbol.substring(0, symbol.indexOf('|')) : "NSE");
        LocalDate quoteSessionDate = expectedQuoteSessionDate(exchange);
        if (quoteSessionDate == null) {
            log.warn("setPreviousClose", "Skipping previous-close cache write without a verified session date symbol={} exchange={}",
                    symbol, exchange);
            return;
        }
        // 1. Cache to Redis
        try {
            String redisKey = "market:prev-close:" + normalizedSymbol;
            redisTemplate.opsForValue().set(redisKey, String.valueOf(previousClose), 26, TimeUnit.HOURS);
            redisTemplate.opsForValue().set("market:prev-close-date:" + normalizedSymbol,
                    quoteSessionDate.toString(), 26, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("setPreviousClose", "Failed to set previous close in Redis for symbol: " + symbol, e);
        }

        // 2. Upsert to MongoDB (single record per symbol)
        if (previousCloseRepository != null) {
            try {
                PreviousCloseDocument doc = PreviousCloseDocument.builder()
                        .symbol(normalizedSymbol)
                        .previousClose(previousClose)
                        .tradeDate(quoteSessionDate.toString())
                        .updatedAt(Instant.now())
                        .build();
                previousCloseRepository.save(doc);
            } catch (Exception mongoEx) {
                log.warn("setPreviousClose", "Failed to upsert previous close in MongoDB for symbol: " + symbol, mongoEx);
            }
        }
    }

    /**
     * Cache OHLC data to Redis and MongoDB.
     * 
     * PERFORMANCE FIX:
     * When asyncBackfillEnabled is true, cache writing is offloaded to the dedicated
     * 'cacheBackfillExecutor' thread pool. This allows the HTTP response thread to return
     * the fresh provider response to the client immediately (<400ms) without waiting for
     * Redis write operations to complete.
     */
    public void cacheOHLCData(Map<String, OHLCQuote> ohlcData, TimeFrame timeFrame) {
        if (ohlcData == null || ohlcData.isEmpty()) {
            return;
        }
        if (asyncBackfillEnabled && cacheBackfillExecutor != null) {
            CompletableFuture.runAsync(() -> doCacheOHLCDataInternal(ohlcData, timeFrame), cacheBackfillExecutor)
                    .exceptionally(ex -> {
                        log.warn("cacheOHLCData", "Background async cache backfill encountered an exception: {}", ex.getMessage());
                        return null;
                    });
        } else {
            doCacheOHLCDataInternal(ohlcData, timeFrame);
        }
    }

    private void doCacheOHLCDataInternal(Map<String, OHLCQuote> ohlcData, TimeFrame timeFrame) {
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

            // Smart logging: if keys are huge (>1000), show only count in INFO and one sample in DEBUG
            if (cachedKeys.size() > 1000) {
                log.info("cacheOHLCData", "Cached {} symbols with timeframe: {} for date: {} ({} key-value pairs)",
                        ohlcData.size(), interval, today, cachedKeys.size());

                if (!cachedKeys.isEmpty()) {
                    log.debug("cacheOHLCData", "Sample key pattern: {}", cachedKeys.get(0));
                }
            } else {
                List<String> sampleKeys = cachedKeys.subList(0, Math.min(3, cachedKeys.size()));
                log.info("cacheOHLCData", "Cached {} symbols with timeframe: {} for date: {}. Sample keys: {}",
                        ohlcData.size(), interval, today, sampleKeys);
            }

            // Also persist lastPrice + previousClose to market:latest-price:*
            try {
                cacheLatestPrices(ohlcData);
                log.debug("cacheOHLCData", "Also cached latest prices (incl. previousClose) for {} symbols", ohlcData.size());
            } catch (Exception latestEx) {
                log.warn("cacheOHLCData", "Failed to cache latest prices alongside OHLC data: {}", latestEx.getMessage());
            }
        } catch (Exception e) {
            CacheLoggingUtil.logCacheException(log, "CACHE_OHLC", null, "Error caching OHLC data", e);
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

    /**
     * Cache a fetched historical response for many symbols using one existing
     * Redis pipeline. This is best-effort, like the single-symbol method above:
     * invalid/empty members are skipped and a cache failure does not fail the
     * market-data response.
     */
    public void cacheHistoricalDataBatch(Map<String, HistoricalData> historicalDataBySymbol, TimeFrame timeFrame) {
        if (historicalDataBySymbol == null || historicalDataBySymbol.isEmpty() || timeFrame == null) {
            return;
        }

        // Preserve the existing intraday key/date behavior; this optimization is
        // for daily-or-larger candles used by index baseline/end calculations.
        if (timeFrame != TimeFrame.DAY && timeFrame != TimeFrame.WEEK
                && timeFrame != TimeFrame.MONTH && timeFrame != TimeFrame.YEAR) {
            historicalDataBySymbol.forEach((symbol, history) -> cacheHistoricalData(symbol, timeFrame, history));
            return;
        }

        long startedAt = System.nanoTime();
        List<StockBars> batch = new ArrayList<>(historicalDataBySymbol.size());
        int skipped = 0;
        for (Map.Entry<String, HistoricalData> entry : historicalDataBySymbol.entrySet()) {
            String symbol = entry.getKey();
            HistoricalData historicalData = entry.getValue();
            if (symbol == null || symbol.isBlank() || historicalData == null
                    || historicalData.getDataPoints() == null || historicalData.getDataPoints().isEmpty()) {
                skipped++;
                continue;
            }

            try {
                List<OHLCV> bars = historicalData.getDataPoints().stream()
                        .filter(point -> point != null && point.getTime() != null)
                        .map(point -> StockCacheService.createPricePoint(point.getTime(), point.getOpen(),
                                point.getHigh(), point.getLow(), point.getClose(), point.getVolume(), null))
                        .collect(Collectors.toList());
                if (bars.isEmpty()) {
                    skipped++;
                    continue;
                }

                batch.add(StockBars.builder()
                        .symbol(symbol)
                        .interval(timeFrame.getApiValue())
                        .startDate(bars.get(0).getTime().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                        .bars(bars)
                        .build());
            } catch (Exception e) {
                skipped++;
                log.warn("cacheHistoricalDataBatch", "Skipping invalid history member {}: {}", symbol,
                        e.getMessage());
            }
        }

        if (batch.isEmpty()) {
            log.info("cacheHistoricalDataBatch", "No valid members to cache (received={}, skipped={})",
                    historicalDataBySymbol.size(), skipped);
            return;
        }

        int pipelineBatches = 0;
        int successfulBatches = 0;
        for (int offset = 0; offset < batch.size(); offset += HISTORICAL_REDIS_PIPELINE_SYMBOL_BATCH_SIZE) {
            List<StockBars> pipelineBatch = batch.subList(offset,
                    Math.min(offset + HISTORICAL_REDIS_PIPELINE_SYMBOL_BATCH_SIZE, batch.size()));
            pipelineBatches++;
            try {
                boolean cached;
                cached = stockCacheService.cacheHistoricalDataBatch(pipelineBatch);
                if (cached) {
                    successfulBatches++;
                }
            } catch (Exception e) {
                // Keep the cache best-effort and continue with later bounded chunks.
                log.warn("cacheHistoricalDataBatch", "Redis pipeline batch failed (offset={}, symbols={}): {}",
                        offset, pipelineBatch.size(), e.getMessage());
            }
        }
        log.info("cacheHistoricalDataBatch",
                "Historical cache batch completed (timeframe={}, received={}, queued={}, skipped={}, pipelineBatches={}, successfulBatches={}, durationMs={})",
                timeFrame.getApiValue(), historicalDataBySymbol.size(), batch.size(), skipped, pipelineBatches,
                successfulBatches,
                (System.nanoTime() - startedAt) / 1_000_000);
    }

    public Map<String, OHLCQuote> getOHLCFromCache(List<String> tradingSymbols, TimeFrame timeFrame) {
        if (tradingSymbols == null || tradingSymbols.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            // Clean symbols (remove NSE_EQ:, NSE:, BSE_EQ:, BSE: exchange prefixes to match Redis keys)
            List<String> cleanSymbols = tradingSymbols.stream()
                    .map(symbol -> {
                        String clean = symbol != null ? symbol.trim() : "";
                        if (clean.contains("|")) {
                            clean = clean.substring(clean.indexOf("|") + 1);
                        }
                        return clean.replaceAll("(?i)^(NSE_EQ:|NSE:|BSE_EQ:|BSE:)", "").trim().toUpperCase();
                    })
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());

            if (cleanSymbols.isEmpty()) {
                log.debug("getOHLCFromCache", "All symbols were empty or invalid, skipping cache lookup");
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

                    // Also alias under requested original symbol formats (e.g. NSE_EQ:RELIANCE -> quote)
                    for (String orig : tradingSymbols) {
                        String cleanOrig = orig.replaceAll("(?i)^(NSE_EQ:|NSE:|BSE_EQ:|BSE:)", "").trim().toUpperCase();
                        if (cleanOrig.equalsIgnoreCase(symbol)) {
                            result.put(orig, quote);
                        }
                    }

                    // Record the cache hit for logging
                    cacheHits.put(symbol, String.format("O:%.2f,H:%.2f,L:%.2f,C:%.2f",
                            latestBar.getOpen(), latestBar.getHigh(), latestBar.getLow(), latestBar.getClose()));
                }
            }

            if (!result.isEmpty()) {
                // Log the cache hits with values
                log.info("getOHLCFromCache", "Retrieved OHLC data from cache for {} symbols", result.size());
                log.debug("getOHLCFromCache", "Retrieved values: {}", cacheHits);

                // Note: overlayLatestPrices is now called once at the orchestrator layer (MarketDataService) 
                // to avoid duplicate Redis MGET queries on cache hits while ensuring all paths are covered.
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
     * trading is not broken by a calendar outage.
     */
    public void overlayLatestPrices(Map<String, OHLCQuote> result) {
        if (result == null || result.isEmpty()) {
            return;
        }

        final boolean overlayLiveLastPrice = OfficialClosePolicy.shouldOverlayLiveLastPrice(
                isLiveTickOverlayAllowed());

        // OPTIMIZATION: Combine individual Redis GET calls into 1 single Redis MGET (multiGet) call.
        // Support exchange-namespaced keys (e.g. market:latest-price:BSE:RELIANCE or market:latest-price:RELIANCE).
        List<Map.Entry<String, OHLCQuote>> entries = new ArrayList<>(result.entrySet());
        List<String> primaryKeys = new ArrayList<>(entries.size());
        List<String> fallbackKeys = new ArrayList<>(entries.size());
        List<String> exchanges = new ArrayList<>(entries.size());

        for (Map.Entry<String, OHLCQuote> entry : entries) {
            String raw = entry.getKey();
            String exchange = "NSE";
            String cleanSymbol = raw;

            if (cleanSymbol.contains("|")) {
                String[] parts = cleanSymbol.split("\\|", 2);
                exchange = parts[0];
                cleanSymbol = parts[1];
            } else if (cleanSymbol.contains(":")) {
                String[] parts = cleanSymbol.split(":", 2);
                exchange = parts[0];
                cleanSymbol = parts[1];
            }
            cleanSymbol = cleanSymbol.toUpperCase().trim();
            exchange = exchange.toUpperCase().trim();
            exchanges.add(exchange);

            // Primary: market:latest-price:EXCHANGE:SYMBOL
            primaryKeys.add("market:latest-price:" + exchange + ":" + cleanSymbol);
            // Fallback: market:latest-price:SYMBOL (for backward compatibility)
            fallbackKeys.add("market:latest-price:" + cleanSymbol);
        }

        try {
            // Fetch qualified and legacy quote snapshots in one batch. The legacy scalar
            // previous-close key is excluded because it has no date and can be stale.
            List<String> combinedKeys = new ArrayList<>(primaryKeys.size() * 2);
            combinedKeys.addAll(primaryKeys);
            combinedKeys.addAll(fallbackKeys);

            List<String> allValues = redisTemplate.opsForValue().multiGet(combinedKeys);
            
            int n = entries.size();
            List<String> jsonList = (allValues != null && allValues.size() >= n) ? allValues.subList(0, n) : null;
            List<String> fallbackJsonList = (allValues != null && allValues.size() >= 2 * n) ? allValues.subList(n, 2 * n) : null;
            if (jsonList != null || fallbackJsonList != null) {
                for (int i = 0; i < entries.size(); i++) {
                    OHLCQuote quote = entries.get(i).getValue();
                    if (quote == null) continue;

                    // Populate only a quote snapshot that belongs to the expected session.
                    String json = (jsonList != null && i < jsonList.size()) ? jsonList.get(i) : null;
                    if (json == null && fallbackJsonList != null && i < fallbackJsonList.size()) {
                        json = fallbackJsonList.get(i);
                    }
                    if (json != null) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> latestData = objectMapper.readValue(json, Map.class);
                            double latestPrice = ((Number) latestData.getOrDefault("lastPrice", 0.0)).doubleValue();
                            double prevClose = ((Number) latestData.getOrDefault("previousClose", 0.0)).doubleValue();
                            LocalDate quoteDate = readDate(latestData.get("quoteTradeDate"));
                            long updatedAt = readLong(latestData.get("updatedAt"));
                            LocalDate expectedDate = expectedQuoteSessionDate(exchanges.get(i));
                            boolean sessionMatches = quoteDate != null && quoteDate.equals(expectedDate);
                            boolean isMarketOpen = isLiveTickOverlayAllowed();
                            boolean recentUndatedSnapshot = quoteDate == null && (!isMarketOpen || isRecent(updatedAt, marketQuoteMaxAgeMinutes));
                            if (!sessionMatches && !recentUndatedSnapshot) {
                                log.info("overlayLatestPrices", "Ignoring stale quote snapshot symbol={} quoteDate={} expectedDate={}",
                                        entries.get(i).getKey(), quoteDate, expectedDate);
                                continue;
                            }

                            // The latest-price record is written from the same quote
                            // snapshot and is newer than the legacy scalar prev-close key.
                            if (prevClose > 0 && Double.isFinite(prevClose)) {
                                quote.setPreviousClose(prevClose);
                            }
                            Object change = latestData.get("change");
                            if (change instanceof Number number && Double.isFinite(number.doubleValue())) {
                                quote.setChange(number.doubleValue());
                            }
                            Object changePercent = latestData.get("changePercent");
                            if (changePercent instanceof Number number && Double.isFinite(number.doubleValue())) {
                                quote.setChangePercent(number.doubleValue());
                            }
                            Object volume = latestData.get("volume");
                            if (volume instanceof Number number && number.longValue() >= 0) {
                                quote.setVolume(number.longValue());
                            }
                            Object oi = latestData.get("oi");
                            if (oi instanceof Number number && number.longValue() >= 0) {
                                quote.setOi(number.longValue());
                            }
                            Object oiDayHigh = latestData.get("oiDayHigh");
                            if (oiDayHigh instanceof Number number && number.longValue() >= 0) {
                                quote.setOiDayHigh(number.longValue());
                            }
                            Object oiDayLow = latestData.get("oiDayLow");
                            if (oiDayLow instanceof Number number && number.longValue() >= 0) {
                                quote.setOiDayLow(number.longValue());
                            }
                            Object averagePrice = latestData.get("averagePrice");
                            if (averagePrice instanceof Number number && Double.isFinite(number.doubleValue())) {
                                quote.setAveragePrice(number.doubleValue());
                            }
                            Object totalBuyQuantity = latestData.get("totalBuyQuantity");
                            if (totalBuyQuantity instanceof Number number && Double.isFinite(number.doubleValue())) {
                                quote.setTotalBuyQuantity(number.doubleValue());
                            }
                            Object totalSellQuantity = latestData.get("totalSellQuantity");
                            if (totalSellQuantity instanceof Number number && Double.isFinite(number.doubleValue())) {
                                quote.setTotalSellQuantity(number.doubleValue());
                            }
                            Object lowerCircuitLimit = latestData.get("lowerCircuitLimit");
                            if (lowerCircuitLimit instanceof Number number && Double.isFinite(number.doubleValue())) {
                                quote.setLowerCircuitLimit(number.doubleValue());
                            }
                            Object upperCircuitLimit = latestData.get("upperCircuitLimit");
                            if (upperCircuitLimit instanceof Number number && Double.isFinite(number.doubleValue())) {
                                quote.setUpperCircuitLimit(number.doubleValue());
                            }
                            Object depth = latestData.get("marketDepth");
                            Instant quoteInstant = null;
                            Object quoteTimestamp = latestData.get("quoteTimestamp");
                            if (quoteTimestamp instanceof String timestamp && !timestamp.isBlank()) {
                                try { quoteInstant = Instant.parse(timestamp); } catch (RuntimeException ignored) { }
                            }
                            if (depth instanceof Map<?, ?>) {
                                quote.setMarketDepth(objectMapper.convertValue(depth,
                                        com.am.marketdata.common.model.QuoteDepth.class));
                            }
                            if (quoteInstant != null) {
                                quote.setQuoteTimestamp(quoteInstant);
                            }
                            if (quoteDate != null) {
                                quote.setQuoteTradeDate(quoteDate);
                            }
                            if (quote.getMarketDepth() != null && !isFreshMarketDepth(quote, entries.get(i).getKey())) {
                                quote.setMarketDepth(null);
                            }
                            if (latestPrice > 0) {
                                if (overlayLiveLastPrice || quote.getLastPrice() == 0.0) {
                                    quote.setLastPrice(latestPrice);
                                }
                            }
                            // Populate OHLC from latest-price Redis JSON if missing or zeroes
                            double openVal = ((Number) latestData.getOrDefault("open", 0.0)).doubleValue();
                            double highVal = ((Number) latestData.getOrDefault("high", 0.0)).doubleValue();
                            double lowVal = ((Number) latestData.getOrDefault("low", 0.0)).doubleValue();
                            double closeVal = latestPrice > 0 ? latestPrice : ((Number) latestData.getOrDefault("close", 0.0)).doubleValue();
                            if (openVal > 0 || highVal > 0 || lowVal > 0 || closeVal > 0) {
                                if (quote.getOhlc() == null || quote.getOhlc().getClose() == 0.0) {
                                    OHLCQuote.OHLC ohlcObj = quote.getOhlc() != null ? quote.getOhlc() : new OHLCQuote.OHLC();
                                    if (openVal > 0) ohlcObj.setOpen(openVal);
                                    if (highVal > 0) ohlcObj.setHigh(highVal);
                                    if (lowVal > 0) ohlcObj.setLow(lowVal);
                                    if (closeVal > 0) ohlcObj.setClose(closeVal);
                                    quote.setOhlc(ohlcObj);
                                }
                            }
                        } catch (Exception parseEx) {
                            log.warn("overlayLatestPrices", "Failed to parse Redis latest-price JSON for symbol {}: {}",
                                    entries.get(i).getKey(), parseEx.getMessage());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("overlayLatestPrices", "Batch overlay failed for {} symbols: {}", primaryKeys.size(), ex.getMessage(), ex);
        }

        // Mongo snapshots are useful only when they match the expected quote session.
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
                        Map<String, PreviousCloseDocument> dbCloseMap = mongoDocs.stream()
                                .filter(d -> d.getPreviousClose() != null && d.getPreviousClose() > 0)
                                .collect(Collectors.toMap(PreviousCloseDocument::getSymbol, d -> d, (v1, v2) -> v1));

                        for (Map.Entry<String, OHLCQuote> entry : result.entrySet()) {
                            OHLCQuote quote = entry.getValue();
                            if (quote != null && quote.getPreviousClose() == 0.0) {
                                String norm = normalizeSymbol(entry.getKey());
                                PreviousCloseDocument document = dbCloseMap.get(norm);
                                String rawSymbol = entry.getKey();
                                String exchange = rawSymbol.contains(":") ? rawSymbol.substring(0, rawSymbol.indexOf(':'))
                                        : (rawSymbol.contains("|") ? rawSymbol.substring(0, rawSymbol.indexOf('|')) : "NSE");
                                LocalDate expectedDate = expectedQuoteSessionDate(exchange);
                                if (document != null && expectedDate != null
                                        && expectedDate.toString().equals(document.getTradeDate())) {
                                    quote.setPreviousClose(document.getPreviousClose());
                                }
                            }
                        }
                    }
                } catch (Exception mongoEx) {
                    log.warn("overlayLatestPrices", "Failed to fetch previousClose from MongoDB fallback: " + mongoEx.getMessage());
                }
            }

            // Meaningful Comment: Self-healing fallback. If any symbols still have previousClose = 0.0 after MongoDB lookup (e.g. they are missing in Nifty 500
            // and the scheduler never synced them), fetch them from the Historical API dynamically and cache them immediately.
            List<String> finalMissingSymbols = new java.util.ArrayList<>();
            for (Map.Entry<String, OHLCQuote> entry : result.entrySet()) {
                if (entry.getValue() != null && entry.getValue().getPreviousClose() == 0.0) {
                    finalMissingSymbols.add(entry.getKey());
                }
            }

            if (!finalMissingSymbols.isEmpty()) {
                if (cacheBackfillExecutor != null) {
                    CompletableFuture.runAsync(() -> {
                        try {
                            com.marketdata.common.MarketDataProvider provider = com.am.marketdata.common.util.ApplicationContextProvider.getBean("upstox", com.marketdata.common.MarketDataProvider.class);
                            if (provider != null) {
                                log.info("overlayLatestPrices", "Self-healing (async): Triggering historical fallback sync for " + finalMissingSymbols.size() + " symbols: " + finalMissingSymbols);
                                Map<String, OHLCQuote> freshQuotes = provider.getOHLC(finalMissingSymbols, TimeFrame.DAY);
                                if (freshQuotes != null && !freshQuotes.isEmpty()) {
                                    // Cache backfill the fresh quotes so subsequent reads have previousClose populated
                                    cacheOHLCData(freshQuotes, TimeFrame.DAY);
                                }
                            }
                        } catch (Exception selfHealEx) {
                            log.warn("overlayLatestPrices", "Failed async self-healing previousClose fallback: " + selfHealEx.getMessage());
                        }
                    }, cacheBackfillExecutor);
                } else {
                    log.debug("overlayLatestPrices", "cacheBackfillExecutor null; skipping blocking self-healing fallback for " + finalMissingSymbols.size() + " symbols");
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
                    // The bar cache holds historical OHLC only. Overlay the latest Redis quote
                    // snapshot so fresh price details and order-book depth reach API consumers.
                    overlayLatestPrices(cachedData);
                    log.info("getQuotes", String.format("Retrieved quotes from cache for %d symbols with timeFrame: %s",
                            cachedData.size(), timeFrame.getApiValue()));
                    Map<String, Object> response = new HashMap<>();
                    response.put("quotes", cachedData);
                    response.put("source", "cache");
                    return response;
                }
            }

            log.info("getQuotes", String.format("Fetching quotes from provider for %d symbols with timeFrame: %s",
                    symbols.size(), timeFrame.getApiValue()));
            MarketDataService marketDataService = ApplicationContextProvider.getBean(MarketDataService.class);
            Map<String, OHLCQuote> providerData = marketDataService.getOHLC(symbolList, timeFrame, true, null);

            if (providerData == null || providerData.isEmpty()) {
                log.warn("getQuotes",
                        "No quotes data returned from provider for timeFrame: " + timeFrame.getApiValue());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("ERROR", Map.of(
                        "error", "NO_DATA",
                        "message", "No quotes data available for the requested symbols and timeframe"));
                return errorResponse;
            }

            cacheOHLCData(providerData, timeFrame);

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

    private boolean isFreshMarketDepth(OHLCQuote quote, String symbol) {
        if (quote == null || quote.getMarketDepth() == null || quote.getQuoteTimestamp() == null
                || marketDepthMaxAgeSeconds <= 0) return false;
        // Market closure alone does not make a recent exchange snapshot stale.
        // The timestamp check prevents an old order book from being shown as current.
        long maxAgeMillis = TimeUnit.SECONDS.toMillis(marketDepthMaxAgeSeconds);
        boolean fresh = MarketDepthFreshness.isFresh(quote.getQuoteTimestamp(), maxAgeMillis);
        if (!fresh) {
            log.debug("getQuotes", "Omitted market depth because its quote timestamp is stale symbol={}", symbol);
        }
        return fresh;
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
            // Step 1: Pre-normalize all symbols and build lookup keys to check Redis
            List<String> normalizedSymbols = new ArrayList<>();
            List<String> redisPrevCloseKeys = new ArrayList<>();
            List<String> redisPrevCloseDateKeys = new ArrayList<>();
            Map<String, String> normalizedToRaw = new HashMap<>();

            for (String rawSymbol : quotes.keySet()) {
                String symbol = rawSymbol;
                if (symbol.contains("|")) {
                    symbol = symbol.substring(symbol.indexOf("|") + 1);
                }
                if (symbol.contains(":")) {
                    symbol = symbol.substring(symbol.indexOf(":") + 1);
                }
                symbol = symbol.toUpperCase().trim();
                normalizedSymbols.add(symbol);
                redisPrevCloseKeys.add("market:prev-close:" + symbol);
                redisPrevCloseDateKeys.add("market:prev-close-date:" + symbol);
                normalizedToRaw.put(symbol, rawSymbol);
            }

            // Step 2: Bulk multiGet to check which prev-close keys are already in Redis
            List<String> existingCloseMetadata = Collections.emptyList();
            List<String> closeLookupKeys = new ArrayList<>(redisPrevCloseKeys.size() * 2);
            closeLookupKeys.addAll(redisPrevCloseKeys);
            closeLookupKeys.addAll(redisPrevCloseDateKeys);
            try {
                existingCloseMetadata = redisTemplate.opsForValue().multiGet(closeLookupKeys);
            } catch (Exception redisCheckEx) {
                log.warn("cacheLatestPrices", "Failed to check previous-close cache metadata, refreshing dated values", redisCheckEx);
            }

            // Map each symbol to whether it already has a previous close cached
            Map<String, String> existingPrevCloseBySymbol = new HashMap<>();
            Map<String, String> existingPrevCloseDateBySymbol = new HashMap<>();
            for (int i = 0; i < normalizedSymbols.size(); i++) {
                String symbol = normalizedSymbols.get(i);
                String existingValue = existingCloseMetadata != null && i < existingCloseMetadata.size()
                        ? existingCloseMetadata.get(i) : null;
                existingPrevCloseBySymbol.put(symbol, existingValue);
                int dateIndex = normalizedSymbols.size() + i;
                String existingDate = existingCloseMetadata != null && dateIndex < existingCloseMetadata.size()
                        ? existingCloseMetadata.get(dateIndex) : null;
                existingPrevCloseDateBySymbol.put(symbol, existingDate);
            }

            // Step 3: Accumulate Redis pipeline commands and MongoDB documents
            Map<String, String> latestPriceRedisWrites = new HashMap<>();
            Map<String, String> prevCloseRedisWrites = new HashMap<>();
            List<PreviousCloseDocument> mongoPrevCloseDocs = new ArrayList<>();

            for (String symbol : normalizedSymbols) {
                String rawSymbol = normalizedToRaw.get(symbol);
                OHLCQuote quote = quotes.get(rawSymbol);
                if (quote == null || quote.getLastPrice() <= 0.0) {
                    continue;
                }

                double lastPrice = quote.getLastPrice();
                double open = quote.getOhlc() != null ? quote.getOhlc().getOpen() : 0.0;
                double high = quote.getOhlc() != null ? quote.getOhlc().getHigh() : 0.0;
                double low = quote.getOhlc() != null ? quote.getOhlc().getLow() : 0.0;
                double previousClose = quote.getPreviousClose();
                LocalDate quoteTradeDate = quote.getQuoteTradeDate();
                if (quoteTradeDate == null && quote.getQuoteTimestamp() != null) {
                    quoteTradeDate = quote.getQuoteTimestamp().atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();
                }
                if (quoteTradeDate == null && isLiveTickOverlayAllowed()) {
                    quoteTradeDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
                }

                // Refresh a changed previous close. The old scalar key has no date, so
                // leaving yesterday's value in place can override a newer quote snapshot.
                String existingPrevClose = existingPrevCloseBySymbol.get(symbol);
                boolean previousCloseChanged = existingPrevClose == null;
                if (existingPrevClose != null) {
                    try {
                        previousCloseChanged = Math.abs(Double.parseDouble(existingPrevClose) - previousClose) >= 0.005;
                    } catch (NumberFormatException ignored) {
                        previousCloseChanged = true;
                    }
                }
                boolean previousCloseSessionChanged = quoteTradeDate != null
                        && !quoteTradeDate.toString().equals(existingPrevCloseDateBySymbol.get(symbol));
                if (previousClose > 0.0 && (previousCloseChanged || previousCloseSessionChanged)) {
                    prevCloseRedisWrites.put("market:prev-close:" + symbol, String.valueOf(previousClose));
                    if (quoteTradeDate != null) {
                        prevCloseRedisWrites.put("market:prev-close-date:" + symbol, quoteTradeDate.toString());
                    }

                    if (previousCloseRepository != null && quoteTradeDate != null) {
                        PreviousCloseDocument doc = PreviousCloseDocument.builder()
                                .symbol(symbol)
                                .previousClose(previousClose)
                                .tradeDate(quoteTradeDate.toString())
                                .updatedAt(Instant.now())
                                .build();
                        mongoPrevCloseDocs.add(doc);
                    }
                }

                Map<String, Object> cacheData = new HashMap<>();
                cacheData.put("symbol", symbol);
                cacheData.put("lastPrice", lastPrice);
                cacheData.put("open", open);
                cacheData.put("high", high);
                cacheData.put("low", low);
                if (Double.isFinite(previousClose) && previousClose > 0.0) {
                    double change = lastPrice - previousClose;
                    cacheData.put("previousClose", previousClose);
                    cacheData.put("change", change);
                    cacheData.put("changePercent", change / previousClose * 100.0);
                }
                cacheData.put("updatedAt", System.currentTimeMillis());
                cacheData.put("source", "UPSTOX_WS");
                if (quote.getQuoteTimestamp() != null) {
                    cacheData.put("quoteTimestamp", quote.getQuoteTimestamp().toString());
                }
                if (quoteTradeDate != null) {
                    cacheData.put("quoteTradeDate", quoteTradeDate.toString());
                }
                if (quote.getVolume() != null) cacheData.put("volume", quote.getVolume());
                if (quote.getOi() != null) cacheData.put("oi", quote.getOi());
                if (quote.getOiDayHigh() != null) cacheData.put("oiDayHigh", quote.getOiDayHigh());
                if (quote.getOiDayLow() != null) cacheData.put("oiDayLow", quote.getOiDayLow());
                if (quote.getAveragePrice() != null) cacheData.put("averagePrice", quote.getAveragePrice());
                if (quote.getTotalBuyQuantity() != null) cacheData.put("totalBuyQuantity", quote.getTotalBuyQuantity());
                if (quote.getTotalSellQuantity() != null) cacheData.put("totalSellQuantity", quote.getTotalSellQuantity());
                if (quote.getLowerCircuitLimit() != null) cacheData.put("lowerCircuitLimit", quote.getLowerCircuitLimit());
                if (quote.getUpperCircuitLimit() != null) cacheData.put("upperCircuitLimit", quote.getUpperCircuitLimit());
                if (quote.getMarketDepth() != null) cacheData.put("marketDepth", quote.getMarketDepth());

                String json = objectMapper.writeValueAsString(cacheData);
                // 1. Write standard plain key for backward compatibility
                latestPriceRedisWrites.put("market:latest-price:" + symbol, json);
                // 2. Write exchange-qualified key if rawSymbol has exchange prefix (e.g. BSE:RELIANCE or BSE_EQ|...)
                if (rawSymbol.contains(":") || rawSymbol.contains("|")) {
                    String ex = "NSE";
                    if (rawSymbol.contains(":")) {
                        ex = rawSymbol.split(":", 2)[0].trim().toUpperCase();
                    } else if (rawSymbol.contains("|")) {
                        ex = rawSymbol.split("\\|", 2)[0].trim().toUpperCase();
                    }
                    latestPriceRedisWrites.put("market:latest-price:" + ex + ":" + symbol, json);
                }
            }

            // Step 4: Execute bulk Redis pipelined write for latest prices (7 days TTL - weekend & holiday resilient)
            if (!latestPriceRedisWrites.isEmpty()) {
                redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                    for (Map.Entry<String, String> e : latestPriceRedisWrites.entrySet()) {
                        byte[] keyBytes = redisTemplate.getStringSerializer().serialize(e.getKey());
                        byte[] valueBytes = redisTemplate.getStringSerializer().serialize(e.getValue());
                        connection.setEx(keyBytes, 7 * 24 * 3600, valueBytes); // 7 days expiration
                    }
                    return null;
                });
            }

            // Step 5: Save previous close and its session date together for safe reads.
            if (!prevCloseRedisWrites.isEmpty()) {
                redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                    for (Map.Entry<String, String> e : prevCloseRedisWrites.entrySet()) {
                        byte[] keyBytes = redisTemplate.getStringSerializer().serialize(e.getKey());
                        byte[] valueBytes = redisTemplate.getStringSerializer().serialize(e.getValue());
                        connection.setEx(keyBytes, 26 * 3600, valueBytes); // 26 hours expiration
                    }
                    return null;
                });
                log.info("cacheLatestPrices", "Pipelined {} new previous close values to Redis", prevCloseRedisWrites.size());
            }

            // Step 6: Bulk save to MongoDB (only for new/uncached previous closes)
            if (previousCloseRepository != null && !mongoPrevCloseDocs.isEmpty()) {
                try {
                    previousCloseRepository.saveAll(mongoPrevCloseDocs);
                    log.info("cacheLatestPrices", "Saved {} new previous close documents to MongoDB in bulk", mongoPrevCloseDocs.size());
                } catch (Exception mongoEx) {
                    log.warn("cacheLatestPrices", "Failed to bulk save previous closes in MongoDB", mongoEx);
                }
            }

        } catch (Exception e) {
            log.error("cacheLatestPrices", "Error caching latest prices to Redis/MongoDB in bulk", e);
        }
    }

    /**
     * Live last-trade overlay is only valid while the cash market is open.
     * Closed / weekend / holiday → official daily close must win (see MarketDataService).
     */
    private boolean isLiveTickOverlayAllowed() {
        if (marketHoursService == null) {
            return true;
        }
        try {
            return marketHoursService.isMarketOpen();
        } catch (Exception e) {
            log.warn("overlayLatestPrices", "Market hours check failed; keeping live-tick overlay: " + e.getMessage());
            return true;
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
            List<String> requestedSymbols = new ArrayList<>(symbols);
            // Use MGET in bounded chunks. A NIFTY 500 request must not make 500
            // sequential Redis round trips or build an unbounded command payload.
            for (int start = 0; start < requestedSymbols.size(); start += 500) {
                List<String> chunk = requestedSymbols.subList(start, Math.min(start + 500, requestedSymbols.size()));
                List<String> primaryKeys = new ArrayList<>(chunk.size());
                List<String> fallbackKeys = new ArrayList<>(chunk.size());
                for (String symbol : chunk) {
                    String raw = symbol == null ? "" : symbol.trim().toUpperCase();
                    String exchange = "NSE";
                    if (raw.contains("|")) {
                        String[] parts = raw.split("\\|", 2);
                        exchange = parts[0];
                        raw = parts[1];
                    } else if (raw.contains(":")) {
                        String[] parts = raw.split(":", 2);
                        exchange = parts[0];
                        raw = parts[1];
                    }
                    String cleanSymbol = raw.trim().toUpperCase();
                    primaryKeys.add("market:latest-price:" + exchange.trim().toUpperCase() + ":" + cleanSymbol);
                    fallbackKeys.add("market:latest-price:" + cleanSymbol);
                }
                List<String> keys = new ArrayList<>(chunk.size() * 2);
                keys.addAll(primaryKeys);
                keys.addAll(fallbackKeys);
                List<String> values = redisTemplate.opsForValue().multiGet(keys);

                for (int index = 0; index < chunk.size(); index++) {
                    String symbol = chunk.get(index);
                    String cleanSymbol = canonicalTicker(symbol);
                    String json = values != null && index < values.size() ? values.get(index) : null;
                    if (json == null && values != null && index + chunk.size() < values.size()) {
                        json = values.get(index + chunk.size());
                    }
                    if (json != null) {
                    Map<String, Object> map = objectMapper.readValue(json, Map.class);

                    // Only ignore Redis prices as stale during active market hours.
                    // When market is closed, the closing session price remains valid until the next session.
                    long updatedAt = ((Number) map.getOrDefault("updatedAt", 0L)).longValue();
                    long ageMs = System.currentTimeMillis() - updatedAt;
                    boolean isMarketOpen = isLiveTickOverlayAllowed();
                    if (isMarketOpen && ageMs > 15 * 60 * 1000) {
                        log.warn("getLatestPrices", "Redis price for " + cleanSymbol + " is stale during market hours (age: " + (ageMs / 1000) + "s). Ignoring.");
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

                    if (Double.isFinite(lastPrice) && lastPrice > 0.0) {
                        result.put(symbol, quote);
                    } else {
                        log.warn("getLatestPrices", "Ignoring non-positive cached last price for {}", cleanSymbol);
                    }
                }
            }
            }
        } catch (Exception e) {
            log.error("getLatestPrices", "Error fetching latest prices from Redis", e);
        }
        return result;
    }

    private String canonicalTicker(String symbol) {
        String cleanSymbol = symbol == null ? "" : symbol.trim().toUpperCase();
        if (cleanSymbol.contains("|")) {
            cleanSymbol = cleanSymbol.substring(cleanSymbol.indexOf("|") + 1);
        }
        if (cleanSymbol.contains(":")) {
            cleanSymbol = cleanSymbol.substring(cleanSymbol.indexOf(":") + 1);
        }
        return cleanSymbol.trim();
    }

    /**
     * Cache option chain data with dynamic TTL.
     * Also updates a 24-hour stale backup cache for disaster recovery / upstream downtime.
     */
    public void cacheOptionChain(String underlyingSymbol, String expiryDate, Map<String, Object> data, boolean isMarketOpen) {
        if (underlyingSymbol == null || data == null || data.isEmpty()) {
            return;
        }
        String cleanSymbol = normalizeSymbol(underlyingSymbol);
        String expiryKey = (expiryDate != null && !expiryDate.isEmpty()) ? expiryDate : "DEFAULT";
        String primaryKey = "market:optionchain:" + cleanSymbol + ":" + expiryKey;
        String staleKey = "market:optionchain:stale:" + cleanSymbol + ":" + expiryKey;

        try {
            String json = objectMapper.writeValueAsString(data);
            // Live market: 60s. After-hours / weekend / holiday: 12 hours
            long ttlSeconds = isMarketOpen ? 60L : 43200L;
            redisTemplate.opsForValue().set(primaryKey, json, ttlSeconds, TimeUnit.SECONDS);

            // Stale backup key valid for 24 hours
            redisTemplate.opsForValue().set(staleKey, json, 86400L, TimeUnit.SECONDS);
            log.info("cacheOptionChain", "Cached option chain for key: " + primaryKey + " with TTL: " + ttlSeconds + "s");
        } catch (Exception e) {
            log.warn("cacheOptionChain", "Failed to cache option chain in Redis: " + e.getMessage());
        }
    }

    /**
     * Get fresh option chain from Redis.
     */
    public Map<String, Object> getOptionChainFromCache(String underlyingSymbol, String expiryDate) {
        if (underlyingSymbol == null) {
            return null;
        }
        String cleanSymbol = normalizeSymbol(underlyingSymbol);
        String expiryKey = (expiryDate != null && !expiryDate.isEmpty()) ? expiryDate : "DEFAULT";
        String primaryKey = "market:optionchain:" + cleanSymbol + ":" + expiryKey;

        try {
            String json = redisTemplate.opsForValue().get(primaryKey);
            if (json != null && !json.isEmpty()) {
                return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.warn("getOptionChainFromCache", "Error reading option chain cache for key: " + primaryKey + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Get stale fallback option chain when upstream (Upstox) fails.
     */
    public Map<String, Object> getStaleOptionChain(String underlyingSymbol, String expiryDate) {
        if (underlyingSymbol == null) {
            return null;
        }
        String cleanSymbol = normalizeSymbol(underlyingSymbol);
        String expiryKey = (expiryDate != null && !expiryDate.isEmpty()) ? expiryDate : "DEFAULT";
        String staleKey = "market:optionchain:stale:" + cleanSymbol + ":" + expiryKey;

        try {
            String json = redisTemplate.opsForValue().get(staleKey);
            if (json != null && !json.isEmpty()) {
                Map<String, Object> map = objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                if (map != null) {
                    map.put("isStale", true);
                }
                return map;
            }
        } catch (Exception e) {
            log.warn("getStaleOptionChain", "Error reading stale option chain cache: " + e.getMessage());
        }
        return null;
    }

    /**
     * Acquire a distributed lock for option chain calculation/fetching (Cache Stampede / Thundering Herd shield).
     * Returns true if lock was acquired.
     */
    public boolean acquireOptionChainLock(String underlyingSymbol, String expiryDate, long lockSeconds) {
        String cleanSymbol = normalizeSymbol(underlyingSymbol);
        String expiryKey = (expiryDate != null && !expiryDate.isEmpty()) ? expiryDate : "DEFAULT";
        String lockKey = "lock:optionchain:" + cleanSymbol + ":" + expiryKey;

        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", lockSeconds, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            log.warn("acquireOptionChainLock", "Redis lock error: " + e.getMessage());
            return true; // fail open if redis lock fails
        }
    }

    /**
     * Release distributed lock for option chain.
     */
    public void releaseOptionChainLock(String underlyingSymbol, String expiryDate) {
        String cleanSymbol = normalizeSymbol(underlyingSymbol);
        String expiryKey = (expiryDate != null && !expiryDate.isEmpty()) ? expiryDate : "DEFAULT";
        String lockKey = "lock:optionchain:" + cleanSymbol + ":" + expiryKey;

        try {
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            log.warn("releaseOptionChainLock", "Failed to release lock: " + e.getMessage());
        }
    }
}
