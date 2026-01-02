package com.am.marketdata.scraper.service;

import com.am.marketdata.scraper.cookie.CookieManager;
import com.am.marketdata.scraper.exception.CookieException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Service responsible for cookie management and regular indices data processing.
 * Runs every 2 minutes during trading hours to fetch indices data.
 * Stock indices data is handled by a separate scheduler (StockIndicesSchedulerService).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "scheduler.cookie.enabled", havingValue = "true", matchIfMissing = true)
public class CookieSchedulerService {
    private final CookieManager cookieManager;
    private final MarketDataProcessingService marketDataProcessingService;

    @PostConstruct
    public void initialize() {
        try {
            log.info("Initializing CookieSchedulerService");
            refreshCookies();
            
            // If within trading hours, start market data processing
            if (isWithinTradingHours()) {
                log.info("Within trading hours, starting initial market data processing");
                //marketDataProcessingService.fetchAndProcessMarketData();
                marketDataProcessingService.fetchAndProcessStockIndicesOnly();
            } else {
                log.info("Outside trading hours, skipping initial market data processing");
            }
        } catch (Exception e) {
            log.error("Failed to initialize service: {}", e.getMessage(), e);
        }
    }

    // Run every hour for cookie refresh
    @Scheduled(cron = "${scheduler.cookie.refresh:0 0 * * * *}")
    public void scheduledCookieRefresh() {
        try {
            log.info("Starting scheduled cookie refresh");
            cookieManager.refreshIfNeeded();
        } catch (CookieException e) {
            log.error("Failed to refresh cookies in scheduled task: {}", e.getMessage(), e);
        }
    }

    // Run every 2 minutes for indices data processing
    @Scheduled(cron = "${scheduler.indices.fetch:0 */2 * * * *}", zone = "Asia/Kolkata")
    public void scheduleIndicesDataProcessing() {
        try {
            // Only process between 9:15 AM and 3:35 PM
            if (isWithinTradingHours()) {
                // Refresh cookies if needed before processing
                cookieManager.refreshIfNeeded();
                
                log.info("Starting scheduled indices data processing");
                marketDataProcessingService.fetchAndProcessMarketData();
            } else {
                log.debug("Outside trading hours, skipping indices data processing");
            }
        } catch (Exception e) {
            log.error("Failed to process indices data: {}", e.getMessage(), e);
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
        
        //return !now.isBefore(marketStart) && !now.isAfter(marketEnd);
        return true;
    }
}
