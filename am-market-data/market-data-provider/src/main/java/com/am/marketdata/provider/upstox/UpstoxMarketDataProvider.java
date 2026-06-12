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

@Service("upstoxMarketDataProvider")
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

            log.info("backfillPreviousClose",
                    "Backfilling previousClose via historical API for {} symbols: {}",
                    symbolsNeedingPrevClose.size(), symbolsNeedingPrevClose);

            java.time.LocalDate today = java.time.LocalDate.now();
            String toDate = today.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
            // Fetch 5 calendar days back to safely cover weekends/holidays
            String fromDate = today.minusDays(5).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);

            for (String symbol : symbolsNeedingPrevClose) {
                try {
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

                    com.am.marketdata.provider.upstox.model.HistoricalDataResponse histResponse =
                            upstoxSdkService.getHistoricalCandleData(instrumentKey, "days", 1, toDate, fromDate);

                    if (histResponse != null && histResponse.getData() != null
                            && histResponse.getData().getCandles() != null
                            && !histResponse.getData().getCandles().isEmpty()) {

                        java.util.List<java.util.List<Object>> candles = histResponse.getData().getCandles();
                        // Candles are in descending order (newest first): [0]=today, [1]=yesterday
                        // If today's market is closed, [0] is yesterday's candle
                        // We want the close of the candle BEFORE the most recent one
                        double prevClose = 0.0;
                        if (candles.size() >= 2) {
                            // Use index 1 (the day before the most recent candle)
                            java.util.List<Object> prevCandle = candles.get(1);
                            if (prevCandle != null && prevCandle.size() >= 5) {
                                Object closeObj = prevCandle.get(4);
                                if (closeObj instanceof Number) {
                                    prevClose = ((Number) closeObj).doubleValue();
                                }
                            }
                        } else if (candles.size() == 1) {
                            // Only one candle available — use it as a best-effort fallback
                            java.util.List<Object> onlyCandle = candles.get(0);
                            if (onlyCandle != null && onlyCandle.size() >= 5) {
                                Object closeObj = onlyCandle.get(4);
                                if (closeObj instanceof Number) {
                                    prevClose = ((Number) closeObj).doubleValue();
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

            String v3Unit = mapToUpstoxV3Unit(interval);
            int intervalValue = getUpstoxIntervalValue(interval);

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

            HistoricalDataResponse response;
            try {
                log.info("getHistoricalData",
                        "Fetching historical data via SDK for instrument key: " + instrumentKey + ", unit: " + v3Unit
                                + ", interval: " + intervalValue + ", from: " + fromDateStr + ", to: " + toDateStr);

                response = upstoxSdkService.getHistoricalCandleData(
                        instrumentKey, v3Unit, intervalValue, toDateStr, fromDateStr);
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
                historicalData.setDataPoints(dataPoints);
            }

            return historicalData;
        } catch (Exception e) {
            log.error("getHistoricalData", "Error fetching Upstox historical data", e);
            return new HistoricalData();
        }
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
            return "days";
        }
        switch (interval) {
            case MINUTE:
            case FIVE_MINUTE:
            case TEN_MINUTE:
            case FIFTEEN_MINUTE:
            case THIRTY_MINUTE:
                return "minutes";
            case HOUR:
                return "hours";
            case DAY:
                return "days";
            case WEEK:
                return "weeks";
            case MONTH:
                return "months";
            default:
                return "days";
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
