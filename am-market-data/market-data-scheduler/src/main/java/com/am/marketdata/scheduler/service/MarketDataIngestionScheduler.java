package com.am.marketdata.scheduler.service;

import com.am.marketdata.internal.service.MarketDataIngestionService;
import com.am.marketdata.service.SymbolOrchestratorService;
import com.am.marketdata.common.log.AppLogger;
import com.am.marketdata.internal.service.MarketDataHistoricalSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.PostConstruct;
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

    private final SymbolOrchestratorService symbolService;

    @Value("${scheduler.ingestion.enabled:true}")
    private boolean enabled;

    // derived from service now
    // private List<String> symbols;

    @Value("${scheduler.ingestion.provider:ZERODHA}")
    private String provider;

    @Value("${scheduler.ingestion.force:true}")
    private boolean forceRefresh;

    // Market Hours Config
    @Value("${scheduler.market.start:09:15}")
    private String marketStartTime;

    @Value("${scheduler.market.end:15:30}")
    private String marketEndTime;

    @PostConstruct
    public void init() {
        if (enabled && isMarketOpen() && !isWeekend()) {
            log.info("init", "Application started during market hours. Triggering ingestion.");
            startIngestion();
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
        log.info("scheduledStart", "Scheduled trigger: Starting Market Data Ingestion");
        startIngestion();
    }

    /**
     * Stop Ingestion at Market Close (e.g., 3:30 PM)
     */
    public void stopIngestionJob() {
        if (!enabled)
            return;
        log.info("scheduledStop", "Scheduled trigger: Stopping Market Data Ingestion");
        ingestionService.stopIngestion(provider);
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
        log.info("scheduledHistoricalSync", "Scheduled trigger: Starting Historical Data Sync (Smart Delta)");
        // null duration -> uses default logic (10yr or incremental)
        historicalSyncService.syncHistoricalData(null, null, true, false);
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

    private void startIngestion() {
        // Trigger Ingestion for symbols from orchestrator
        List<String> symbolsToProcess = getSymbolsToProcess();
        log.info("startIngestion", "Starting ingestion for {} symbols",
                symbolsToProcess != null ? symbolsToProcess.size() : 0);

        if (symbolsToProcess != null && !symbolsToProcess.isEmpty()) {
            ingestionService.startIngestion(symbolsToProcess, provider, "1D", true, forceRefresh);
        } else {
            log.warn("startIngestion", "No symbols found to process!");
        }
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
