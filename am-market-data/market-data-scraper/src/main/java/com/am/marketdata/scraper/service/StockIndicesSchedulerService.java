package com.am.marketdata.scraper.service;

import com.am.marketdata.scraper.exception.CookieException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduler service specifically for stock indices data.
 * Runs at 9:30 AM and 4:00 PM with retry mechanism.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "scheduler.stock-indices.enabled", havingValue = "true", matchIfMissing = true)
public class StockIndicesSchedulerService {
    @Value("${scheduler.stock-indices.retry.interval-minutes:15}")
    private int retryIntervalMinutes;
    
    @Value("${scheduler.stock-indices.retry.max-retries:20}")
    private int maxRetries;
    
    private final MarketDataProcessingService marketDataProcessingService;
    
    private final AtomicBoolean morningFetchCompleted = new AtomicBoolean(false);
    private final AtomicBoolean eveningFetchCompleted = new AtomicBoolean(false);
    private int currentRetryCount = 0;
    private LocalDate lastProcessedDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
    
    @PostConstruct
    public void initialize() {
        log.info("Initializing StockIndicesSchedulerService with retry interval: {} minutes, max retries: {}", 
                retryIntervalMinutes, maxRetries);
        
        // Reset flags at startup based on current time
        resetFlagsIfNeeded();
    }
    
    /**
     * Morning schedule at 9:30 AM IST
     */
    @Scheduled(cron = "${scheduler.stock-indices.morning-fetch:0 30 9 * * *}", zone = "Asia/Kolkata")
    public void scheduleMorningStockIndicesFetch() {
        resetFlagsIfNeeded();
        
        if (!morningFetchCompleted.get()) {
            log.info("Starting scheduled morning stock indices fetch at 9:30 AM");
            fetchStockIndicesWithRetry(true);
        } else {
            log.debug("Morning stock indices already fetched successfully today");
        }
    }
    
    /**
     * Evening schedule at 4:00 PM IST
     */
    @Scheduled(cron = "${scheduler.stock-indices.evening-fetch:0 0 16 * * *}", zone = "Asia/Kolkata")
    public void scheduleEveningStockIndicesFetch() {
        resetFlagsIfNeeded();
        
        if (!eveningFetchCompleted.get()) {
            log.info("Starting scheduled evening stock indices fetch at 4:00 PM");
            fetchStockIndicesWithRetry(false);
        } else {
            log.debug("Evening stock indices already fetched successfully today");
        }
    }
    
    /**
     * Retry scheduler that runs every X minutes if needed
     */
    @Scheduled(cron = "${scheduler.stock-indices.retry.cron:0 */15 * * * *}", zone = "Asia/Kolkata")
    public void retryFailedFetch() {
        resetFlagsIfNeeded();
        
        // Only retry if we have active failures and haven't exceeded max retries
        if (currentRetryCount > 0 && currentRetryCount < maxRetries) {
            // In dev mode, don't check trading hours
            if (isDevMode()) {
                log.info("Development mode: Attempting retry regardless of time");
                fetchStockIndicesWithRetry(true); // Always retry morning session in dev
                return;
            }
            
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
            int currentHour = now.getHour();
            
            // Morning session: retry between 9:30 AM and 3:30 PM if morning fetch failed
            if (currentHour >= 9 && currentHour < 15 && !morningFetchCompleted.get()) {
                log.info("Retry attempt #{} for morning stock indices fetch", currentRetryCount + 1);
                fetchStockIndicesWithRetry(true);
            }
            // Evening session: retry between 4:00 PM and 6:00 PM if evening fetch failed
            else if (currentHour >= 16 && currentHour < 18 && !eveningFetchCompleted.get()) {
                log.info("Retry attempt #{} for evening stock indices fetch", currentRetryCount + 1);
                fetchStockIndicesWithRetry(false);
            }
        } else if (currentRetryCount >= maxRetries) {
            log.warn("Max retry attempts ({}) reached for stock indices fetch", maxRetries);
            // Reset retry count but keep the completion flags as is
            currentRetryCount = 0;
        }
    }
    
    /**
     * Fetch stock indices with retry logic
     * @param isMorningSession true if this is the morning session, false for evening
     */
    private void fetchStockIndicesWithRetry(boolean isMorningSession) {
        try {
            log.info("Attempting to fetch stock indices data (session: {})", 
                    isMorningSession ? "morning" : "evening");
            
            // Attempt to fetch and process stock indices
            boolean success = marketDataProcessingService.fetchAndProcessStockIndicesOnly();
            
            if (success) {
                log.info("Successfully fetched and processed stock indices data (session: {})",
                        isMorningSession ? "morning" : "evening");
                
                // Mark the appropriate session as completed
                if (isMorningSession) {
                    morningFetchCompleted.set(true);
                } else {
                    eveningFetchCompleted.set(true);
                }
                
                // Reset retry count on success
                currentRetryCount = 0;
            } else {
                // Increment retry count on failure
                currentRetryCount++;
                log.warn("Failed to fetch stock indices data (session: {}). Will retry in {} minutes. Attempt {}/{}",
                        isMorningSession ? "morning" : "evening", 
                        retryIntervalMinutes, 
                        currentRetryCount, 
                        maxRetries);
            }
        } catch (CookieException e) {
            currentRetryCount++;
            log.error("Cookie error during stock indices fetch (session: {}): {}. Will retry in {} minutes. Attempt {}/{}",
                    isMorningSession ? "morning" : "evening",
                    e.getMessage(),
                    retryIntervalMinutes,
                    currentRetryCount,
                    maxRetries);
        } catch (Exception e) {
            currentRetryCount++;
            log.error("Unexpected error during stock indices fetch (session: {}): {}. Will retry in {} minutes. Attempt {}/{}",
                    isMorningSession ? "morning" : "evening",
                    e.getMessage(),
                    retryIntervalMinutes,
                    currentRetryCount,
                    maxRetries,
                    e);
        }
    }
    
    /**
     * Reset flags if it's a new day
     */
    private void resetFlagsIfNeeded() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        
        // If it's a new day, reset all flags
        if (!today.equals(lastProcessedDate)) {
            log.info("New day detected. Resetting stock indices fetch flags");
            lastProcessedDate = today;
            morningFetchCompleted.set(false);
            eveningFetchCompleted.set(false);
            currentRetryCount = 0;
        }
        
        // In dev mode, don't reset flags based on time
        if (isDevMode()) {
            log.debug("Development mode: Not resetting flags based on time");
            return;
        }
        
        // Also reset flags based on current time
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        int currentHour = now.getHour();
        
        // Before 9:30 AM, reset morning flag
        if (currentHour < 9 || (currentHour == 9 && now.getMinute() < 30)) {
            if (morningFetchCompleted.get()) {
                log.debug("Resetting morning fetch flag as it's before 9:30 AM");
                morningFetchCompleted.set(false);
            }
        }
        
        // Before 4:00 PM, reset evening flag
        if (currentHour < 16) {
            if (eveningFetchCompleted.get()) {
                log.debug("Resetting evening fetch flag as it's before 4:00 PM");
                eveningFetchCompleted.set(false);
            }
        }
    }
    
    /**
     * Check if we're running in development mode
     */
    private boolean isDevMode() {
        return System.getenv("SPRING_PROFILES_ACTIVE") != null && 
               System.getenv("SPRING_PROFILES_ACTIVE").equals("dev");
    }
}
