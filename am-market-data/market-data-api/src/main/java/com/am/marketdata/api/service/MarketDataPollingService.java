package com.am.marketdata.api.service;

import com.am.marketdata.api.websocket.MarketDataWebSocketHandler;
import com.am.marketdata.provider.common.MarketDataProviderFactory;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.api.model.MarketDataUpdate;
import com.am.marketdata.api.model.StreamConnectRequest;
import com.am.marketdata.api.model.StreamConnectResponse;
import com.am.marketdata.api.util.InstrumentUtils;
import com.am.marketdata.api.model.HistoricalDataResponseV1;
import com.am.marketdata.api.model.HistoricalDataMetadata;
import com.am.common.investment.model.historical.HistoricalData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class MarketDataPollingService {

    private final MarketDataFetchService marketDataFetchService;
    private final MarketDataWebSocketHandler webSocketHandler;
    private final InstrumentUtils instrumentUtils;
    private final MarketDataProviderFactory marketDataProviderFactory;

    // Scheduler for polling
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final Map<String, ScheduledFuture<?>> activeStreams = new ConcurrentHashMap<>();

    /**
     * Helper to resolve symbols using InstrumentUtils
     */
    private Set<String> resolveSymbols(List<String> keys, boolean expandIndices) {
        return instrumentUtils.resolveSymbols(keys, expandIndices);
    }

    @org.springframework.beans.factory.annotation.Value("${market-data.stream.poll-interval-seconds:10}")
    private int pollIntervalSeconds;

    public void connectStream(java.util.List<String> instrumentKeys, String modeStr, String provider, String timeFrame,
            Boolean isIndexSymbol) {

        // Orchestration Step 1: Resolve Symbols (Common Logic)
        Set<String> resolvedSymbols = resolveSymbols(instrumentKeys, false);
        log.info(
                "Initiating stream simulation via polling for {} instruments (resolved from {}). Provider: {}, TimeFrame: {}, IsIndexSymbol: {}",
                resolvedSymbols.size(), instrumentKeys.size(), provider, timeFrame, isIndexSymbol);

        String providerKey = provider != null ? provider.toUpperCase() : "UNKNOWN";
        final String finalTimeFrame = timeFrame != null ? timeFrame : "1D";

        // Cancel existing stream if any for this provider
        disconnectStream(providerKey);

        Runnable pollingTask = () -> {
            try {
                // Orchestration delegated to fetchMarketDataUpdate
                MarketDataUpdate update = fetchMarketDataUpdate(
                        resolvedSymbols,
                        finalTimeFrame,
                        isIndexSymbol,
                        providerKey);

                if (update != null) {
                    webSocketHandler.broadcast(update);
                }
            } catch (Exception e) {
                log.error("Error during polling stream execution for provider {}", providerKey, e);
            }
        };

        // Schedule task - use configured interval
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(pollingTask, 0, pollIntervalSeconds,
                TimeUnit.SECONDS);
        activeStreams.put(providerKey, future);
        log.info("Polling stream started for provider: {} with interval: {} seconds", providerKey, pollIntervalSeconds);
    }

    public StreamConnectResponse initiateStream(
            StreamConnectRequest request) {
        // Use expandIndices from request (defaults to false if not provided)
        boolean expandIndices = request.getExpandIndices() != null ? request.getExpandIndices() : false;

        // Orchestration Step 1: Resolve Symbols
        java.util.Set<String> resolvedSymbols = resolveSymbols(request.getInstrumentKeys(), expandIndices);

        log.info("Resolved {} symbols to {} for stream initiation",
                request.getInstrumentKeys().size(), resolvedSymbols.size());

        // Get active provider from factory
        String provider = marketDataProviderFactory.getProvider().getProviderName().toUpperCase();
        String timeFrame = request.getTimeFrame() != null ? request.getTimeFrame() : "1D";

        boolean shouldStream = request.getStream() == null || request.getStream();

        // Start the background stream ONLY if requested
        if (shouldStream) {
            connectStream(
                    new ArrayList<>(resolvedSymbols),
                    request.getMode(),
                    provider,
                    timeFrame,
                    request.getIsIndexSymbol() != null ? request.getIsIndexSymbol() : false);
        } else {
            log.info("Stream flag is false. Skipping background stream initiation for symbols: {}", resolvedSymbols);
        }

        // Fetch initial data synchronously to return in response
        MarketDataUpdate initialData = fetchMarketDataUpdate(
                resolvedSymbols,
                timeFrame,
                request.getIsIndexSymbol(),
                provider);

        String message = shouldStream
                ? "Stream connection initiated successfully with timeFrame: " + timeFrame
                : "One-time data fetch successful with timeFrame: " + timeFrame;

        return StreamConnectResponse.builder()
                .status("SUCCESS")
                .message(message)
                .data(initialData)
                .build();
    }

    public MarketDataUpdate fetchMarketDataUpdate(
            Set<String> keys, String timeFrame, Boolean isIndexSymbol, String providerKey) {
        try {
            // Orchestration Step 2 & 3: Parallel Execution of Data Fetching

            // Task 1: Fetch Live OHLC Data
            CompletableFuture<Map<String, OHLCQuote>> liveDataFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return marketDataFetchService.getOHLC(keys, false, TimeFrame.DAY, false);
                } catch (Exception e) {
                    log.error("Error fetching live OHLC data", e);
                    return new HashMap<>();
                }
            });

            // Task 2: Fetch Historical Data (if applicable)
            CompletableFuture<HistoricalDataResponseV1> historicalDataFuture;
            if ("1D".equalsIgnoreCase(timeFrame) || "1W".equalsIgnoreCase(timeFrame)
                    || "1M".equalsIgnoreCase(timeFrame)) {
                historicalDataFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        return fetchHistoricalData(keys, timeFrame, isIndexSymbol);
                    } catch (Exception e) {
                        log.error("Error fetching historical data", e);
                        return HistoricalDataResponseV1.builder().build();
                    }
                });
            } else {
                historicalDataFuture = CompletableFuture.completedFuture(HistoricalDataResponseV1.builder().build());
            }

            // Wait for both tasks to complete
            CompletableFuture.allOf(liveDataFuture, historicalDataFuture).join();

            // Get results
            Map<String, OHLCQuote> liveOhlcData = liveDataFuture.get();
            HistoricalDataResponseV1 historicalResponse = historicalDataFuture.get();

            // Orchestration Step 4: Business Calculation (Merge Data)
            Map<String, OHLCQuote> enrichedData = mergeData(liveOhlcData, historicalResponse);

            // Step 3: Build update object
            if (enrichedData != null && !enrichedData.isEmpty()) {
                // Orchestration Step 5: Response Mapping
                Map<String, MarketDataUpdate.QuoteChange> quoteUpdates = buildQuoteUpdates(
                        enrichedData);

                return MarketDataUpdate.builder()
                        .timestamp(System.currentTimeMillis())
                        .quotes(quoteUpdates)
                        .build();
            }
        } catch (Exception e) {
            log.error("Error fetching market data update", e);
        }
        return null;
    }

    public void disconnectStream(String provider) {
        if (provider == null)
            return;
        String key = provider.toUpperCase();
        if (activeStreams.containsKey(key)) {
            ScheduledFuture<?> future = activeStreams.get(key);
            if (future != null && !future.isCancelled()) {
                future.cancel(true);
            }
            activeStreams.remove(key);
            log.info("Polling stream stopped for provider: {}", provider);
        }
    }

    /**
     * Fetches historical data based on timeFrame
     */
    /**
     * Fetches historical data based on timeFrame
     */
    private HistoricalDataResponseV1 fetchHistoricalData(Set<String> symbols,
            String timeFrame, Boolean isIndexSymbol) {
        // Calculate historical date range based on timeFrame
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate historicalDate;

        java.time.LocalDate startDate;

        switch (timeFrame.toUpperCase()) {
            case "1D":
            case "DAY":
                historicalDate = today.minusDays(1);
                startDate = historicalDate.minusDays(7); // Lookback 7 days to cover weekends/holidays
                break;
            case "1W":
            case "WEEK":
                historicalDate = today.minusWeeks(1);
                startDate = historicalDate.minusWeeks(4);
                break;
            case "1M":
            case "MONTH":
                historicalDate = today.minusMonths(1);
                startDate = historicalDate.minusMonths(6);
                break;
            default:
                historicalDate = today.minusDays(1);
                startDate = historicalDate.minusDays(7);
        }

        String historicalDateStr = historicalDate.toString();

        // Convert LocalDate to Date for API call
        // Convert LocalDate to Date for API call
        java.util.Date fromDate = java.util.Date.from(
                startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        java.util.Date toDate = java.util.Date.from(
                historicalDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());

        log.info("Fetching historical data for {} symbols from {} (timeFrame: {}) from: {} to: {}",
                symbols.size(), historicalDateStr, timeFrame, fromDate, toDate);

        Map<String, Object> additionalParams = new HashMap<>();
        if (isIndexSymbol != null && isIndexSymbol) {
            additionalParams.put("isIndexSymbol", true);
        }

        return marketDataFetchService.getHistoricalDataMultipleSymbols(
                symbols,
                fromDate,
                toDate,
                TimeFrame.DAY,
                "STOCK",
                additionalParams,
                false,
                false); // fetchIndexStocks = false (keep symbols as-is for polling)
    }

    /**
     * Merges historical data into live OHLC data
     */
    private Map<String, OHLCQuote> mergeData(Map<String, OHLCQuote> liveData,
            HistoricalDataResponseV1 historicalResponse) {
        Map<String, OHLCQuote> enrichedData = new HashMap<>();

        if (liveData == null || liveData.isEmpty()) {
            return enrichedData;
        }

        for (Map.Entry<String, OHLCQuote> entry : liveData.entrySet()) {
            String symbol = entry.getKey();
            OHLCQuote liveQuote = entry.getValue();

            double previousClose = liveQuote.getPreviousClose();

            // Extract historical data from response
            if (historicalResponse != null && historicalResponse.getData() != null) {
                Map<String, HistoricalData> symbolsData = historicalResponse.getData();

                if (symbolsData != null && symbolsData.containsKey(symbol)) {
                    HistoricalData historicalData = symbolsData.get(symbol);
                    if (historicalData != null && historicalData.getDataPoints() != null
                            && !historicalData.getDataPoints().isEmpty()) {
                        var dataPoints = historicalData.getDataPoints();
                        var lastPoint = dataPoints.get(dataPoints.size() - 1);
                        if (lastPoint.getClose() > 0) {
                            previousClose = lastPoint.getClose();
                            log.debug("Updated previous close for {}: {} (from HistoricalData object)", symbol,
                                    previousClose);
                        }
                    }
                }
            }

            // Build enriched quote with previous close from historical data
            OHLCQuote enrichedQuote = OHLCQuote.builder()
                    .lastPrice(liveQuote.getLastPrice())
                    .previousClose(previousClose)
                    .ohlc(liveQuote.getOhlc())
                    .build();

            enrichedData.put(symbol, enrichedQuote);
        }
        return enrichedData;
    }

    /**
     * Builds QuoteChange objects from OHLC quotes
     * Separated for better maintainability
     */
    private Map<String, com.am.marketdata.api.model.MarketDataUpdate.QuoteChange> buildQuoteUpdates(
            Map<String, OHLCQuote> ohlcQuotes) {

        Map<String, MarketDataUpdate.QuoteChange> quoteUpdates = new HashMap<>();

        for (Map.Entry<String, OHLCQuote> entry : ohlcQuotes.entrySet()) {
            String symbol = entry.getKey();
            OHLCQuote quote = entry.getValue();

            double lastPrice = quote.getLastPrice();
            double open = 0.0;
            double high = 0.0;
            double low = 0.0;
            double close = 0.0;

            // Extract OHLC data if available
            if (quote.getOhlc() != null) {
                open = quote.getOhlc().getOpen();
                high = quote.getOhlc().getHigh();
                low = quote.getOhlc().getLow();
                close = quote.getOhlc().getClose();
            }

            double prevClose = quote.getPreviousClose();
            double change = 0.0;
            double changePercent = 0.0;

            if (prevClose > 0) {
                change = lastPrice - prevClose;
                changePercent = (change / prevClose) * 100;

                // Round to 2 decimal places
                change = BigDecimal.valueOf(change).setScale(2, RoundingMode.HALF_UP).doubleValue();
                changePercent = BigDecimal.valueOf(changePercent).setScale(2, RoundingMode.HALF_UP).doubleValue();
            }

            MarketDataUpdate.QuoteChange update = MarketDataUpdate.QuoteChange
                    .builder()
                    .lastPrice(lastPrice)
                    .open(open)
                    .high(high)
                    .low(low)
                    .close(close)
                    .previousClose(prevClose)
                    .change(change)
                    .changePercent(changePercent)
                    .build();

            quoteUpdates.put(symbol, update);
        }

        return quoteUpdates;
    }
}
