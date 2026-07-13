package com.am.marketdata.api.service;

import com.am.marketdata.api.websocket.MarketDataWebSocketHandler;
import com.am.marketdata.provider.common.MarketDataProviderFactory;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.common.model.MarketDataUpdate;
import com.am.marketdata.api.model.StreamConnectRequest;
import com.am.marketdata.api.model.StreamConnectResponse;
import com.am.marketdata.api.util.InstrumentUtils;
import com.am.marketdata.api.model.HistoricalDataResponseV1;
import com.am.common.investment.model.historical.HistoricalData;
import com.am.marketdata.service.MarketHoursService;
import com.am.marketdata.service.websocket.service.StreamerManager;
import com.am.marketdata.service.websocket.processor.MarketDataProcessor;

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
    private final MarketHoursService marketHoursService;
    private final StreamerManager streamerManager;
    private final MarketDataMockService mockService;
    private final MarketDataProcessor processor;

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
        connectStream(instrumentKeys, modeStr, provider, timeFrame, isIndexSymbol, false);
    }

    public void connectStream(java.util.List<String> instrumentKeys, String modeStr, String provider, String timeFrame,
            Boolean isIndexSymbol, boolean mockMode) {
        Set<String> resolvedSymbols = resolveSymbols(instrumentKeys, false);
        connectStream(resolvedSymbols, modeStr, provider, timeFrame, isIndexSymbol, mockMode);
    }

    /**
     * Starts scheduled polling for resolved symbols (used by initiateStream after index expansion).
     */
    void connectStream(Set<String> resolvedSymbols, String modeStr, String provider, String timeFrame,
            Boolean isIndexSymbol, boolean mockMode) {

        if (resolvedSymbols == null || resolvedSymbols.isEmpty()) {
            log.warn("connectStream called with no symbols; provider={}", provider);
            return;
        }

        log.info(
                "Initiating stream simulation via polling for {} instruments. Provider: {}, TimeFrame: {}, IsIndexSymbol: {}, MockMode: {}",
                resolvedSymbols.size(), provider, timeFrame, isIndexSymbol, mockMode);

        String providerKey = provider != null ? provider.toUpperCase() : "UNKNOWN";
        final String finalTimeFrame = timeFrame != null ? timeFrame : "1D";
        final Set<String> symbolsForPolling = Set.copyOf(resolvedSymbols);

        // Cancel existing stream if any for this provider
        disconnectStream(providerKey);

        // Initialize mock quotes if mockMode is enabled
        if (mockMode) {
            mockService.initializeMockQuotes(symbolsForPolling);
        }

        Runnable pollingTask = () -> {
            try {
                log.debug("Polling cycle executing for provider: {}, mockMode: {}", providerKey, mockMode);
                MarketDataUpdate update;
                if (mockMode) {
                    update = mockService.generateMockUpdate(symbolsForPolling);
                } else {
                    update = fetchMarketDataUpdate(
                            symbolsForPolling,
                            finalTimeFrame,
                            isIndexSymbol,
                            providerKey,
                            false);
                }

                if (update != null) {
                    log.info("Broadcasting polling update for provider: {} quotes={}",
                            providerKey,
                            update.getQuotes() != null ? update.getQuotes().size() : 0);
                    webSocketHandler.broadcast(update);
                    try {
                        Map<String, OHLCQuote> ohlcQuotes = convertToOHLCQuotes(update);
                        processor.processUpdate(ohlcQuotes, providerKey);
                    } catch (Exception ex) {
                        log.error("Failed to publish polling data to Kafka/processor", ex);
                    }
                } else {
                    log.warn("Fetched market data update is null for provider: {}", providerKey);
                }
            } catch (Exception e) {
                log.error("Error during polling stream execution for provider {}", providerKey, e);
            }
        };

        // Schedule task - use 5 seconds if mockMode is active, else use configured interval
        int intervalSeconds = mockMode ? 5 : pollIntervalSeconds;
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(pollingTask, 0, intervalSeconds,
                TimeUnit.SECONDS);
        activeStreams.put(providerKey, future);
        log.info("Polling stream started for provider: {} with interval: {} seconds (mockMode={})",
                providerKey, intervalSeconds, mockMode);
    }

    public StreamConnectResponse initiateStream(StreamConnectRequest request) {
        boolean expandIndices = request.getExpandIndices() != null ? request.getExpandIndices() : false;
        Set<String> resolvedSymbols = resolveSymbols(request.getInstrumentKeys(), expandIndices);

        if (Boolean.TRUE.equals(request.getIsIndexSymbol())) {
            resolvedSymbols.addAll(request.getInstrumentKeys());
        }

        log.info("Resolved {} symbols to {} for stream initiation",
                request.getInstrumentKeys().size(), resolvedSymbols.size());

        String provider = resolveProvider(request);
        log.info("Resolved provider for stream initiation: {}", provider);

        String timeFrame = request.getTimeFrame() != null ? request.getTimeFrame() : "1D";
        boolean isMarketOpen = marketHoursService.isMarketOpen();
        boolean isMock = isMockProvider(request, provider);
        StreamStrategy strategy = resolveStreamStrategy(request, provider, isMarketOpen, isMock);

        switch (strategy) {
            case MOCK_POLLING -> {
                log.info("MOCK provider: starting simulated polling stream for {} symbols (ignores market hours)",
                        resolvedSymbols.size());
                connectStream(resolvedSymbols, request.getMode(), "MOCK", timeFrame,
                        request.getIsIndexSymbol() != null ? request.getIsIndexSymbol() : false,
                        true);
            }
            case LIVE_STREAM -> {
                log.info("Market is OPEN - Initiating WebSocket stream via StreamerManager.");
                streamerManager.subscribe(new HashSet<>(resolvedSymbols));
            }
            case POLLING -> {
                log.info("Starting polling stream for provider {} (marketOpen={}, forcePolling={})",
                        provider, isMarketOpen, Boolean.TRUE.equals(request.getForcePolling()));
                connectStream(resolvedSymbols, request.getMode(), provider, timeFrame,
                        request.getIsIndexSymbol() != null ? request.getIsIndexSymbol() : false,
                        false);
            }
            case SNAPSHOT_ONLY -> log.info(
                    "Background stream skipped: stream={}, provider={}, marketOpen={}, mock={}",
                    request.getStream(), provider, isMarketOpen, isMock);
        }

        MarketDataUpdate initialData = buildInitialSnapshot(resolvedSymbols, request, provider, timeFrame, isMock, strategy);

        return StreamConnectResponse.builder()
                .status("SUCCESS")
                .message(buildConnectMessage(strategy, isMock))
                .data(initialData)
                .build();
    }

    enum StreamStrategy {
        MOCK_POLLING,
        LIVE_STREAM,
        POLLING,
        SNAPSHOT_ONLY
    }

    static boolean isStreamRequested(StreamConnectRequest request) {
        return request.getStream() == null || Boolean.TRUE.equals(request.getStream());
    }

    static boolean isMockProvider(StreamConnectRequest request, String resolvedProvider) {
        if (Boolean.TRUE.equals(request.getMockMode())) {
            return true;
        }
        String provider = request.getProvider();
        if (provider != null && "MOCK".equalsIgnoreCase(provider.trim())) {
            return true;
        }
        return resolvedProvider != null && "MOCK".equalsIgnoreCase(resolvedProvider.trim());
    }

    /** @deprecated use {@link #isMockProvider(StreamConnectRequest, String)} */
    static boolean isMockProvider(StreamConnectRequest request) {
        return isMockProvider(request, request.getProvider());
    }

    static StreamStrategy resolveStreamStrategy(StreamConnectRequest request, String provider,
                                                boolean isMarketOpen, boolean isMock) {
        if (!isStreamRequested(request)) {
            return StreamStrategy.SNAPSHOT_ONLY;
        }
        if (isMock) {
            return StreamStrategy.MOCK_POLLING;
        }
        boolean isUpstox = "UPSTOX".equalsIgnoreCase(provider);
        boolean forcePolling = Boolean.TRUE.equals(request.getForcePolling());
        if (isUpstox && isMarketOpen && !forcePolling) {
            return StreamStrategy.LIVE_STREAM;
        }
        if (forcePolling || !isUpstox) {
            return StreamStrategy.POLLING;
        }
        return StreamStrategy.SNAPSHOT_ONLY;
    }

    private String resolveProvider(StreamConnectRequest request) {
        if (request.getProvider() != null && !request.getProvider().trim().isEmpty()) {
            return request.getProvider().trim().toUpperCase();
        }
        return marketDataProviderFactory.getProvider().getProviderName().toUpperCase();
    }

    private MarketDataUpdate buildInitialSnapshot(Set<String> resolvedSymbols, StreamConnectRequest request,
                                                  String provider, String timeFrame, boolean isMock,
                                                  StreamStrategy strategy) {
        if (isMock) {
            log.info("Mock mode: generating initial snapshot from local persistence (no external provider call).");
            if (strategy != StreamStrategy.MOCK_POLLING) {
                mockService.initializeMockQuotes(resolvedSymbols);
            }
            return mockService.generateMockUpdate(resolvedSymbols);
        }
        return fetchMarketDataUpdate(
                resolvedSymbols,
                timeFrame,
                request.getIsIndexSymbol(),
                provider,
                false);
    }

    private static String buildConnectMessage(StreamStrategy strategy, boolean isMock) {
        return switch (strategy) {
            case LIVE_STREAM -> "Stream connection initiated successfully (Live Market Framework).";
            case MOCK_POLLING -> "Stream connection initiated successfully (Active Mock/Simulation Framework).";
            case POLLING -> "Stream connection initiated successfully (Simulated/Polling Framework).";
            case SNAPSHOT_ONLY -> isMock
                    ? "Mock snapshot only; continuous stream was not requested."
                    : "Market Closed. Fetched latest available data snapshot.";
        };
    }

    public MarketDataUpdate fetchMarketDataUpdate(
            Set<String> keys, String timeFrame, Boolean isIndexSymbol, String providerKey) {
        // Backward compatibility
        return fetchMarketDataUpdate(keys, timeFrame, isIndexSymbol, providerKey, false);
    }

    public MarketDataUpdate fetchMarketDataUpdate(
            Set<String> keys, String timeFrame, Boolean isIndexSymbol, String providerKey, boolean forceRefresh) {
        try {
            // Orchestration Step 2 & 3: Parallel Execution of Data Fetching

            // Task 1: Fetch Live OHLC Data
            CompletableFuture<Map<String, OHLCQuote>> liveDataFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    // Pass forceRefresh parameter
                    return marketDataFetchService.getOHLC(keys, forceRefresh, TimeFrame.DAY, false);
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
        // Also ensure streamer manager disconnects if it's the active one
        if ("UPSTOX".equalsIgnoreCase(key)) {
            streamerManager.stopStreaming();
        }
    }

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
    private Map<String, MarketDataUpdate.QuoteChange> buildQuoteUpdates(
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

    private Map<String, OHLCQuote> convertToOHLCQuotes(MarketDataUpdate update) {
        if (update == null || update.getQuotes() == null) {
            return Collections.emptyMap();
        }
        Map<String, OHLCQuote> ohlcQuotes = new HashMap<>();
        for (Map.Entry<String, MarketDataUpdate.QuoteChange> entry : update.getQuotes().entrySet()) {
            String symbol = entry.getKey();
            MarketDataUpdate.QuoteChange change = entry.getValue();
            OHLCQuote.OHLC ohlc = OHLCQuote.OHLC.builder()
                    .open(change.getOpen() != null ? change.getOpen() : 0.0)
                    .high(change.getHigh() != null ? change.getHigh() : 0.0)
                    .low(change.getLow() != null ? change.getLow() : 0.0)
                    .close(change.getClose() != null ? change.getClose() : 0.0)
                    .build();
            OHLCQuote quote = OHLCQuote.builder()
                    .lastPrice(change.getLastPrice() != null ? change.getLastPrice() : 0.0)
                    .previousClose(change.getPreviousClose() != null ? change.getPreviousClose() : 0.0)
                    .ohlc(ohlc)
                    .build();
            ohlcQuotes.put(symbol, quote);
        }
        return ohlcQuotes;
    }
}
