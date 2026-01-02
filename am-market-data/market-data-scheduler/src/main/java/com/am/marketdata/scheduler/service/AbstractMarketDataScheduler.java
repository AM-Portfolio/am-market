package com.am.marketdata.scheduler.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract base class for all market data schedulers
 * Implements the Template Method pattern for scheduling operations
 * 
 * @param <T> The type of data operation this scheduler handles
 */
@Slf4j
public abstract class AbstractMarketDataScheduler<T> {

    private static final String TAG_OPERATION_TYPE = "operation.type";
    
    protected final MeterRegistry meterRegistry;
    protected final ISINService isinService;
    protected final Timer executionTimer;
    
    @Value("${scheduler.timezone:Asia/Kolkata}")
    protected String timeZone;

    /**
     * Constructor for AbstractMarketDataScheduler
     * 
     * @param meterRegistry Metrics registry for recording metrics
     * @param isinService Service to retrieve symbols for processing
     * @param metricPrefix Prefix for metrics related to this scheduler
     * @param operationType Type of operation for tagging metrics
     */
    protected AbstractMarketDataScheduler(
            MeterRegistry meterRegistry,
            ISINService isinService,
            String metricPrefix,
            String operationType) {
        this.meterRegistry = meterRegistry;
        this.isinService = isinService;
        
        this.executionTimer = Timer.builder(metricPrefix + ".execution.time")
                .description("Time taken to execute " + operationType + " processing")
                .tag(TAG_OPERATION_TYPE, operationType)
                .register(meterRegistry);
    }

    /**
     * Initialize the scheduler
     * Template method that can be overridden by subclasses
     */
    @PostConstruct
    public void initialize() {
        log.info("Initializing {} scheduler", getSchedulerName());
        if (isWithinTradingHours()) {
            log.info("Within trading hours, starting initial {} processing", getSchedulerName());
            process();
        } else {
            log.info("Outside trading hours, skipping initial {} processing", getSchedulerName());
        }
    }

    /**
     * Main scheduling method to be implemented by concrete schedulers
     * This will typically be annotated with @Scheduled in the concrete implementation
     */
    public void scheduleProcessing() {
        MDC.put("scheduler", getSchedulerName());
        MDC.put("execution_time", LocalDateTime.now().toString());
        
        try {
            if (!isWithinTradingHours()) {
                log.info("Outside trading hours, skipping {} processing", getSchedulerName());
                return;
            }

            log.info("Starting scheduled {} processing", getSchedulerName());
            process();
        } catch (Exception e) {
            log.error("Error in {} scheduler: {}", getSchedulerName(), e.getMessage(), e);
            recordFailureMetric();
        } finally {
            MDC.remove("scheduler");
            MDC.remove("execution_time");
        }
    }

    /**
     * Template method for processing market data
     * Implements the workflow with hooks for customization
     * 
     * @return true if processing was successful, false otherwise
     */
    protected boolean process() {
        Timer.Sample sample = Timer.start();
        boolean anySuccess = false;
        
        try {
            List<String> symbols = getSymbolsToProcess();
            log.info("Processing {} data for {} symbols", getSchedulerName(), symbols.size());
            
            // Process each symbol in parallel with CompletableFuture
            CompletableFuture<?>[] futures = symbols.stream()
                .map(this::processSymbolAsync)
                .toArray(CompletableFuture[]::new);
            
            // Wait for all futures to complete
            CompletableFuture.allOf(futures).join();
            
            // Check if any operation succeeded
            for (CompletableFuture<?> future : futures) {
                try {
                    Boolean result = (Boolean) future.get();
                    if (result != null && result) {
                        anySuccess = true;
                    }
                } catch (Exception e) {
                    // Individual failures are already logged
                }
            }
            
            if (anySuccess) {
                log.info("Successfully processed {} data for at least one symbol", getSchedulerName());
                recordSuccessMetric();
            } else {
                log.error("Failed to process {} data for any symbols", getSchedulerName());
                recordFailureMetric();
            }
            
            return anySuccess;
        } catch (Exception e) {
            log.error("Error in {} processing: {}", getSchedulerName(), e.getMessage(), e);
            recordFailureMetric();
            return false;
        } finally {
            sample.stop(executionTimer);
        }
    }

    /**
     * Process a single symbol asynchronously
     * Template method to be implemented by concrete schedulers
     * 
     * @param symbol The symbol to process
     * @return CompletableFuture with the result (true if successful, false otherwise)
     */
    protected abstract CompletableFuture<Boolean> processSymbolAsync(String symbol);

    /**
     * Get the name of this scheduler for logging and metrics
     * 
     * @return The scheduler name
     */
    public abstract String getSchedulerName();

    /**
     * Get the symbols to process
     * Default implementation uses ISINService, but can be overridden
     * 
     * @return List of symbols to process
     */
    protected List<String> getSymbolsToProcess() {
        return isinService.findDistinctIsins();
    }

    /**
     * Record a success metric
     * Template method that can be overridden by subclasses
     */
    protected abstract void recordSuccessMetric();

    /**
     * Record a failure metric
     * Template method that can be overridden by subclasses
     */
    protected abstract void recordFailureMetric();

    /**
     * Check if the current time is within trading hours
     * Template method that can be overridden by subclasses
     * 
     * @return true if within trading hours, false otherwise
     */
    protected boolean isWithinTradingHours() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of(timeZone));
        
        // By default, check if it's a weekend
        DayOfWeek day = now.getDayOfWeek();
        if (isTradingDay(day)) {
            LocalTime time = now.toLocalTime();
            LocalTime marketOpen = getTradingStartTime();
            LocalTime marketClose = getTradingEndTime();

            return !time.isBefore(marketOpen) && !time.isAfter(marketClose);
        }
        
        return true;
    }

    /**
     * Check if the given day is a trading day
     * Uses configuration from application-scheduler.yml
     * 
     * @param day The day to check
     * @return true if it's a trading day, false otherwise
     */
    protected boolean isTradingDay(DayOfWeek day) {
        // Map cron days to DayOfWeek
        Map<String, DayOfWeek> cronDayMap = Map.of(
            "MON", DayOfWeek.MONDAY,
            "TUE", DayOfWeek.TUESDAY,
            "WED", DayOfWeek.WEDNESDAY,
            "THU", DayOfWeek.THURSDAY,
            "FRI", DayOfWeek.FRIDAY,
            "SAT", DayOfWeek.SATURDAY,
            "SUN", DayOfWeek.SUNDAY
        );
        
        // Check if current day is in the trading days
        return cronDayMap.containsKey(day.name().substring(0, 3));
    }

    /**
     * Get the trading start time
     * Must be implemented by concrete schedulers
     * 
     * @return The trading start time
     */
    protected abstract LocalTime getTradingStartTime();

    /**
     * Get the trading end time
     * Must be implemented by concrete schedulers
     * 
     * @return The trading end time
     */
    protected abstract LocalTime getTradingEndTime();
}
