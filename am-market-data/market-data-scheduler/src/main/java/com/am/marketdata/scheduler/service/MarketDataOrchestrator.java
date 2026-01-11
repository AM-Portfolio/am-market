package com.am.marketdata.scheduler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Centralized Orchestrator for all Market Data Scheduled Tasks.
 * Replaces individual scheduler configurations to provide a single view of all
 * jobs.
 */
@Slf4j
@Component
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class MarketDataOrchestrator {

    private final java.util.Optional<CookieScheduler> cookieScheduler;
    private final java.util.Optional<MarketDataIngestionScheduler> ingestionScheduler;
    private final java.util.Optional<RedisCacheCleanupScheduler> redisCacheCleanupScheduler;
    private final java.util.Optional<StockIndicesSchedulerService> stockIndicesSchedulerService;
    private final java.util.Optional<StreamerScheduler> streamerScheduler;

    // --- High Frequency Jobs ---

    /**
     * Indices Data Processing: Runs every 2 minutes
     */
    @Scheduled(cron = "${scheduler.indices.fetch:0 */2 * * * *}", zone = "Asia/Kolkata")
    public void triggerIndicesDataProcessing() {
        log.info("Orchestrator: Triggering Indices Data Processing");
        if (cookieScheduler.isPresent()) {
            cookieScheduler.get().executeIndicesDataProcessing();
        } else {
            log.warn("Orchestrator: CookieScheduler is not present, skipping Indices Data Processing");
        }
    }

    /**
     * Stock Indices Retry: Runs every 15 minutes (or configured interval)
     */
    @Scheduled(cron = "${scheduler.stock-indices.retry.cron:0 */15 * * * *}", zone = "Asia/Kolkata")
    public void triggerStockIndicesRetry() {
        log.info("Orchestrator: Triggering Stock Indices Retry Check");
        if (stockIndicesSchedulerService.isPresent()) {
            stockIndicesSchedulerService.get().executeRetryJob();
        } else {
            log.warn("Orchestrator: StockIndicesSchedulerService is not present, skipping Stock Indices Retry");
        }
    }

    // --- Hourly Jobs ---

    /**
     * Cookie Refresh: Runs every hour ONLY during market hours (9 AM - 4 PM,
     * Mon-Fri)
     */
    @Scheduled(cron = "${scheduler.cookie.refresh:0 0 9-16 * * MON-FRI}", zone = "Asia/Kolkata")
    public void triggerCookieRefresh() {
        log.info("Orchestrator: Triggering Cookie Refresh (Market Hours)");
        if (cookieScheduler.isPresent()) {
            cookieScheduler.get().executeCookieRefresh();
        } else {
            log.warn("Orchestrator: CookieScheduler is not present, skipping Cookie Refresh");
        }
    }

    /**
     * Weekend Cookie Maintenance: Runs on Saturday at 10:00 AM
     * Ensures cookies are valid/refreshed for weekend analysis or preparation.
     */
    @Scheduled(cron = "${scheduler.cookie.weekend:0 0 10 * * SAT}", zone = "Asia/Kolkata")
    public void triggerWeekendCookieMaintenance() {
        log.info("Orchestrator: Triggering Weekend Cookie Maintenance");
        if (cookieScheduler.isPresent()) {
            cookieScheduler.get().executeCookieRefresh();
        } else {
            log.warn("Orchestrator: CookieScheduler is not present, skipping Weekend Cookie Maintenance");
        }
    }

    // --- Daily: Market Open/Close ---

    /**
     * Market Open Operations (e.g., 9:15 AM)
     * Starts Ingestion
     */
    @Scheduled(cron = "${scheduler.ingestion.start-cron:0 15 9 * * MON-FRI}", zone = "Asia/Kolkata")
    public void triggerMarketOpenJobs() {
        log.info("Orchestrator: Triggering Market Open Jobs");
        if (ingestionScheduler.isPresent()) {
            ingestionScheduler.get().startIngestionJob();
        } else {
            log.warn("Orchestrator: MarketDataIngestionScheduler is not present, skipping Market Open Jobs");
        }
    }

    /**
     * Streamer Start (e.g., 8:00 AM)
     */
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Kolkata")
    public void triggerStreamerStart() {
        log.info("Orchestrator: Triggering Streamer Start");
        if (streamerScheduler.isPresent()) {
            streamerScheduler.get().executeStartStreaming();
        } else {
            log.warn("Orchestrator: StreamerScheduler is not present, skipping Streamer Start");
        }
    }

    /**
     * Market Close Operations (e.g., 3:30 PM)
     * Stops Ingestion
     */
    @Scheduled(cron = "${scheduler.ingestion.stop-cron:0 30 15 * * MON-FRI}", zone = "Asia/Kolkata")
    public void triggerIngestionStop() {
        log.info("Orchestrator: Triggering Ingestion Stop");
        if (ingestionScheduler.isPresent()) {
            ingestionScheduler.get().stopIngestionJob();
        } else {
            log.warn("Orchestrator: MarketDataIngestionScheduler is not present, skipping Ingestion Stop");
        }
    }

    /**
     * Streamer Stop (e.g., 4:00 PM)
     */
    @Scheduled(cron = "0 0 16 * * *", zone = "Asia/Kolkata")
    public void triggerStreamerStop() {
        log.info("Orchestrator: Triggering Streamer Stop");
        if (streamerScheduler.isPresent()) {
            streamerScheduler.get().executeStopStreaming();
        } else {
            log.warn("Orchestrator: StreamerScheduler is not present, skipping Streamer Stop");
        }
    }

    /**
     * Morning Stock Indices Fetch (e.g., 9:30 AM)
     */
    @Scheduled(cron = "${scheduler.stock-indices.morning-fetch:0 30 9 * * *}", zone = "Asia/Kolkata")
    public void triggerMorningStockIndicesFetch() {
        log.info("Orchestrator: Triggering Morning Stock Indices Fetch");
        if (stockIndicesSchedulerService.isPresent()) {
            stockIndicesSchedulerService.get().executeMorningStockIndicesFetch();
        } else {
            log.warn("Orchestrator: StockIndicesSchedulerService is not present, skipping Morning Stock Indices Fetch");
        }
    }

    /**
     * Evening Stock Indices Fetch (e.g., 4:00 PM)
     */
    @Scheduled(cron = "${scheduler.stock-indices.evening-fetch:0 0 16 * * *}", zone = "Asia/Kolkata")
    public void triggerEveningStockIndicesFetch() {
        log.info("Orchestrator: Triggering Evening Stock Indices Fetch");
        if (stockIndicesSchedulerService.isPresent()) {
            stockIndicesSchedulerService.get().executeEveningStockIndicesFetch();
        } else {
            log.warn("Orchestrator: StockIndicesSchedulerService is not present, skipping Evening Stock Indices Fetch");
        }
    }

    // --- Daily: Maintenance ---

    /**
     * Manual Historical Data Sync Trigger
     */
    public void triggerHistoricalData(String symbol, String duration, boolean forceRefresh) {
        log.info("Orchestrator: Triggering Manual Historical Data Sync for {}, Duration: {}, Force: {}", symbol,
                duration, forceRefresh);
        if (ingestionScheduler.isPresent()) {
            ingestionScheduler.get().executeManualHistoricalSync(symbol, duration, forceRefresh);
        } else {
            log.warn("Orchestrator: MarketDataIngestionScheduler is not present, skipping Manual Historical Data Sync");
        }
    }

    /**
     * Historical Data Sync (e.g. 7:15 AM)
     */
    @Scheduled(cron = "${scheduler.historical.sync-cron:0 15 7 * * *}")
    public void triggerHistoricalSync() {
        log.info("Orchestrator: Triggering Historical Data Sync");
        if (ingestionScheduler.isPresent()) {
            ingestionScheduler.get().executeHistoricalSync();
        } else {
            log.warn("Orchestrator: MarketDataIngestionScheduler is not present, skipping Historical Data Sync");
        }
    }

    /**
     * Redis Cache Cleanup (e.g., 2:00 AM)
     */
    @Scheduled(cron = "${scheduler.redis.cleanup.cron:0 0 2 * * *}")
    public void triggerRedisCleanup() {
        log.info("Orchestrator: Triggering Redis Cleanup");
        if (redisCacheCleanupScheduler.isPresent()) {
            redisCacheCleanupScheduler.get().executeCleanup();
        } else {
            log.warn("Orchestrator: RedisCacheCleanupScheduler is not present, skipping Redis Cleanup");
        }
    }
}
