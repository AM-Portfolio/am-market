package com.am.marketdata.scheduler.service;

import com.am.marketdata.service.websocket.service.StreamerManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
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

    /**
     * Start the streamer (Scheduled at 8:00 AM)
     */
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Kolkata")
    public void scheduleStartStreaming() {
        log.info("Triggering scheduled Streamer start...");
        streamerManager.startStreaming();
    }

    /**
     * Stop the streamer (Scheduled at 4:00 PM)
     */
    @Scheduled(cron = "0 0 16 * * *", zone = "Asia/Kolkata")
    public void scheduleStopStreaming() {
        log.info("Triggering scheduled Streamer stop...");
        streamerManager.stopStreaming();
    }
}
