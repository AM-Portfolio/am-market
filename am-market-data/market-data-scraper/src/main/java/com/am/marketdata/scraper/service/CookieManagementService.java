package com.am.marketdata.scraper.service;

import com.am.marketdata.scraper.cookie.CookieManager;
import com.am.marketdata.scraper.exception.CookieException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Service responsible for cookie management and regular indices data processing
 * logic.
 * Scheduling is handled by CookieScheduler in market-data-scheduler module.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "scheduler.cookie.enabled", havingValue = "true", matchIfMissing = true)
public class CookieManagementService {
    private final CookieManager cookieManager;
    private final MarketDataProcessingService marketDataProcessingService;

    @PostConstruct
    public void initialize() {
        try {
            log.info("Initializing CookieManagementService");
            // refreshCookies(); // Disabled on startup as per requirement. Relying on
            // Scheduler.

            // Initial market data processing removed as per requirements
            log.info("CookieService Initialized. Waiting for scheduler.");
        } catch (Exception e) {
            log.error("Failed to initialize service: {}", e.getMessage(), e);
        }
    }

    public void processIndicesData() {
        try {
            log.info("Processing indices data check...");
            // Only process between 9:15 AM and 3:35 PM
            if (isWithinTradingHours()) {
                log.info("Within trading hours. Refreshing cookies if needed...");
                // Refresh cookies if needed before processing
                cookieManager.refreshIfNeeded();

                log.info("Starting indices data processing (triggered)");
                marketDataProcessingService.fetchAndProcessMarketData();
            } else {
                log.info("Outside trading hours, skipping indices data processing");
            }
        } catch (Exception e) {
            log.error("Failed to process indices data (execution): {}", e.getMessage(), e);
        }
    }

    public void refreshCookies() {
        try {
            log.info("Attempting to refresh cookies");
            cookieManager.refreshIfNeeded();
        } catch (CookieException e) {
            log.error("Failed to refresh cookies: {}", e.getMessage(), e);
            throw e;
        }
    }

    private boolean isWithinTradingHours() {
        LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));
        LocalTime marketStart = LocalTime.of(9, 15);
        LocalTime marketEnd = LocalTime.of(15, 35);
        return now.isAfter(marketStart) && now.isBefore(marketEnd);
    }
}
