package com.am.marketdata.scheduler.service;

import com.am.marketdata.service.SymbolOrchestratorService;
import com.am.marketdata.service.websocket.service.StreamerManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Scheduler for Market Data Streamer.
 * Triggers start/stop of streaming based on market hours.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreamerScheduler {

    private final StreamerManager streamerManager;
    private final SymbolOrchestratorService symbolService;

    protected List<String> getSymbolsToProcess() {
        return symbolService.findDistinctIsins();
    }

    /**
     * Start the streamer (Scheduled at 8:00 AM)
     */
    public void executeStartStreaming() {
        log.info("Triggering scheduled Streamer start...");
        // Ensure symbols are fresh before starting
        List<String> symbols = getSymbolsToProcess();
        log.info("Ensuring subscriptions for {} symbols", symbols != null ? symbols.size() : 0);
        streamerManager.refreshSubscriptions();
        streamerManager.startStreaming();
    }

    /**
     * Stop the streamer (Scheduled at 4:00 PM)
     */
    public void executeStopStreaming() {
        log.info("Triggering scheduled Streamer stop...");
        streamerManager.stopStreaming();
    }
}
