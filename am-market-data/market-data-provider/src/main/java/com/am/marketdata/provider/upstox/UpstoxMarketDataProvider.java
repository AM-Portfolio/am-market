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

            OHLCResponse response = null;

            // Try SDK Service first
            try {
                com.am.marketdata.provider.upstox.model.OHLCResponse sdkResponse = upstoxSdkService
                        .getOhlc(context.instrumentKeys, upstoxInterval);
                if (sdkResponse != null && sdkResponse.getData() != null && !sdkResponse.getData().isEmpty()) {
                    // Map SDK response to OHLCResponse model structure used below
                    response = sdkResponse;
                }
            } catch (Exception e) {
                log.warn("getOHLC",
                        "Failed to fetch OHLC via SDK Service, falling back to API Service: " + e.getMessage());
            }

            // Fallback to API Service if SDK failed or returned empty
            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                response = upstoxApiService.getOhlc(context.instrumentKeys, upstoxInterval);
            }

            Map<String, OHLCQuote> result = new HashMap<>();

            if (response != null && response.getData() != null) {
                for (Map.Entry<String, OHLCResponse.OHLCData> entry : response.getData().entrySet()) {
                    String instrumentKey = entry.getKey();
                    OHLCResponse.OHLCData data = entry.getValue();

                    // Map back to symbol if possible, otherwise use key
                    String symbol = context.keyToSymbolMap.getOrDefault(instrumentKey, instrumentKey);

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
            return result;
        } catch (Exception e) {
            log.error("getOHLC", "Error fetching Upstox OHLC", e);
            return new HashMap<>();
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

            GetMarketQuoteLastTradedPriceResponseV3 response = upstoxSdkService.getLtp(context.instrumentKeys);
            Map<String, LTPQuote> result = new HashMap<>();

            if (response != null && response.getData() != null) {
                for (Map.Entry<String, MarketQuoteSymbolLtpV3> entry : response.getData().entrySet()) {
                    String instrumentKey = entry.getKey();
                    MarketQuoteSymbolLtpV3 data = entry.getValue();

                    // Map back to symbol using the context map
                    String symbol = context.keyToSymbolMap.getOrDefault(instrumentKey, instrumentKey);

                    LTPQuote quote = new LTPQuote();
                    quote.lastPrice = data.getLastPrice();
                    quote.instrumentToken = 0;

                    result.put(symbol, quote);
                }
            }
            return result;
        } catch (Exception e) {
            log.error("getLTP", "Error fetching Upstox LTP via SDK Service", e);
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

            String unit = mapToUpstoxInterval(interval);
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

            HistoricalDataResponse response = null;

            // 1. Try SDK Service
            try {
                // Pass RAW instrument key to SDK, do not URL encode it manually as SDK handles
                // it

                log.info("getHistoricalData",
                        "Fetching historical data via SDK for instrument key: " + instrumentKey + ", unit: " + unit
                                + ", interval: " + intervalValue);

                response = upstoxSdkService.getHistoricalCandleData(instrumentKey, unit,
                        intervalValue, toDateStr,
                        fromDateStr);
            } catch (Exception e) {
                log.warn("getHistoricalData", "Failed to fetch historical data via SDK: " +
                        e.getMessage());
            }

            // 2. Try API Service if SDK failed or returned empty
            if (response == null || response.getData() == null || response.getData().getCandles() == null
                    || response.getData().getCandles().isEmpty()) {

                // Encode key for API usage (manual URL construction)
                String encodedKey = instrumentKey;
                try {
                    if (encodedKey != null) {
                        encodedKey = java.net.URLEncoder
                                .encode(encodedKey, java.nio.charset.StandardCharsets.UTF_8.toString())
                                .replace("+", "%20");
                    }
                } catch (Exception ex) {
                    log.error("Failed to encode key for API fallback", ex);
                }

                log.info("getHistoricalData",
                        "Fetching historical data via API for instrument key: " + encodedKey + ", unit: " + unit
                                + ", interval: " + intervalValue + ",from: " + fromDateStr + ", to: " + toDateStr);
                response = upstoxApiService.getHistoricalCandleData(encodedKey, unit, fromDateStr, toDateStr);
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

    private String mapToUpstoxInterval(TimeFrame interval) {
        if (interval == null)
            return "minutes";
        switch (interval) {
            case MINUTE:
                return "minute";
            case FIVE_MINUTE:
                return "minute";
            case FIFTEEN_MINUTE:
                return "minute";
            case THIRTY_MINUTE:
                return "minute";
            case HOUR:
                return "day"; // Upstox historical API might not support 'hour', defaulting to day or checking
                              // docs. usually it's minute/day/week/month.
                              // Wait, user logs show "day" working.
                              // Upstox V2 intervals: 1minute, day, 30minute, etc are part of path? No, path
                              // structure is /historical-candle/{key}/{interval}/{to}/{from}
                              // Interval is string like '1minute', 'day', '30minute'.
                              // But here we return "unit".
                              // Let's stick to user request "Use singular".
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
