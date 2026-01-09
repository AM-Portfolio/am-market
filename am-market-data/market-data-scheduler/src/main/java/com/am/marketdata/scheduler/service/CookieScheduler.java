package com.am.marketdata.scheduler.service;

import com.am.marketdata.scraper.service.CookieManagementService;
import com.am.marketdata.scraper.exception.CookieException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Scheduler for Cookie Management and Indices Processing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "scheduler.cookie.enabled", havingValue = "true", matchIfMissing = true)
public class CookieScheduler {

    private final CookieManagementService cookieManagementService;

    // Run every hour for cookie refresh
    public void executeCookieRefresh() {
        try {
            log.info("Starting scheduled cookie refresh");
            cookieManagementService.refreshCookies(); // Ensure this method exists and is public
        } catch (CookieException e) {
            log.error("Failed to refresh cookies in scheduled task: {}", e.getMessage(), e);
        }
    }

    // Run every 2 minutes for indices data processing
    public void executeIndicesDataProcessing() {
        try {
            log.info("Triggering scheduled indices data processing");
            cookieManagementService.processIndicesData();
        } catch (Exception e) {
            log.error("Failed to process indices data: {}", e.getMessage(), e);
        }
    }
}
