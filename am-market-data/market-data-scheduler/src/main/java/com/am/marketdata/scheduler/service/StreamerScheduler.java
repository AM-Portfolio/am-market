package com.am.marketdata.scheduler.service;

import com.am.marketdata.service.SymbolOrchestratorService;
import com.am.marketdata.service.websocket.service.StreamerManager;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for Market Data Streamer.
 * Triggers start/stop of streaming based on market hours.
 * Auto-start at 9:00 AM Monday-Friday (market open)
 * Auto-stop at 3:30 PM Monday-Friday (market close)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreamerScheduler {

    private final StreamerManager streamerManager;
    private final SymbolOrchestratorService symbolService;

    /**
     * Start the streamer (Scheduled at 9:00 AM Monday-Friday)
     * Auto-connects WebSocket and subscribes to all symbols
     */
    @Scheduled(cron = "0 0 9 ? * MON-FRI", zone = "Asia/Kolkata")
    public void executeStartStreaming() {
        log.info("⏰ Triggering scheduled Streamer start at 9:00 AM...");

        // Refresh symbols and subscribe
        Set<String> instrumentKeys = symbolService.findDistinctSymbols();
        log.info("Ensuring subscriptions for {} instrument keys", instrumentKeys != null ? instrumentKeys.size() : 0);

        streamerManager.refreshSubscriptions();
        streamerManager.startStreaming();

        log.info("✅ Streamer started successfully");
    }

    /**
     * Stop the streamer (Scheduled at 3:30 PM Monday-Friday)
     * Disconnects WebSocket gracefully at market close
     */
    @Scheduled(cron = "0 30 15 ? * MON-FRI", zone = "Asia/Kolkata")
    public void executeStopStreaming() {
        log.info("⏰ Triggering scheduled Streamer stop at 3:30 PM (market close)...");
        streamerManager.stopStreaming();
        log.info("✅ Streamer stopped successfully");
    }
}
