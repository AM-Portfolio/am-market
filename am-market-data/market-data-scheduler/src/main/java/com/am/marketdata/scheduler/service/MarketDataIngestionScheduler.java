package com.am.marketdata.scheduler.service;

import com.am.marketdata.internal.service.MarketDataIngestionService;
import com.am.marketdata.service.SymbolOrchestratorService;
import com.am.marketdata.service.websocket.service.StreamerManager;
import com.am.marketdata.common.log.AppLogger;
import com.am.marketdata.internal.service.MarketDataHistoricalSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalTime;

/**
 * Scheduler to control Market Data Ingestion.
 * Starts ingestion at market open and stops at market close.
 */
@Service
@RequiredArgsConstructor
public class MarketDataIngestionScheduler {

    private final AppLogger log = AppLogger.getLogger(MarketDataIngestionScheduler.class);

    private final MarketDataIngestionService ingestionService;
    private final MarketDataHistoricalSyncService historicalSyncService;
    private final StreamerManager streamerManager;

    private final SymbolOrchestratorService symbolService;

    @Value("${scheduler.ingestion.enabled:true}")
    private boolean enabled;

    @Value("${scheduler.ingestion.provider:UPSTOX}")
    private String provider;

    /** When false, historical/REST batch uses cache+DB only (no Upstox historical API on poll). */
    @Value("${scheduler.ingestion.force:false}")
    private boolean forceRefresh;

    /** Live prices via Upstox WebSocket during market hours (recommended). */
    @Value("${scheduler.ingestion.use-websocket:true}")
    private boolean useWebSocket;

    /** Legacy REST polling loop (10s). Off by default when WebSocket is used. */
    @Value("${scheduler.ingestion.poll-enabled:false}")
    private boolean pollEnabled;

    // Market Hours Config
    @Value("${scheduler.market.start:09:15}")
    private String marketStartTime;

    @Value("${scheduler.market.end:15:30}")
    private String marketEndTime;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (enabled && isMarketOpen() && !isWeekend()) {
            log.info("init", "Application started during market hours. Starting live data path (websocket={}, poll={}).",
                    useWebSocket, pollEnabled);
            startLiveMarketData();
        }
    }

    /**
     * Start Ingestion at Market Open (e.g., 9:15 AM)
     */
    public void startIngestionJob() {
        if (!enabled)
            return;
        if (isWeekend()) {
            log.info("startIngestionJob", "Skipping market data ingestion on weekends.");
            return;
        }
        log.info("scheduledStart", "Scheduled trigger: Starting live market data (websocket={}, poll={})",
                useWebSocket, pollEnabled);
        startLiveMarketData();
    }

    /**
     * Stop Ingestion at Market Close (e.g., 3:30 PM)
     */
    public void stopIngestionJob() {
        if (!enabled)
            return;
        log.info("scheduledStop", "Scheduled trigger: Stopping live market data");
        stopLiveMarketData();
    }

    /**
     * Run Historical Sync (Smart Delta) at 07:15 AM
     */
    public void executeHistoricalSync() {
        if (!enabled)
            return;
        if (isWeekend()) {
            log.info("executeHistoricalSync", "Skipping historical data sync on weekends.");
            return;
        }
        log.info("scheduledHistoricalSync", "Scheduled trigger: Starting Historical Data Sync (Smart Delta, cache-aware)");
        // Once per day before market open; incremental when data already in cache/DB
        historicalSyncService.syncHistoricalData(null, null, false, false);
    }

    public void executeManualHistoricalSync(String symbol, String duration, boolean forceRefresh) {
        log.info("executeManualHistoricalSync",
                "Manual trigger: Starting Historical Data Sync for {}, Duration: {}, Force: {}", symbol, duration,
                forceRefresh);
        // fetchIndexStocks is defaulted to TRUE for manual triggers as per requirements
        historicalSyncService.syncHistoricalData(symbol, duration, forceRefresh, true);
    }

    protected List<String> getSymbolsToProcess() {
        return new ArrayList<>(symbolService.findDistinctSymbols());
    }

    /**
     * Market hours: Upstox WebSocket for live prices; optional legacy REST poll.
     * Historical candles come from daily sync (07:15) + cache, not from this loop.
     */
    private void startLiveMarketData() {
        if (useWebSocket && isUpstoxProvider()) {
            streamerManager.refreshSubscriptions();
            streamerManager.startStreaming();
            log.info("startLiveMarketData", "Upstox WebSocket stream started for portfolio symbols");
        }

        if (pollEnabled) {
            startRestPollingIngestion();
        }
    }

    private void stopLiveMarketData() {
        if (useWebSocket && isUpstoxProvider()) {
            streamerManager.stopStreaming();
        }
        ingestionService.stopIngestion(provider);
    }

    /** Legacy REST polling (OHLC/historical). Disabled by default. */
    private void startRestPollingIngestion() {
        List<String> symbolsToProcess = getSymbolsToProcess();
        log.info("startRestPollingIngestion", "Starting REST polling for {} symbols (forceRefresh={})",
                symbolsToProcess != null ? symbolsToProcess.size() : 0, forceRefresh);

        if (symbolsToProcess != null && !symbolsToProcess.isEmpty()) {
            ingestionService.startIngestion(symbolsToProcess, provider, "1D", true, forceRefresh);
        } else {
            log.warn("startRestPollingIngestion", "No symbols found to process!");
        }
    }

    private boolean isUpstoxProvider() {
        return provider != null && "UPSTOX".equalsIgnoreCase(provider.trim());
    }

    private boolean isMarketOpen() {
        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.parse(marketStartTime);
        LocalTime end = LocalTime.parse(marketEndTime);
        return now.isAfter(start) && now.isBefore(end);
    }

    private boolean isWeekend() {
        java.time.DayOfWeek day = java.time.LocalDate.now().getDayOfWeek();
        return day == java.time.DayOfWeek.SATURDAY || day == java.time.DayOfWeek.SUNDAY;
    }
}
