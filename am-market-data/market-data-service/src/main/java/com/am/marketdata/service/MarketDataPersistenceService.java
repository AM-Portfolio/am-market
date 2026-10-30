package com.am.marketdata.service;

import com.am.common.investment.model.equity.EquityPrice;
import com.am.common.investment.model.equity.MarketData;
import com.am.common.investment.model.equity.MarketIndexIndices;
import com.am.common.investment.model.historical.HistoricalData;
import com.am.common.investment.model.stockindice.StockIndicesMarketData;
import com.am.common.investment.service.EquityService;
import com.am.common.investment.service.MarketIndexIndicesService;
import com.am.common.investment.service.StockIndicesMarketDataService;
import com.am.common.investment.service.historical.HistoricalDataService;
import com.am.marketdata.common.mapper.OHLCMapper;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.service.global.GlobalIndexCacheWriter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Implementation of MarketDataPersistenceService
 * Handles saving and retrieving market data from both database and cache
 */
@Slf4j
@Service
public class MarketDataPersistenceService implements com.am.marketdata.common.service.MarketDataIngestionService {

    private final HistoricalDataService historicalDataService;
    private final MarketDataCacheService marketDataCacheService;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final EquityService equityService;
    private final com.am.common.investment.service.EquityLatestPriceService equityLatestPriceService;
    private final OHLCMapper ohlcMapper;
    private final MarketIndexIndicesService marketIndexIndicesService;
    private final StockIndicesMarketDataService stockIndicesMarketDataService;
    private final GlobalIndexCacheWriter globalIndexCacheWriter;
    private final Map<String, Boolean> indexClassificationCache = new ConcurrentHashMap<>();

    private static final Set<String> KNOWN_INDEX_SYMBOLS = Set.of(
            "INDIA VIX", "NIFTY 50", "NIFTY NEXT 50", "NIFTY 100", "NIFTY 200",
            "NIFTY 500", "NIFTY MIDCAP 50", "NIFTY MIDCAP 100", "NIFTY SMLCAP 100",
            "NIFTY MIDCAP 150", "NIFTY SMLCAP 50", "NIFTY SMLCAP 250", "NIFTY SMLCAP 500",
            "NIFTY BANK", "NIFTY AUTO", "NIFTY FMCG", "NIFTY MEDIA", "NIFTY METAL",
            "NIFTY PHARMA", "NIFTY PSU BANK", "NIFTY PVT BANK", "NIFTY REALTY",
            "NIFTY HEALTHCARE", "NIFTY CONSR DURABLE", "NIFTY OIL AND GAS", "NIFTY MIDSML HLTH",
            "NIFTY IT", "NIFTY FIN SERVICES", "NIFTY ENERGY", "NIFTY PHARMACEUTICALS",
            "SENSEX", "NIFTY", "BANKNIFTY", "FINNIFTY", "MIDCPNIFTY");

    public MarketDataPersistenceService(
            HistoricalDataService historicalDataService,
            MarketDataCacheService marketDataCacheService,
            EquityService equityService,
            com.am.common.investment.service.EquityLatestPriceService equityLatestPriceService,
            OHLCMapper ohlcMapper,
            MarketIndexIndicesService marketIndexIndicesService,
            StockIndicesMarketDataService stockIndicesMarketDataService,
            GlobalIndexCacheWriter globalIndexCacheWriter,
            @Qualifier("marketDataPersistenceExecutor") ThreadPoolTaskExecutor taskExecutor) {
        this.historicalDataService = historicalDataService;
        this.marketDataCacheService = marketDataCacheService;
        this.equityService = equityService;
        this.equityLatestPriceService = equityLatestPriceService;
        this.ohlcMapper = ohlcMapper;
        this.marketIndexIndicesService = marketIndexIndicesService;
        this.stockIndicesMarketDataService = stockIndicesMarketDataService;
        this.globalIndexCacheWriter = globalIndexCacheWriter;
        this.taskExecutor = taskExecutor;
    }

    /**
     * Get the market data cache service
     * 
     * @return The market data cache service
     */
    public MarketDataCacheService getMarketDataCacheService() {
        return marketDataCacheService;
    }

    public CompletableFuture<Void> saveOHLCData(Map<String, OHLCQuote> ohlcData) {
        return saveOHLCData(ohlcData, null);
    }

    public CompletableFuture<Void> saveOHLCData(Map<String, OHLCQuote> ohlcData, String provider) {
        if (ohlcData == null || ohlcData.isEmpty()) {
            log.warn("No OHLC data to save");
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                if ("MOCK".equalsIgnoreCase(provider)) {
                    log.info("Simulated/MOCK stream - bypassing MongoDB database and Cache save to prevent pollution");
                } else {
                    Map<String, OHLCQuote> globalQuotes = extractGlobalQuotes(ohlcData);
                    Map<String, OHLCQuote> indexQuotes = extractIndexQuotes(ohlcData);
                    Map<String, OHLCQuote> equityQuotes = new HashMap<>(ohlcData);
                    equityQuotes.keySet().removeAll(indexQuotes.keySet());
                    equityQuotes.keySet().removeAll(globalQuotes.keySet());
                    log.info("Routing OHLC batch: total={}, equity={}, index={}, global={}",
                            ohlcData.size(), equityQuotes.size(), indexQuotes.size(), globalQuotes.size());

                    // 1. Save only individual stocks to the equity measurement.
                    if (!equityQuotes.isEmpty()) {
                        log.debug("Saving {} equity OHLC data points to database", equityQuotes.size());
                        List<EquityPrice> equityPrices = ohlcMapper.toEquityPriceList(equityQuotes, provider);
                        equityService.saveAllPrices(equityPrices);
                        log.debug("Successfully saved {} equity prices to database", equityPrices.size());
                    }

                    // 2. Save Indian index ticks only to the market_index measurement.
                    saveIndexQuotes(indexQuotes);

                    // 3. Keep global index ticks on the isolated global_market_index path.
                    saveGlobalQuotes(globalQuotes);

                    // 4. Update the cache with default timeframe (1D for current day data)
                    marketDataCacheService.cacheOHLCData(ohlcData, TimeFrame.DAY);
                    log.debug("Successfully cached OHLC data for {} symbols", ohlcData.size());

                    // 5. Keep the document used by the index API in sync.
                    updateStockIndicesIfNecessary(indexQuotes);
                }
            } catch (Exception e) {
                log.error("Error saving OHLC data: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to save OHLC data", e);
            }
        }, taskExecutor);
    }

    /**
     * Updates the permanent StockIndicesMarketData collection if any symbols in the map are indices.
     * This bridges the gap between Streaming/Fallback data and the Index Page.
     */
    private void updateStockIndicesIfNecessary(Map<String, OHLCQuote> ohlcData) {
        for (Map.Entry<String, OHLCQuote> entry : ohlcData.entrySet()) {
            String rawSymbol = entry.getKey();
            String symbol = normalizeSymbol(rawSymbol);

            try {
                StockIndicesMarketData indexData = stockIndicesMarketDataService.findByIndexSymbol(symbol);
                if (indexData != null) {
                    OHLCQuote quote = entry.getValue();
                    var meta = indexData.getMetadata();
                    if (meta == null) {
                        meta = new com.am.common.investment.model.events.StockInsidicesEventData.IndexMetadata();
                        indexData.setMetadata(meta);
                    }

                    // Update prices
                    meta.setLast(quote.getLastPrice());
                    if (quote.getOhlc() != null) {
                        meta.setOpen(quote.getOhlc().getOpen());
                        meta.setHigh(quote.getOhlc().getHigh());
                        meta.setLow(quote.getOhlc().getLow());
                    }

                    // Recalculate change statistics
                    // 1. Prioritize the official adjusted previousClose from the Redis cache
                    Double prevClose = marketDataCacheService.getPreviousClose(symbol);
                    
                    // 2. Fall back to the live tick's previousClose if the cache is empty
                    if (prevClose == null || prevClose == 0.0) {
                        prevClose = quote.getPreviousClose();
                        // Dynamically populate Redis so subsequent ticks don't hit the fallback logic
                        if (prevClose != null && prevClose != 0.0) {
                            marketDataCacheService.setPreviousClose(symbol, prevClose);
                        }
                    }
                    
                    // 3. Fall back to existing MongoDB metadata if still unresolved
                    if ((prevClose == null || prevClose == 0.0) && meta.getPreviousClose() != null) {
                        prevClose = meta.getPreviousClose();
                    }

                    if (prevClose != null && prevClose != 0 && quote.getLastPrice() != 0) {
                        double deviation = Math.abs(prevClose - quote.getLastPrice()) / quote.getLastPrice();
                        if (deviation > 0.30) {
                            log.warn("Discarding suspicious previousClose={} for {} (lastPrice={}, deviation={}%) during streaming update",
                                    prevClose, symbol, quote.getLastPrice(), String.format("%.1f", deviation * 100));
                            prevClose = 0.0;
                        }
                    }

                    if (prevClose != null && prevClose != 0) {
                        double change = quote.getLastPrice() - prevClose;
                        meta.setChange(change);
                        meta.setPercChange((change / prevClose) * 100.0);
                        meta.setPreviousClose(prevClose);
                    } else {
                        meta.setChange(0.0);
                        meta.setPercChange(0.0);
                    }
                    
                    meta.setTimeVal(String.valueOf(System.currentTimeMillis()));

                    // Save back to permanent DB
                    stockIndicesMarketDataService.save(indexData);
                    log.debug("Unified Persistence: Updated permanent Index DB for symbol: {}", symbol);
                }
            } catch (Exception e) {
                log.warn("Failed to update permanent Index DB for symbol: {}. Error: {}", symbol, e.getMessage());
            }
        }
    }

    private Map<String, OHLCQuote> extractIndexQuotes(Map<String, OHLCQuote> ohlcData) {
        Map<String, OHLCQuote> indexQuotes = new HashMap<>();
        Set<String> unresolvedSymbols = new HashSet<>();

        for (String rawSymbol : ohlcData.keySet()) {
            String normalized = normalizeSymbol(rawSymbol);
            if (isExplicitIndexKey(rawSymbol) || KNOWN_INDEX_SYMBOLS.contains(normalized)) {
                indexClassificationCache.put(normalized, true);
            } else if (!indexClassificationCache.containsKey(normalized)) {
                unresolvedSymbols.add(normalized);
            }
        }

        if (!unresolvedSymbols.isEmpty()) {
            try {
                List<StockIndicesMarketData> knownIndices = stockIndicesMarketDataService
                        .findByIndexSymbols(unresolvedSymbols);
                Set<String> resolvedIndexSymbols = knownIndices == null
                        ? Collections.emptySet()
                        : knownIndices.stream()
                                .map(StockIndicesMarketData::getIndexSymbol)
                                .filter(symbol -> symbol != null && !symbol.isBlank())
                                .map(this::normalizeSymbol)
                                .collect(Collectors.toSet());

                unresolvedSymbols.forEach(symbol ->
                        indexClassificationCache.put(symbol, resolvedIndexSymbols.contains(symbol)));
            } catch (Exception e) {
                // The explicit key prefix and known-index checks remain authoritative. Do not
                // block equity persistence if the metadata lookup is temporarily unavailable.
                log.warn("Unable to resolve index symbols from metadata; using known classifications: {}",
                        e.getMessage());
            }
        }

        for (Map.Entry<String, OHLCQuote> entry : ohlcData.entrySet()) {
            String normalized = normalizeSymbol(entry.getKey());
            if (Boolean.TRUE.equals(indexClassificationCache.get(normalized))) {
                indexQuotes.put(entry.getKey(), entry.getValue());
            }
        }
        return indexQuotes;
    }

    private Map<String, OHLCQuote> extractGlobalQuotes(Map<String, OHLCQuote> ohlcData) {
        Map<String, OHLCQuote> globalQuotes = new HashMap<>();
        for (Map.Entry<String, OHLCQuote> entry : ohlcData.entrySet()) {
            String symbol = entry.getKey();
            if (symbol != null && symbol.trim().toUpperCase(Locale.ROOT).startsWith("GLOBAL_")) {
                globalQuotes.put(symbol, entry.getValue());
            }
        }
        return globalQuotes;
    }

    private void saveIndexQuotes(Map<String, OHLCQuote> indexQuotes) {
        for (Map.Entry<String, OHLCQuote> entry : indexQuotes.entrySet()) {
            OHLCQuote quote = entry.getValue();
            if (quote == null) {
                continue;
            }

            String symbol = normalizeSymbol(entry.getKey());
            double previousClose = quote.getPreviousClose();
            double change = quote.getChange() != null
                    ? quote.getChange()
                    : quote.getLastPrice() - previousClose;
            double changePercent = quote.getChangePercent() != null
                    ? quote.getChangePercent()
                    : previousClose > 0 ? (change / previousClose) * 100.0 : 0.0;

            OHLCQuote.OHLC ohlc = quote.getOhlc();
            MarketData marketData = MarketData.builder()
                    .last(quote.getLastPrice())
                    .open(ohlc != null ? ohlc.getOpen() : quote.getLastPrice())
                    .high(ohlc != null ? ohlc.getHigh() : quote.getLastPrice())
                    .low(ohlc != null ? ohlc.getLow() : quote.getLastPrice())
                    .previousClose(previousClose)
                    .variation(change)
                    .percentChange(changePercent)
                    .build();

            MarketIndexIndices indexData = MarketIndexIndices.builder()
                    .key(entry.getKey())
                    .index(symbol)
                    .indexSymbol(symbol)
                    .marketData(marketData)
                    .timestamp(LocalDateTime.now(ZoneId.of("Asia/Kolkata")))
                    .build();

            marketIndexIndicesService.save(indexData);
        }
        if (!indexQuotes.isEmpty()) {
            log.debug("Saved {} index ticks to market_index", indexQuotes.size());
        }
    }

    private void saveGlobalQuotes(Map<String, OHLCQuote> globalQuotes) {
        for (Map.Entry<String, OHLCQuote> entry : globalQuotes.entrySet()) {
            if (entry.getValue() != null) {
                globalIndexCacheWriter.writeTick(entry.getKey(), entry.getValue());
            }
        }
        if (!globalQuotes.isEmpty()) {
            log.debug("Saved {} global index ticks to global_market_index", globalQuotes.size());
        }
    }

    private boolean isExplicitIndexKey(String rawSymbol) {
        String upper = rawSymbol == null ? "" : rawSymbol.trim().toUpperCase(Locale.ROOT);
        return upper.startsWith("NSE_INDEX|") || upper.startsWith("BSE_INDEX|")
                || upper.startsWith("NSE_INDEX:") || upper.startsWith("BSE_INDEX:");
    }

    private String normalizeSymbol(String rawSymbol) {
        if (rawSymbol == null) {
            return "";
        }
        String symbol = rawSymbol.trim().toUpperCase(Locale.ROOT);
        for (String prefix : List.of("NSE_INDEX|", "BSE_INDEX|", "NSE_INDEX:", "BSE_INDEX:",
                "NSE_EQ|", "BSE_EQ|", "NSE_EQ:", "BSE_EQ:", "NSE:", "BSE:")) {
            if (symbol.startsWith(prefix)) {
                return symbol.substring(prefix.length()).trim();
            }
        }
        return symbol;
    }

    public CompletableFuture<Void> saveHistoricalData(String symbol, TimeFrame interval,
            HistoricalData historicalData) {
        if (historicalData == null || historicalData.getDataPoints() == null
                || historicalData.getDataPoints().isEmpty()) {
            log.warn("No historical data to save for symbol: {}", symbol);
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                // First save to database
                log.debug("Saving historical data to database for symbol: {}", symbol);
                historicalDataService.saveHistoricalData(historicalData);
                log.debug("Successfully saved historical data for symbol: {}", symbol);

                // Then update the cache
                marketDataCacheService.cacheHistoricalData(symbol, interval, historicalData);
                log.debug("Successfully cached historical data for symbol: {}", symbol);
            } catch (Exception e) {
                log.error("Error saving historical data for symbol {}: {}", symbol, e.getMessage(), e);
                throw new RuntimeException("Failed to save historical data", e);
            }
        }, taskExecutor);
    }

    public Map<String, OHLCQuote> getOHLCData(List<String> tradingSymbols, TimeFrame timeFrame, boolean forceRefresh) {
        if (tradingSymbols == null || tradingSymbols.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            // Filter out index symbols early to prevent false cache misses
            Set<String> knownIndices = new HashSet<>(Arrays.asList(
                "INDIA VIX", "NIFTY 50", "NIFTY NEXT 50", "NIFTY 100", "NIFTY 200", 
                "NIFTY 500", "NIFTY MIDCAP 50", "NIFTY MIDCAP 100", "NIFTY SMLCAP 100", 
                "NIFTY MIDCAP 150", "NIFTY SMLCAP 50", "NIFTY SMLCAP 250", "NIFTY SMLCAP 500", 
                "NIFTY BANK", "NIFTY AUTO", "NIFTY FMCG", "NIFTY MEDIA", "NIFTY METAL", 
                "NIFTY PHARMA", "NIFTY PSU BANK", "NIFTY PVT BANK", "NIFTY REALTY", 
                "NIFTY HEALTHCARE", "NIFTY CONSR DURABLE", "NIFTY OIL AND GAS", 
                "NIFTY MIDSML HLTH", "NIFTY IT", "NIFTY FIN SERVICES", "NIFTY ENERGY", 
                "NIFTY PHARMACEUTICALS", "SENSEX", "NIFTY", "BANKNIFTY"
            ));
            List<String> filteredSymbols = tradingSymbols.stream()
                    .filter(symbol -> {
                        String clean = symbol.replaceAll("(?i)^(NSE_EQ\\||NSE_INDEX\\||BSE_EQ\\||BSE_INDEX\\||NSE_EQ:|BSE_EQ:|NSE:|BSE:)", "").trim().toUpperCase();
                        return !knownIndices.contains(clean) && 
                               !clean.startsWith("NIFTY ") && 
                               !clean.contains("VIX") && 
                               !clean.startsWith("SENSEX") && 
                               !clean.equals("FINNIFTY") &&
                               !clean.equals("MIDCPNIFTY");
                    })
                    .collect(Collectors.toList());

            if (filteredSymbols.isEmpty()) {
                log.debug("All symbols were indices, returning empty result");
                return Collections.emptyMap();
            }

            // Use "LIVE" as the cache key for current/live prices (when timeFrame is null)
            String tfValue = timeFrame != null ? timeFrame.getApiValue() : "1D";

            Map<String, OHLCQuote> result = new HashMap<>();
            Set<String> remainingSymbols = new HashSet<>(filteredSymbols);

            if (!forceRefresh) {
                // Try cache first
                Map<String, OHLCQuote> cachedData = marketDataCacheService.getOHLCFromCache(tradingSymbols, timeFrame);
                if (cachedData != null && !cachedData.isEmpty()) {
                    log.debug("Retrieved {} OHLC data from cache with timeFrame {}",
                            cachedData.size(), tfValue);
                    result.putAll(cachedData);

                    // Remove found symbols
                    cachedData.keySet().forEach(key -> {
                        String symbol = key.replaceAll("(?i)^(NSE_EQ\\||NSE_INDEX\\||BSE_EQ\\||BSE_INDEX\\||NSE_EQ:|BSE_EQ:|NSE:|BSE:)", "");
                        remainingSymbols.remove(symbol);
                    });

                    log.debug("{} symbols remaining after cache lookup for timeFrame {}",
                            remainingSymbols.size(), tfValue);

                    if (remainingSymbols.isEmpty()) {
                        return result;
                    }
                }
            }

            // If we have remaining symbols or forceRefresh is true, try database
            if (!remainingSymbols.isEmpty() || forceRefresh) {
                log.debug("{} OHLC data from database for {} symbols with timeFrame {}",
                        forceRefresh ? "Forcing refresh of" : "Fetching missing",
                        remainingSymbols.size(), tfValue);

                // Clean symbols (remove NSE: prefix if present)
                List<String> cleanSymbols = remainingSymbols.stream()
                        .map(symbol -> symbol.replaceAll("(?i)^(NSE_EQ\\||NSE_INDEX\\||BSE_EQ\\||BSE_INDEX\\||NSE_EQ:|BSE_EQ:|NSE:|BSE:)", ""))
                        .collect(Collectors.toList());

                /*
                 * FAST DATABASE FALLBACK LOOKUP:
                 * ---------------------------------------------------------------------------------------------
                 * WHAT PROBLEM IT SOLVES:
                 * Previously, this called equityService.getPricesByTradingSymbols(...) which scanned 30 days
                 * (-30d) of historical tick data before running last(), causing a 10-second InfluxDB delay.
                 * 
                 * HOW IT WORKS:
                 * We call equityLatestPriceService.getLatestPricesByTradingSymbols(...) which uses a narrow 
                 * holiday-safe -5d time window, returning latest quotes in ~35ms instead of 10,000ms.
                 */
                List<EquityPrice> equityPrices = equityLatestPriceService.getLatestPricesByTradingSymbols(cleanSymbols);

                if (!equityPrices.isEmpty()) {
                    // Convert equity prices to OHLCQuote format
                    for (EquityPrice price : equityPrices) {
                        if (price.getLastPrice() == null) {
                            continue;
                        }
                        OHLCQuote quote = createOHLCQuoteFromEquityPrice(price);
                        // String symbol = "NSE:" + price.getSymbol();
                        result.put(price.getSymbol(), quote);

                        // Remove found symbols from the remaining set
                        remainingSymbols.remove(price.getSymbol());
                    }

                    log.debug("Retrieved OHLC data from database for {} symbols with timeFrame {}",
                            equityPrices.size(), tfValue);
                }
            }

            return result;
        } catch (Exception e) {
            String tfValue = timeFrame != null ? timeFrame.getApiValue() : "LIVE";
            log.error("Error retrieving OHLC data with timeFrame {}: {}",
                    tfValue, e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Maps the interval format from API format to database format
     * 
     * @param apiInterval Interval in API format (e.g., "1d", "day", "1m", "minute")
     * @return Interval in database format
     */
    private String mapIntervalFormat(String apiInterval) {
        if (apiInterval == null || apiInterval.isEmpty()) {
            return "day"; // Default to daily interval
        }

        // Convert to lowercase for case-insensitive comparison
        String interval = apiInterval.toLowerCase();

        // Map common interval formats
        if (interval.contains("day") || interval.equals("1d") || interval.equals("d")) {
            return "day";
        } else if (interval.contains("week") || interval.equals("1w") || interval.equals("w")) {
            return "week";
        } else if (interval.contains("month") || interval.equals("1m") || interval.equals("m")) {
            return "month";
        } else if (interval.contains("minute") || interval.contains("min")) {
            // Extract number of minutes if specified (e.g., "5minute" -> "5minute")
            if (interval.matches("\\d+.*")) {
                String minutes = interval.replaceAll("[^\\d]", "");
                return minutes + "minute";
            }
            return "minute";
        } else if (interval.contains("hour") || interval.contains("hr") || interval.equals("1h")
                || interval.equals("h")) {
            return "hour";
        }

        // Return as is if no mapping found
        return interval;
    }

    /**
     * Creates an OHLCQuote object from an EquityPrice object
     * 
     * @param price EquityPrice object
     * @return OHLCQuote object
     */
    private OHLCQuote createOHLCQuoteFromEquityPrice(EquityPrice price) {
        OHLCQuote quote = new OHLCQuote();
        quote.setLastPrice(price.getLastPrice());

        OHLCQuote.OHLC ohlc = new OHLCQuote.OHLC();
        ohlc.setOpen(price.getOhlcv().getOpen());
        ohlc.setHigh(price.getOhlcv().getHigh());
        ohlc.setLow(price.getOhlcv().getLow());
        ohlc.setClose(price.getOhlcv().getClose());

        quote.setOhlc(ohlc);

        // Set additional fields if available
        // if (price.getVolume() != null) {
        // // Volume is not currently part of the OHLC model
        // }

        return quote;
    }

    public HistoricalData getHistoricalData(String symbol, TimeFrame interval, String fromDate, String toDate) {
        return getHistoricalData(symbol, interval, fromDate, toDate, false);
    }

    public HistoricalData getHistoricalData(String symbol, TimeFrame interval, String fromDate, String toDate, boolean isIndexSymbol) {
        if (symbol == null || symbol.isEmpty() || interval == null) {
            return null;
        }

        try {
            // Clean symbol (remove exchange prefixes like NSE_EQ|, NSE_INDEX|, NSE:, NSE_EQ: if present)
            String cleanSymbol = symbol.replaceAll("(?i)^(NSE_EQ\\||NSE_INDEX\\||BSE_EQ\\||BSE_INDEX\\||NSE_EQ:|BSE_EQ:|NSE:|BSE:)", "").trim();

            // Parse dates - handle potential format variations
            LocalDate from;
            LocalDate to;
            try {
                from = LocalDate.parse(fromDate, DateTimeFormatter.ISO_LOCAL_DATE);
                to = LocalDate.parse(toDate, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception e) {
                // Try alternative format if ISO format fails
                DateTimeFormatter alternativeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                from = LocalDate.parse(fromDate, alternativeFormatter);
                to = LocalDate.parse(toDate, alternativeFormatter);
            }

            // First try to get from cache
            HistoricalData cachedData = marketDataCacheService.getHistoricalDataFromCache(
                    symbol, interval, fromDate, toDate);

            if (cachedData != null && cachedData.getDataPoints() != null && !cachedData.getDataPoints().isEmpty()) {
                // Verify that the cached data actually covers the requested date range
                if (validateDateRangeCoverage(cachedData, from, to)) {
                    log.debug("Retrieved historical data from cache for symbol: {}", symbol);
                    return cachedData;
                } else {
                    log.info("Cached data for symbol {} exists but does not cover the requested range ({} to {}). Bypassing cache to query database.", 
                            symbol, fromDate, toDate);
                }
            }

            // If not in cache or cache range is invalid, try to get from database
            log.debug("No valid historical data found in cache, fetching from database for symbol: {}", symbol);

            // Map interval to the format expected by the database service (e.g. "5m", "1H") rather than enum name ("five_minute")
            String mappedInterval = interval.getApiValue();

            // Get historical data from database using HistoricalDataService
            // Convert LocalDate to Instant at the start of the day in UTC
            Instant fromInstant = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
            // For 'to' date, we want to include the entire day, so we move to the start of
            // the next day
            // This also prevents "empty range" errors in InfluxDB if from == to
            Instant toInstant = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

            // Handle Optional return type
            HistoricalData historicalData = historicalDataService.getHistoricalData(
                    cleanSymbol, fromInstant, toInstant, mappedInterval, isIndexSymbol).orElse(null);

            if (historicalData != null && historicalData.getDataPoints() != null
                    && !historicalData.getDataPoints().isEmpty()) {
                log.debug("Retrieved historical data from database for symbol: {}", symbol);
                return historicalData;
            }

            log.debug("No historical data found in database for symbol: {}", symbol);
            return null;
        } catch (Exception e) {
            log.error("Error retrieving historical data for symbol {}: {}", symbol, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Bulk historical-store read used after the existing Redis batch lookup.
     * Existing single-symbol APIs remain unchanged for all other callers.
     */
    public Map<String, HistoricalData> getHistoricalDataBatch(List<String> symbols, TimeFrame interval,
            String fromDate, String toDate, boolean isIndexSymbol) {
        if (symbols == null || symbols.isEmpty() || interval == null) return Collections.emptyMap();
        try {
            LocalDate from = LocalDate.parse(fromDate, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate to = LocalDate.parse(toDate, DateTimeFormatter.ISO_LOCAL_DATE);
            Instant fromInstant = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant toInstant = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

            Map<String, String> originalByCleanSymbol = new HashMap<>();
            for (String symbol : symbols) {
                if (symbol == null || symbol.isBlank()) continue;
                String cleanSymbol = symbol.replaceAll(
                        "(?i)^(NSE_EQ\\||NSE_INDEX\\||BSE_EQ\\||BSE_INDEX\\||NSE_EQ:|BSE_EQ:|NSE:|BSE:)", "")
                        .trim().toUpperCase(java.util.Locale.ROOT);
                originalByCleanSymbol.putIfAbsent(cleanSymbol, symbol);
            }
            if (originalByCleanSymbol.isEmpty()) return Collections.emptyMap();

            Map<String, HistoricalData> storedData = historicalDataService.getHistoricalDataBatch(
                    new java.util.ArrayList<>(originalByCleanSymbol.keySet()), fromInstant, toInstant,
                    interval.getApiValue(), isIndexSymbol);
            if (storedData == null || storedData.isEmpty()) return Collections.emptyMap();

            Map<String, HistoricalData> result = new HashMap<>();
            storedData.forEach((cleanSymbol, data) -> {
                String requestedSymbol = cleanSymbol == null ? null
                        : originalByCleanSymbol.get(cleanSymbol.toUpperCase(java.util.Locale.ROOT));
                if (requestedSymbol != null && data != null && data.getDataPoints() != null
                        && !data.getDataPoints().isEmpty()) {
                    result.put(requestedSymbol, data);
                }
            });
            return result;
        } catch (Exception e) {
            log.error("Error retrieving batch historical data for {} symbols interval={}: {}",
                    symbols.size(), interval.getApiValue(), e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Verifies if the cached historical data covers the requested date range within a tolerance window
     */
    private boolean validateDateRangeCoverage(HistoricalData data, LocalDate requiredStart, LocalDate requiredEnd) {
        if (data == null || data.getDataPoints() == null || data.getDataPoints().isEmpty()) {
            return false;
        }

        List<com.am.common.investment.model.historical.OHLCVTPoint> points = data.getDataPoints();
        
        // Ensure points are sorted chronologically
        points.sort(java.util.Comparator.comparing(com.am.common.investment.model.historical.OHLCVTPoint::getTime));

        java.time.LocalDateTime firstPointTime = points.get(0).getTime();
        java.time.LocalDateTime lastPointTime = points.get(points.size() - 1).getTime();

        long dataStartMs = firstPointTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long dataEndMs = lastPointTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        long requiredStartMs = requiredStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long requiredEndMs = requiredEnd.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

        long toleranceMs = 7L * 24 * 60 * 60 * 1000; // 7 days tolerance for holidays/weekends

        boolean missingEarlyData = dataStartMs > (requiredStartMs + toleranceMs);
        boolean missingRecentData = dataEndMs < (requiredEndMs - toleranceMs);

        return !missingEarlyData && !missingRecentData;
    }
}
