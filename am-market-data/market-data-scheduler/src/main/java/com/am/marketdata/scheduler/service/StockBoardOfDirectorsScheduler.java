// package com.am.marketdata.scheduler.service;

// import com.am.marketdata.processor.service.operation.StockOverviewDataOperation;
// import io.micrometer.core.instrument.MeterRegistry;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Service;

// import java.time.LocalTime;
// import java.util.concurrent.CompletableFuture;

// /**
//  * Scheduler service specifically for board of directors data processing
//  * Extends the AbstractMarketDataScheduler for consistent workflow
//  */
// @Slf4j
// @Service
// @ConditionalOnProperty(value = "scheduler.board-of-directors.enabled", havingValue = "true", matchIfMissing = true)
// public class StockBoardOfDirectorsScheduler extends AbstractMarketDataScheduler<StockOverviewDataOperation> {

//     private static final String METRIC_PREFIX = "board.of.directors.scheduler";
//     private static final String METRIC_SUCCESS_COUNT = METRIC_PREFIX + ".success.count";
//     private static final String METRIC_FAILURE_COUNT = METRIC_PREFIX + ".failure.count";
//     private static final String OPERATION_TYPE = "board-of-directors";

//     private final StockOverviewDataOperation stockOverviewDataOperation;

//     @Value("${scheduler.board-of-directors.trading.start-time:00:01}")
//     private String startTime;

//     @Value("${scheduler.board-of-directors.trading.end-time:23:59}")
//     private String endTime;

//     @Value("${scheduler.board-of-directors.timezone:Asia/Kolkata}")
//     private String timeZone;

//     public StockBoardOfDirectorsScheduler(
//             StockOverviewDataOperation stockOverviewDataOperation,
//             ISINService isinService,
//             MeterRegistry meterRegistry) {
//         super(meterRegistry, isinService, METRIC_PREFIX, OPERATION_TYPE);
//         this.stockOverviewDataOperation = stockOverviewDataOperation;
//     }

//     /**
//      * Schedule board of directors processing
//      * Runs according to the configured cron expression
//      */
//     @Override
//     @Scheduled(cron = "${scheduler.board-of-directors.cron:*/2 * * * * MON-SUN}", zone = "${scheduler.board-of-directors.timezone:Asia/Kolkata}")
//     public void scheduleProcessing() {
//         super.scheduleProcessing();
//     }

//     /**
//      * Process a single symbol asynchronously
//      * 
//      * @param symbol The symbol to process
//      * @return CompletableFuture with the result (true if successful, false otherwise)
//      */
//     @Override
//     protected CompletableFuture<Boolean> processSymbolAsync(String symbol) {
//         return stockOverviewDataOperation
//             .withSymbol(symbol)
//             .executeAsync()
//             .exceptionally(ex -> {
//                 log.error("Failed to process board of directors for symbol {}: {}", symbol, ex.getMessage());
//                 return false;
//             });
//     }

//     /**
//      * Get the name of this scheduler for logging and metrics
//      * 
//      * @return The scheduler name
//      */
//     @Override
//     public String getSchedulerName() {
//         return OPERATION_TYPE;
//     }

//     /**
//      * Record a success metric
//      */
//     @Override
//     protected void recordSuccessMetric() {
//         meterRegistry.counter(METRIC_SUCCESS_COUNT, "operation.type", OPERATION_TYPE).increment();
//     }

//     /**
//      * Record a failure metric
//      */
//     @Override
//     protected void recordFailureMetric() {
//         meterRegistry.counter(METRIC_FAILURE_COUNT, "operation.type", OPERATION_TYPE).increment();
//     }

//     /**
//      * Get the trading start time
//      * 
//      * @return The trading start time
//      */
//     @Override
//     protected LocalTime getTradingStartTime() {
//         return LocalTime.parse(startTime);
//     }

//     /**
//      * Get the trading end time
//      * 
//      * @return The trading end time
//      */
//     @Override
//     protected LocalTime getTradingEndTime() {
//         return LocalTime.parse(endTime);
//     }
// }
