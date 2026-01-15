package com.am.marketdata.internal.service;

import com.am.marketdata.provider.common.MarketDataProviderFactory;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.common.model.MarketDataUpdate;
import com.am.marketdata.api.util.InstrumentUtils;
import com.am.marketdata.api.model.HistoricalDataResponseV1;
import com.am.marketdata.api.service.MarketDataFetchService;
import com.am.common.investment.model.historical.HistoricalData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.*;

/**
 * Service for internal market data ingestion and orchestration.
 * Responsible for keeping the cache warm by polling upstream providers.
 */
@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class MarketDataIngestionService {

    private final MarketDataFetchService marketDataFetchService;
    private final InstrumentUtils instrumentUtils;
    private final MarketDataProviderFactory marketDataProviderFactory;

    // Scheduler for polling
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final Map<String, ScheduledFuture<?>> activeStreams = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Value("${market-data.stream.poll-interval-seconds:10}")
    private int pollIntervalSeconds;

    /**
     * Start ingestion stream for a set of instruments
     */
    public void startIngestion(List<String> instrumentKeys, String provider, String timeFrame, Boolean isIndexSymbol) {
        startIngestion(instrumentKeys, provider, timeFrame, isIndexSymbol, false);
    }

    public void startIngestion(List<String> instrumentKeys, String provider, String timeFrame, Boolean isIndexSymbol,
            boolean forceRefresh) {

        // Resolve Symbols
        Set<String> resolvedSymbols = instrumentUtils.resolveSymbols(instrumentKeys, false);
        log.info(
                "Starting data ingestion for {} instruments. Provider: {}, TimeFrame: {}",
                resolvedSymbols.size(), provider, timeFrame);

        String providerKey = provider != null ? provider.toUpperCase() : "UNKNOWN";
        final String finalTimeFrame = timeFrame != null ? timeFrame : "1D";

        // Cancel existing stream if any for this provider
        stopIngestion(providerKey);

        Runnable pollingTask = () -> {
            try {
                // Trigger fetch - this inherently updates the cache via
                // MarketDataFetchServiceImpl
                fetchMarketDataUpdate(
                        resolvedSymbols,
                        finalTimeFrame,
                        isIndexSymbol,
                        providerKey,
                        forceRefresh);

                log.debug("Ingestion polling cycle completed for provider {}", providerKey);

            } catch (Exception e) {
                log.error("Error during ingestion polling for provider {}", providerKey, e);
            }
        };

        // Schedule task
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(pollingTask, 0, pollIntervalSeconds,
                TimeUnit.SECONDS);
        activeStreams.put(providerKey, future);
        log.info("Ingestion started for provider: {} with interval: {} seconds", providerKey, pollIntervalSeconds);
    }

    public void stopIngestion(String provider) {
        if (provider == null)
            return;
        String key = provider.toUpperCase();
        if (activeStreams.containsKey(key)) {
            ScheduledFuture<?> future = activeStreams.get(key);
            if (future != null && !future.isCancelled()) {
                future.cancel(true);
            }
            activeStreams.remove(key);
            log.info("Ingestion stopped for provider: {}", provider);
        }
    }

    public MarketDataUpdate fetchMarketDataUpdate(
            Set<String> keys, String timeFrame, Boolean isIndexSymbol, String providerKey) {
        return fetchMarketDataUpdate(keys, timeFrame, isIndexSymbol, providerKey, false);
    }

    public MarketDataUpdate fetchMarketDataUpdate(
            Set<String> keys, String timeFrame, Boolean isIndexSymbol, String providerKey, boolean forceRefresh) {
        try {
            // Task 1: Fetch Live OHLC Data (Populates Cache)
            CompletableFuture<Map<String, OHLCQuote>> liveDataFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return marketDataFetchService.getOHLC(keys, false, TimeFrame.DAY, forceRefresh);
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
                        return fetchHistoricalData(keys, timeFrame, isIndexSymbol, forceRefresh);
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

            // Merge Data
            Map<String, OHLCQuote> enrichedData = mergeData(liveOhlcData, historicalResponse);

            if (enrichedData != null && !enrichedData.isEmpty()) {
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

    private HistoricalDataResponseV1 fetchHistoricalData(Set<String> symbols,
            String timeFrame, Boolean isIndexSymbol, boolean forceRefresh) {
        // Calculate historical date range based on timeFrame
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate historicalDate;
        java.time.LocalDate startDate;

        switch (timeFrame.toUpperCase()) {
            case "1D":
            case "DAY":
                historicalDate = today.minusDays(1);
                startDate = historicalDate.minusDays(7);
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

        java.util.Date fromDate = java.util.Date.from(
                startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        java.util.Date toDate = java.util.Date.from(
                historicalDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());

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
                forceRefresh,
                false); // fetchIndexStocks = false (symbols already resolved)
    }

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
                        }
                    }
                }
            }

            OHLCQuote enrichedQuote = OHLCQuote.builder()
                    .lastPrice(liveQuote.getLastPrice())
                    .previousClose(previousClose)
                    .ohlc(liveQuote.getOhlc())
                    .build();

            enrichedData.put(symbol, enrichedQuote);
        }
        return enrichedData;
    }

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
