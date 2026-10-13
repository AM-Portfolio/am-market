package com.am.marketdata.scheduler;

import com.am.marketdata.service.ipo.UpstoxIpoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background Scheduler running periodic and startup synchronization for Upstox IPO data.
 * Executes nightly sync at 00:00 IST and optional warm sync on startup if database is empty.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpstoxIpoSyncScheduler {

    private final UpstoxIpoService upstoxIpoService;

    @Value("${market-data.ipo.upstox.enabled:true}")
    private boolean enabled;

    @Value("${market-data.ipo.upstox.sync-on-startup:true}")
    private boolean syncOnStartup;

    /**
     * Nightly scheduled cron job running at 00:00:00 IST every night.
     */
    @Scheduled(cron = "${scheduler.ipo.upstox-cron:0 0 0 * * *}", zone = "Asia/Kolkata")
    @com.am.scheduler.annotation.TrackedAndLockedScheduler(
            name = "upstoxIpoSyncSchedulerJob",
            lockAtMostFor = "30m",
            lockAtLeastFor = "2m")
    public void syncNightlyUpstoxIpos() {
        if (!enabled) {
            log.info("Upstox IPO Sync Scheduler is disabled via configuration.");
            return;
        }
        try {
            log.info("Executing scheduled nightly Upstox IPO sync...");
            int count = upstoxIpoService.syncUpstoxIpos("all");
            log.info("Nightly Upstox IPO sync completed successfully. Synced {} IPOs.", count);
        } catch (Exception e) {
            log.error("Nightly Upstox IPO sync failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Executes initial sync on application startup if configured.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartupIfEnabled() {
        if (!enabled || !syncOnStartup) {
            return;
        }
        new Thread(() -> {
            try {
                log.info("Triggering initial startup sync for open and upcoming Upstox IPOs...");
                upstoxIpoService.syncUpstoxIpos("open");
                upstoxIpoService.syncUpstoxIpos("upcoming");
            } catch (Exception e) {
                log.warn("Startup Upstox IPO sync encounter error: {}", e.getMessage());
            }
        }, "upstox-ipo-startup-sync").start();
    }
}
