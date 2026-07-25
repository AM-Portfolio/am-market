package com.am.marketdata.provider.upstox;

import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.provider.upstox.model.HistoricalDataResponse;
import com.am.marketdata.provider.upstox.model.OHLCResponse;
import com.marketdata.common.MarketDataProvider;
import com.upstox.api.GetMarketQuoteLastTradedPriceResponseV3;
import com.upstox.api.MarketQuoteSymbolLtpV3;

import com.am.common.investment.model.historical.HistoricalData;
import com.am.common.investment.model.historical.OHLCVTPoint;
import com.zerodhatech.models.Instrument;
import com.zerodhatech.models.LTPQuote;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import java.util.stream.Collectors;

import com.am.marketdata.common.log.AppLogger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.am.observability.trace.IgnoreTracing;

@Service("upstoxMarketDataProvider")
@IgnoreTracing
public class UpstoxMarketDataProvider implements MarketDataProvider {

    private final AppLogger log = AppLogger.getLogger();

    private final UpstoxApiService upstoxApiService;
    private final UpstoxSdkService upstoxSdkService;
    private final com.am.marketdata.provider.upstox.resolver.UpstoxSymbolResolver symbolResolver;

    // Upstox API restricts the number of instrument keys per request to 500.
    // Adding batching logic to prevent HTTP 400 Bad Request errors when fetching for many symbols.
    private static final int BATCH_SIZE = 500;
    // Delay between batch requests to prevent triggering HTTP 429 Rate Limits.
    private static final int BATCH_DELAY_MS = 150;

    public UpstoxMarketDataProvider(
            UpstoxApiService upstoxApiService,
            UpstoxSdkService upstoxSdkService,
            com.am.marketdata.provider.upstox.resolver.UpstoxSymbolResolver symbolResolver) {
        this.upstoxApiService = upstoxApiService;
        this.upstoxSdkService = upstoxSdkService;
        this.symbolResolver = symbolResolver;
    }

    // ... (initialize, cleanup, setAccessToken, getLoginUrl, generateSession,
    // getQuotes methods remain unchanged)

    @Override
    public void initialize() {
        upstoxApiService.initialize();
    }

    @Override
    public void cleanup() {
        // Cleanup logic
    }

    @Override
    public void setAccessToken(String accessToken) {
        upstoxApiService.setAccessToken(accessToken);
    }

    @Override
    public String getLoginUrl() {
        return upstoxApiService.getLoginUrl();
    }

    @Override
    public Object generateSession(String requestToken) {
        return upstoxApiService.generateSession(requestToken);
    }

    @Override
    public Map<String, Object> getQuotes(String[] symbols) {
        return new HashMap<>();
    }

    @Override
    public Map<String, OHLCQuote> getOHLC(List<String> symbols, TimeFrame timeFrame) {
        try {
            com.am.marketdata.provider.common.InstrumentContext context = symbolResolver.resolveContext(symbols);

            log.info("getOHLC",
                    String.format("Resolved %d instruments for symbols: %s", context.instrumentKeys.size(), symbols));
            if (context.getInstrumentKeys().isEmpty()) {
                log.warn("getOHLC", "No instrument keys resolved for symbols: " + symbols);
                return new HashMap<>();
            }

            log.info("getOHLC", "Fetching OHLC from Upstox API for keys: " + context.instrumentKeys);

            // Upstox requires interval for HOhlc. Defaulting to 1 day as it's common for
            // general OHLC quote
            String upstoxInterval = timeFrame.getUpStockValue();

            log.debug("getOHLC", "Fetching OHLC using interval: " + upstoxInterval);

            Map<String, OHLCQuote> result = new HashMap<>();
            List<String> allKeys = context.instrumentKeys;

            log.info("getOHLC", String.format("Fetching OHLC quotes in batches of %d to comply with API limits", BATCH_SIZE));
            
            for (int i = 0; i < allKeys.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, allKeys.size());
                List<String> batchKeys = allKeys.subList(i, end);
                
                log.info("getOHLC", String.format("Fetching OHLC batch: %d to %d", i, end));

                OHLCResponse response = null;

                // Try SDK Service first
                try {
                    com.am.marketdata.provider.upstox.model.OHLCResponse sdkResponse = upstoxSdkService
                            .getOhlc(batchKeys, upstoxInterval);
                    if (sdkResponse != null && sdkResponse.getData() != null && !sdkResponse.getData().isEmpty()) {
                        // Map SDK response to OHLCResponse model structure used below
                        response = sdkResponse;
                    }
                } catch (Exception e) {
                    log.warn("getOHLC",
                            "Failed to fetch OHLC batch via SDK Service, falling back to API Service: " + e.getMessage());
                }

                // Fallback to API Service if SDK failed or returned empty
                if (response == null || response.getData() == null || response.getData().isEmpty()) {
                    response = upstoxApiService.getOhlc(batchKeys, upstoxInterval);
                }

                if (response != null && response.getData() != null) {
                    for (Map.Entry<String, OHLCResponse.OHLCData> entry : response.getData().entrySet()) {
                        String instrumentKey = entry.getKey();
                        OHLCResponse.OHLCData data = entry.getValue();

                        // Map back to symbol if possible, otherwise use key
                        String symbol = context.getSymbol(instrumentKey);

                        OHLCQuote quote = new OHLCQuote();
                        // Use getters as fields might be mapped differently or computed
                        quote.setLastPrice(data.getLast_price() != null ? data.getLast_price() : 0.0);

                        if (data.getOhlc() != null) {
                            OHLCQuote.OHLC ohlc = new OHLCQuote.OHLC();
                            ohlc.setOpen(data.getOhlc().getOpen());
                            ohlc.setHigh(data.getOhlc().getHigh());
                            ohlc.setLow(data.getOhlc().getLow());
                            ohlc.setClose(data.getOhlc().getClose());
                            quote.setOhlc(ohlc);
                        }

                        // Also set previous close if available in data
                        if (data.getPrevious_close() != null) {
                            log.debug("getOHLC",
                                    String.format("Setting Previous Close for %s: %s", symbol, data.getPrevious_close()));
                            quote.setPreviousClose(data.getPrevious_close());
                        } else {
                            log.debug("getOHLC", "No Previous Close found in mapped data for " + symbol);
                        }

                        result.put(symbol, quote);
                    }
                }
                
                // Add a delay between batches to respect rate limits, but not after the final batch
                if (end < allKeys.size()) {
                    try {
                        Thread.sleep(BATCH_DELAY_MS);
                    } catch (InterruptedException ie) {
                        log.error("getOHLC", "Batching sleep interrupted", ie);
                        Thread.currentThread().interrupt();
                    }
                }
            }

            // Backfill previousClose for any symbol where Upstox OHLC API returned prevOhlc=null.
            // This happens outside market hours (after 3:30 PM IST) or on weekends.
            // We fetch the last 2 days of historical candles and use the penultimate candle's close.
            backfillPreviousClose(result, context);
            return result;
            
        } catch (Exception e) {
            log.error("getOHLC", "Error fetching Upstox OHLC", e);
            return new HashMap<>();
        }
    }

    /**
     * Backfills previousClose for any symbol where the Upstox OHLC market-quote API
     * returned prevOhlc=null. This happens outside market hours (after 3:30 PM IST).
     * Fetches 2 days of daily historical candles and uses the prior day's close.
     */
    private void backfillPreviousClose(
            Map<String, OHLCQuote> result,
            com.am.marketdata.provider.common.InstrumentContext context) {
        try {
            // Find symbols that still have previousClose == 0.0
            List<String> symbolsNeedingPrevClose = result.entrySet().stream()
                    .filter(e -> e.getValue().getPreviousClose() == 0.0)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (symbolsNeedingPrevClose.isEmpty()) {
                return;
            }

            // 1. Try to load previousClose from local database in batch (very fast < 10ms)
            // OPTIMIZATION: Bypass database check if we only need a few symbols (<= 5)
            // because MongoDB connections can time out taking 10 seconds.
            if (symbolsNeedingPrevClose.size() > 5) {
                try {
                    com.am.common.investment.service.EquityService equityService =
                            com.am.marketdata.common.util.ApplicationContextProvider.getBean(com.am.common.investment.service.EquityService.class);
                    if (equityService != null) {
                        List<com.am.common.investment.model.equity.EquityPrice> dbPrices =
                                equityService.getPricesByTradingSymbols(symbolsNeedingPrevClose);
                        if (dbPrices != null) {
                            for (com.am.common.investment.model.equity.EquityPrice dbPrice : dbPrices) {
                                if (dbPrice.getPreviousClose() != null && dbPrice.getPreviousClose() > 0) {
                                    String symbol = dbPrice.getSymbol();
                                    if (result.containsKey(symbol)) {
                                        result.get(symbol).setPreviousClose(dbPrice.getPreviousClose());
                                        log.debug("backfillPreviousClose", "Loaded previousClose from DB for " + symbol + ": " + dbPrice.getPreviousClose());
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception dbEx) {
                    log.warn("backfillPreviousClose", "Failed to retrieve previousClose from database: " + dbEx.getMessage());
                }
            } else {
                log.info("backfillPreviousClose", "Bypassing database query for previousClose because count is small ({}) to prevent timeouts", symbolsNeedingPrevClose.size());
            }

            // 2. Filter remaining symbols that still have previousClose == 0.0 for API fallback
            List<String> remainingSymbols = result.entrySet().stream()
                    .filter(e -> e.getValue().getPreviousClose() == 0.0)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (remainingSymbols.isEmpty()) {
                return;
            }

            log.info("backfillPreviousClose",
                    "Backfilling previousClose via historical API for {} remaining symbols: {}",
                    remainingSymbols.size(), remainingSymbols);

            java.time.LocalDate today = java.time.LocalDate.now();
            String toDate = today.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
            // Fetch 5 calendar days back to safely cover weekends/holidays
            String fromDate = today.minusDays(5).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);

            int callCount = 0;
            for (String symbol : remainingSymbols) {
                try {
                    if (callCount > 0) {
                        try {
                            Thread.sleep(100); // Respect Upstox rate limits (10 requests/sec)
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.warn("backfillPreviousClose", "Interrupted during backfill rate-limit sleep");
                            break;
                        }
                    }
                    callCount++;

                    // Resolve instrument key for this symbol, stripping exchange prefixes if present
                    String cleanSymbol = symbol.replace("NSE_EQ:", "").replace("NSE:", "").trim();
                    String instrumentKey = context.keyToSymbolMap.entrySet().stream()
                            .filter(e -> e.getValue().equals(cleanSymbol) || e.getValue().equals(symbol))
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElse(null);

                    if (instrumentKey == null) {
                        log.warn("backfillPreviousClose",
                                "Could not find instrument key for symbol: {} (cleaned: {}), skipping", symbol, cleanSymbol);
                        continue;
                    }

                     // Encode the instrument key to handle symbols containing special characters (like '&' in M&M).
                     // Unencoded special characters act as query parameter separators in HTTP requests,
                     // truncating the symbol key and returning empty results.
                     String encodedKey = instrumentKey;
                     try {
                         encodedKey = java.net.URLEncoder.encode(instrumentKey, java.nio.charset.StandardCharsets.UTF_8.toString());
                     } catch (java.io.UnsupportedEncodingException uee) {
                         log.error("backfillPreviousClose", "Failed to URL-encode instrument key " + instrumentKey, uee);
                     }

                     com.am.marketdata.provider.upstox.model.HistoricalDataResponse histResponse =
                             upstoxSdkService.getHistoricalCandleData(encodedKey, "day", 1, toDate, fromDate);

                    if (histResponse != null && histResponse.getData() != null
                            && histResponse.getData().getCandles() != null
                            && !histResponse.getData().getCandles().isEmpty()) {

                        java.util.List<java.util.List<Object>> candles = histResponse.getData().getCandles();
                        double prevClose = 0.0;
                        if (!candles.isEmpty()) {
                            java.util.List<Object> newestCandle = candles.get(0);
                            String candleDateStr = newestCandle.get(0) != null ? newestCandle.get(0).toString() : "";
                            
                            // Compare candle date against today's date in Asia/Kolkata timezone
                            java.time.LocalDate todayKolkata = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));
                            String todayStr = todayKolkata.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
                            boolean isTodayCandle = !candleDateStr.isEmpty() && candleDateStr.startsWith(todayStr);
                            
                            // Self-correcting check: If the live quote's open and close exactly match the newest candle's open and close,
                            // it means the live quote is still showing that newest candle's day (e.g., today is a weekend or market holiday).
                            // In this case, the true previous close must be the next older candle in the list (index 1).
                            boolean matchesNewestCandle = false;
                            OHLCQuote liveQuote = result.get(symbol);
                            if (liveQuote != null && liveQuote.getOhlc() != null && newestCandle.size() >= 5) {
                                double liveOpen = liveQuote.getOhlc().getOpen();
                                double liveClose = liveQuote.getOhlc().getClose();
                                double candleOpen = parseDouble(newestCandle.get(1));
                                double candleClose = parseDouble(newestCandle.get(4));
                                if (liveOpen == candleOpen && liveClose == candleClose) {
                                    matchesNewestCandle = true;
                                }
                            }

                            if ((isTodayCandle || matchesNewestCandle) && candles.size() >= 2) {
                                // Index 0 represents today's trading candle (or the last active trading day's candle on a holiday/weekend);
                                // previous close is yesterday's (or the prior trading day's) candle at index 1
                                java.util.List<Object> prevCandle = candles.get(1);
                                if (prevCandle != null && prevCandle.size() >= 5) {
                                    Object closeObj = prevCandle.get(4);
                                    if (closeObj instanceof Number) {
                                        prevClose = ((Number) closeObj).doubleValue();
                                    }
                                }
                            } else {
                                // Index 0 represents yesterday's (or older) candle; it is the correct previous close
                                if (newestCandle.size() >= 5) {
                                    Object closeObj = newestCandle.get(4);
                                    if (closeObj instanceof Number) {
                                        prevClose = ((Number) closeObj).doubleValue();
                                    }
                                }
                            }
                        }

                        if (prevClose > 0) {
                            result.get(symbol).setPreviousClose(prevClose);
                            log.info("backfillPreviousClose",
                                    "Backfilled previousClose for {}: {}", symbol, prevClose);
                        } else {
                            log.warn("backfillPreviousClose",
                                    "Could not extract valid previousClose from historical candles for {}", symbol);
                        }
                    } else {
                        log.warn("backfillPreviousClose",
                                "No historical candle data returned for symbol: {}", symbol);
                    }
                } catch (Exception ex) {
                    log.error("backfillPreviousClose",
                            "Failed to backfill previousClose for symbol {}: {}", symbol, ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("backfillPreviousClose", "Error in backfillPreviousClose: {}", e.getMessage());
        }
    }

    @Override
    public Map<String, LTPQuote> getLTP(String[] symbols) {
        try {
            com.am.marketdata.provider.common.InstrumentContext context = symbolResolver
                    .resolveContext(Arrays.asList(symbols));

            if (context.instrumentKeys.isEmpty()) {
                return new HashMap<>();
            }

            // Log for debugging
            log.info("getLTP", "Fetching LTP for keys: " + context.instrumentKeys);

            Map<String, LTPQuote> result = new HashMap<>();
            List<String> allKeys = context.instrumentKeys;
            
            log.info("getLTP", String.format("Fetching LTP quotes in batches of %d to comply with API limits", BATCH_SIZE));
            
            for (int i = 0; i < allKeys.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, allKeys.size());
                List<String> batchKeys = allKeys.subList(i, end);
                
                log.info("getLTP", String.format("Fetching LTP batch: %d to %d", i, end));

                GetMarketQuoteLastTradedPriceResponseV3 response = null;
                try {
                    response = upstoxSdkService.getLtp(batchKeys);
                } catch (Exception e) {
                    log.warn("getLTP", "Failed to fetch LTP batch via SDK Service, falling back to API Service: " + e.getMessage());
                }

                if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                    for (Map.Entry<String, MarketQuoteSymbolLtpV3> entry : response.getData().entrySet()) {
                        String instrumentKey = entry.getKey();
                        MarketQuoteSymbolLtpV3 data = entry.getValue();

                        // Map back to symbol using the context map
                        String symbol = context.getSymbol(instrumentKey);

                        LTPQuote quote = new LTPQuote();
                        quote.lastPrice = data.getLastPrice();
                        quote.instrumentToken = 0;

                        result.put(symbol, quote);
                    }
                } else {
                    com.am.marketdata.provider.upstox.model.MarketQuoteResponse apiResponse = upstoxApiService.getLtp(batchKeys);
                    if (apiResponse != null && apiResponse.getData() != null) {
                        for (Map.Entry<String, com.am.marketdata.provider.upstox.model.common.StockQuote> entry : apiResponse.getData().entrySet()) {
                            String instrumentKey = entry.getKey();
                            com.am.marketdata.provider.upstox.model.common.StockQuote data = entry.getValue();

                             String symbol = context.getSymbol(instrumentKey);

                            LTPQuote quote = new LTPQuote();
                            quote.lastPrice = data.getLastPrice() != null ? data.getLastPrice() : 0.0;
                            quote.instrumentToken = 0;

                            result.put(symbol, quote);
                        }
                    }
                }
                
                // Add a delay between batches to respect rate limits, but not after the final batch
                if (end < allKeys.size()) {
                    try {
                        Thread.sleep(BATCH_DELAY_MS);
                    } catch (InterruptedException ie) {
                        log.error("getLTP", "Batching sleep interrupted", ie);
                        Thread.currentThread().interrupt();
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.error("getLTP", "Error fetching Upstox LTP", e);
            return new HashMap<>();
        }
    }

    @Override
    public HistoricalData getHistoricalData(String symbol, Date from, Date to, TimeFrame interval, boolean continuous,
            Map<String, Object> additionalParams) {
        // Validation 1: Symbol must not be empty
        if (symbol == null || symbol.trim().isEmpty()) {
            log.warn("getHistoricalData", "Symbol cannot be null or empty");
            return new HistoricalData();
        }

        // Validation 2: From date must be less than To date
        if (from != null && to != null && from.after(to)) {
            log.warn("getHistoricalData", "From date (" + from + ") cannot be after To date (" + to + ")");
            return new HistoricalData();
        }

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String fromDateStr = dateFormat.format(from);
            String toDateStr = dateFormat.format(to);

            boolean requiresAggregation = false;
            int targetMinutes = 1;

            if (interval == TimeFrame.FIVE_MINUTE) {
                requiresAggregation = true;
                targetMinutes = 5;
            } else if (interval == TimeFrame.TEN_MINUTE) {
                requiresAggregation = true;
                targetMinutes = 10;
            } else if (interval == TimeFrame.FIFTEEN_MINUTE) {
                requiresAggregation = true;
                targetMinutes = 15;
            } else if (interval == TimeFrame.HOUR) {
                requiresAggregation = true;
                targetMinutes = 60;
            }

            String v3Unit = requiresAggregation ? "minutes" : mapToUpstoxV3Unit(interval);
            int intervalValue = requiresAggregation ? 1 : getUpstoxIntervalValue(interval);

            // Resolve instrument key first as SDK works with keys
            List<String> symbolsList = Collections.singletonList(symbol);
            com.am.marketdata.provider.common.InstrumentContext context = symbolResolver.resolveContext(symbolsList);
            String instrumentKey = null;
            if (!context.instrumentKeys.isEmpty()) {
                instrumentKey = context.instrumentKeys.get(0);
            } else {
                log.warn("getHistoricalData", "Could not resolve instrument key for historical data symbol: " + symbol
                        + ". Using symbol as key fallback.");
                instrumentKey = symbol;
            }

            // The Upstox SDK handles path parameter encoding internally.
            // Do not URL-encode instrumentKey here to avoid double-encoding (e.g. '|' escaping to '%7C' then '%257C' in HTTP call).
            String cleanKey = instrumentKey;

            HistoricalDataResponse response;
            try {
                log.info("getHistoricalData",
                        "Fetching historical data via SDK for instrument key: " + instrumentKey + ", unit: " + v3Unit
                                + ", interval: " + intervalValue + ", from: " + fromDateStr + ", to: " + toDateStr);

                response = upstoxSdkService.getHistoricalCandleData(
                        cleanKey, v3Unit, intervalValue, toDateStr, fromDateStr);
            } catch (Exception e) {
                log.error("getHistoricalData", "Failed to fetch historical data via SDK: " + e.getMessage(), e);
                return new HistoricalData();
            }

            // Map to Common HistoricalData model
            HistoricalData historicalData = new HistoricalData();
            if (response != null && response.getData() != null && response.getData().getCandles() != null) {
                List<OHLCVTPoint> dataPoints = new ArrayList<>();
                for (List<Object> rawCandle : response.getData().getCandles()) {
                    // candle structure: [timestamp, open, high, low, close, vol, oi]
                    if (rawCandle == null || rawCandle.size() < 5)
                        continue;

                    OHLCVTPoint point = new OHLCVTPoint();
                    try {
                        // Index 0: Timestamp (String)
                        String timestamp = (String) rawCandle.get(0);
                        if (timestamp != null) {
                            // Upstox sample: "2024-04-12T00:00:00+05:30"
                            // Using Instant parser for ISO 8601 strings
                            java.time.Instant instant = java.time.Instant
                                    .parse(timestamp.replace("+0530", "+05:30"));
                            point.setTime(java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault()));
                        } else {
                            point.setTime(java.time.LocalDateTime.now());
                        }
                    } catch (Exception e) {
                        log.warn("getHistoricalData", "Error parsing candle timestamp: " + e.getMessage());
                        point.setTime(java.time.LocalDateTime.now());
                    }

                    try {
                        // Parse values safely handling potential Integer/Double types from JSON
                        point.setOpen(parseDouble(rawCandle.get(1)));
                        point.setHigh(parseDouble(rawCandle.get(2)));
                        point.setLow(parseDouble(rawCandle.get(3)));
                        point.setClose(parseDouble(rawCandle.get(4)));

                        if (rawCandle.size() > 5) {
                            point.setVolume(parseLong(rawCandle.get(5)));
                        } else {
                            point.setVolume(0L);
                        }
                    } catch (Exception e) {
                        log.warn("getHistoricalData", "Error parsing candle data points: " + e.getMessage());
                        continue;
                    }

                    dataPoints.add(point);
                }

                if (requiresAggregation) {
                    List<OHLCVTPoint> aggregatedPoints = aggregateCandles(dataPoints, targetMinutes);
                    historicalData.setDataPoints(aggregatedPoints);
                    log.info("getHistoricalData", "Aggregated " + dataPoints.size() + " 1m candles into "
                            + aggregatedPoints.size() + " " + interval.getApiValue() + " candles.");
                } else {
                    historicalData.setDataPoints(dataPoints);
                }
            }
            return historicalData;
        } catch (Exception e) {
            log.error("getHistoricalData", "Error fetching Upstox historical data", e);
            return new HistoricalData();
        }
    }

    private List<OHLCVTPoint> aggregateCandles(List<OHLCVTPoint> oneMinCandles, int targetMinutes) {
        if (oneMinCandles == null || oneMinCandles.isEmpty()) {
            return Collections.emptyList();
        }

        // Sort candles by timestamp ascending to ensure correct aggregation order
        oneMinCandles.sort(Comparator.comparing(OHLCVTPoint::getTime));

        List<OHLCVTPoint> aggregated = new ArrayList<>();
        int i = 0;
        int n = oneMinCandles.size();

        while (i < n) {
            OHLCVTPoint first = oneMinCandles.get(i);
            java.time.LocalDateTime bucketStart = first.getTime();

            double open = first.getOpen();
            double high = first.getHigh();
            double low = first.getLow();
            double close = first.getClose();
            long volume = first.getVolume();

            int count = 1;
            i++;
            while (i < n && count < targetMinutes) {
                OHLCVTPoint next = oneMinCandles.get(i);
                long diffMinutes = java.time.Duration.between(bucketStart, next.getTime()).toMinutes();
                if (diffMinutes >= targetMinutes) {
                    break;
                }

                high = Math.max(high, next.getHigh());
                low = Math.min(low, next.getLow());
                close = next.getClose();
                volume += next.getVolume();

                i++;
                count++;
            }

            OHLCVTPoint aggregatedPoint = new OHLCVTPoint();
            aggregatedPoint.setTime(bucketStart);
            aggregatedPoint.setOpen(open);
            aggregatedPoint.setHigh(high);
            aggregatedPoint.setLow(low);
            aggregatedPoint.setClose(close);
            aggregatedPoint.setVolume(volume);

            aggregated.add(aggregatedPoint);
        }

        return aggregated;
    }

    private int getUpstoxIntervalValue(TimeFrame interval) {
        if (interval == null)
            return 1;
        switch (interval) {
            case MINUTE:
                return 1;
            case FIVE_MINUTE:
                return 5;
            case TEN_MINUTE:
                return 10;
            case FIFTEEN_MINUTE:
                return 15;
            case THIRTY_MINUTE:
                return 30;
            case HOUR:
                return 1;
            case DAY:
                return 1;
            case WEEK:
                return 1;
            case MONTH:
                return 1;
            default:
                return 1;
        }
    }

    /**
     * V3 historical candle path unit ({@code days}, {@code minutes}, …).
     * See https://upstox.com/developer/api-documentation/v3/get-historical-candle-data/
     */
    private String mapToUpstoxV3Unit(TimeFrame interval) {
        if (interval == null) {
            return "day";
        }
        switch (interval) {
            case MINUTE:
            case FIVE_MINUTE:
            case TEN_MINUTE:
            case FIFTEEN_MINUTE:
            case THIRTY_MINUTE:
                return "minute";
            case HOUR:
                return "hour";
            case DAY:
                return "day";
            case WEEK:
                return "week";
            case MONTH:
                return "month";
            default:
                return "day";
        }
    }

    @Override
    public Object initializeTicker(List<String> symbolIds, Object tickListener) {
        return null; // Not implemented for Upstox yet
    }

    @Override
    public boolean isTickerConnected() {
        return false;
    }

    @Override
    public List<Instrument> getAllInstruments() {
        return new ArrayList<>(); // Return empty list of Zerodha Instruments
    }

    @Override
    public List<Object> getSymbolsForExchange(String exchange) {
        return new ArrayList<>();
    }

    @Override
    public <T> CompletableFuture<T> executeAsync(ProviderOperation<T> operation) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean logout() {
        return true;
    }

    private Double parseDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            return Double.parseDouble((String) value);
        }
        return 0.0;
    }

    private Long parseLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        return 0L;
    }

    @Override
    public String getProviderName() {
        return "upstox";
    }
}
