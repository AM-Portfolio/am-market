// package com.am.marketdata.scraper.service.indices;

// import com.am.marketdata.common.model.NSEStockInsidicesData;
// import com.am.marketdata.scraper.client.NSEApiClient;
// import com.am.marketdata.scraper.config.NSEIndicesConfig;
// import com.am.marketdata.scraper.cookie.CookieManager;
// import com.am.common.investment.service.StockIndicesMarketDataService;
// import io.micrometer.core.instrument.MeterRegistry;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
// import org.springframework.stereotype.Service;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.concurrent.CompletableFuture;
// import java.util.stream.Collectors;

// @Service
// @RequiredArgsConstructor
// @Slf4j
// public class StockIndicesProcessingService {
//     private static final String METRIC_PREFIX = "market.data.indices.";
//     private static final String METRIC_FETCH_TIME = METRIC_PREFIX + "fetch.time";
//     private static final String METRIC_PROCESS_TIME = METRIC_PREFIX + "process.time";
//     private static final String METRIC_SUCCESS_COUNT = METRIC_PREFIX + "success.count";
//     private static final String METRIC_FAILURE_COUNT = METRIC_PREFIX + "failure.count";
//     private static final String METRIC_RETRY_COUNT = METRIC_PREFIX + "retry.count";

//     private final NSEApiClient nseApiClient;
//     private final StockIndicesMarketDataService stockIndicesMarketDataService;
//     private final MeterRegistry meterRegistry;
//     private final CookieManager cookieManager;
//     private final NSEIndicesConfig nseIndicesConfig;
//     private final ThreadPoolTaskExecutor marketDataExecutor;

//     @Value("${market.data.max.retries:3}")
//     private int maxRetries;

//     @Value("${market.data.retry.delay.ms:1000}")
//     private long retryDelayMs;

//     /**
//      * Process stock indices data for specific symbols
//      * @param indexSymbols List of index symbols to process
//      * @return true if any indices were successfully processed
//      */
//     public boolean processStockIndices(List<String> indexSymbols) {
//         try {
//             // Refresh cookies if needed
//             cookieManager.refreshIfNeeded();
            
//             log.info("Starting stock indices processing for {} indices: {}", 
//                     indexSymbols.size(), indexSymbols);
            
//             // Create a list to hold all futures
//             List<CompletableFuture<Boolean>> futures = indexSymbols.stream()
//                 .map(indexSymbol -> CompletableFuture.supplyAsync(() -> {
//                     try {
//                         long startTime = System.currentTimeMillis();
//                         NSEStockInsidicesData data = nseApiClient.getStockIndices(indexSymbol);
//                         if (data != null) {
//                             processStockIndicesData(data);
//                             recordMetrics(startTime, true, indexSymbol);
//                             return true;
//                         }
//                         recordMetrics(startTime, false, indexSymbol);
//                         return false;
//                     } catch (Exception e) {
//                         log.error("Error processing stock index {}: {}", indexSymbol, e.getMessage());
//                         meterRegistry.counter(METRIC_FAILURE_COUNT, "symbol", indexSymbol).increment();
//                         return false;
//                     }
//                 }, marketDataExecutor))
//                 .collect(Collectors.toList());

//             // Wait for all futures to complete
//             CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
//             // Check if any future completed successfully
//             boolean anySuccess = futures.stream()
//                     .map(CompletableFuture::join)
//                     .anyMatch(success -> success);
                    
//             if (anySuccess) {
//                 log.info("Successfully processed some stock indices data");
//             } else {
//                 log.warn("Failed to process any stock indices data");
//             }
            
//             return anySuccess;
//         } catch (Exception e) {
//             log.error("Error in stock indices processing: {}", e.getMessage());
//             return false;
//         }
//     }

//     /**
//      * Process all configured indices (broad market and sector indices)
//      * @return true if any indices were successfully processed
//      */
//     public boolean processAllConfiguredIndices() {
//         List<String> allIndices = new ArrayList<>();
//         allIndices.addAll(nseIndicesConfig.getBroadMarketIndices());
//         allIndices.addAll(nseIndicesConfig.getSectorIndices());
//         return processStockIndices(allIndices);
//     }

//     private void processStockIndicesData(com.am.marketdata.common.model.NSEStockInsidicesData data) {
//         try {
//             stockIndicesMarketDataService.saveStockIndicesData(data);
//             meterRegistry.counter(METRIC_SUCCESS_COUNT, "symbol", data.getIndexSymbol()).increment();
//         } catch (Exception e) {
//             log.error("Error saving stock indices data: {}", e.getMessage());
//             meterRegistry.counter(METRIC_FAILURE_COUNT, "symbol", data.getIndexSymbol()).increment();
//             throw e;
//         }
//     }

//     private void recordMetrics(long startTime, boolean success, String symbol) {
//         long processingTime = System.currentTimeMillis() - startTime;
//         meterRegistry.timer(METRIC_PROCESS_TIME, "symbol", symbol).record(processingTime, TimeUnit.MILLISECONDS);
//         if (success) {
//             meterRegistry.counter(METRIC_SUCCESS_COUNT, "symbol", symbol).increment();
//         } else {
//             meterRegistry.counter(METRIC_FAILURE_COUNT, "symbol", symbol).increment();
//         }
//     }
// }
