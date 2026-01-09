package com.am.marketdata.scheduler.service;

import com.am.marketdata.scraper.service.CookieManagementService;
import com.am.marketdata.scraper.exception.CookieException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for Cookie Management and Indices Processing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CookieScheduler {

    private final CookieManagementService cookieManagementService;

    // Run every hour for cookie refresh
    @Scheduled(cron = "${scheduler.cookie.refresh:0 0 * * * *}")
    public void scheduledCookieRefresh() {
        try {
            log.info("Starting scheduled cookie refresh");
            cookieManagementService.refreshCookies(); // Ensure this method exists and is public
        } catch (CookieException e) {
            log.error("Failed to refresh cookies in scheduled task: {}", e.getMessage(), e);
        }
    }

    // Run every 2 minutes for indices data processing
    @Scheduled(cron = "${scheduler.indices.fetch:0 */2 * * * *}", zone = "Asia/Kolkata")
    public void scheduleIndicesDataProcessing() {
        try {
            log.info("Triggering scheduled indices data processing");
            cookieManagementService.processIndicesData();
        } catch (Exception e) {
            log.error("Failed to process indices data: {}", e.getMessage(), e);
        }
    }
}
