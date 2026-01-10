package com.am.marketdata.internal.service;

import com.am.marketdata.api.model.HistoricalDataResponseV1;
import com.am.marketdata.api.service.MarketDataFetchService;
import com.am.marketdata.api.util.InstrumentUtils;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.internal.model.IngestionJobLog;
import com.am.marketdata.internal.model.MarketDataIngestionStatus;
import com.am.marketdata.internal.repository.IngestionJobLogRepository;
import com.am.marketdata.internal.repository.MarketDataIngestionStatusRepository;
import com.am.marketdata.common.log.AppLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketDataHistoricalSyncService {

    private final AppLogger log = AppLogger.getLogger(MarketDataHistoricalSyncService.class);

    private final MarketDataFetchService marketDataFetchService;
    private final InstrumentUtils instrumentUtils;
    private final MarketDataIngestionStatusRepository ingestionStatusRepository;
    private final IngestionJobLogRepository ingestionJobLogRepository;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    private final ExecutorService batchExecutor = Executors.newFixedThreadPool(10);
    private static final int BATCH_SIZE = 20;

    /**
     * Triggered by Scheduler or Admin Controller
     * 
     * @param symbol           Symbol or index to sync (comma-separated)
     * @param forceRefresh     Whether to force refresh from provider
     * @param fetchIndexStocks If true, fetch individual stocks from index symbols;
     *                         if false, keep index symbols as-is
     */
    /**
     * Triggered by Scheduler or Admin Controller
     * 
     * @param symbol           Symbol or index to sync (comma-separated)
     * @param durationStr      Duration string (e.g. "1Y", "5Y", "6M"). If null,
     *                         uses default logic.
     * @param forceRefresh     Whether to force refresh from provider
     * @param fetchIndexStocks If true, fetch individual stocks from index symbols;
     *                         if false, keep index symbols as-is
     */
    public void syncHistoricalData(String symbol, String durationStr, boolean forceRefresh, boolean fetchIndexStocks) {
        String jobId = UUID.randomUUID().toString();
        LocalDateTime startTime = LocalDateTime.now();
        log.info("syncHistoricalData",
                "Starting Historical Data Sync Job: {} (Symbol: {}, Duration: {}, Force: {}, ExpandIndex: {})",
                jobId, symbol, durationStr, forceRefresh, fetchIndexStocks);

        IngestionJobLog jobLog = IngestionJobLog.builder()
                .jobId(jobId)
                .startTime(startTime)
                .status("RUNNING")
                .failedSymbols(new ArrayList<>())
                .logs(new ArrayList<>())
                .build();

        addLog(jobLog, "Starting Historical Data Sync Job: " + jobId);
        jobLog = ingestionJobLogRepository.save(jobLog);

        try {
            // 1. Get Symbols and Expand if needed
            Set<String> allSymbols;
            if (symbol != null && !symbol.trim().isEmpty()) {
                allSymbols = instrumentUtils.resolveSymbols(symbol, fetchIndexStocks);
                addLog(jobLog, "Resolved symbols count: " + allSymbols.size());
            } else {
                allSymbols = getAllSymbolsToSync();
            }
            jobLog.setTotalSymbols(allSymbols.size());
            log.info("syncHistoricalData", "Found {} symbols to sync", allSymbols.size());

            // Calculate override start date if duration provided
            LocalDate overrideStartDate = parseDurationStartDate(durationStr);
            if (overrideStartDate != null) {
                log.info("syncHistoricalData", "Using override start date: {}", overrideStartDate);
                addLog(jobLog, "Duration " + durationStr + " resolved to start date: " + overrideStartDate);
            }

            // 2. Resolve Status & Group into Buckets
            Map<LocalDate, List<String>> buckets = groupSymbolsByStartDate(allSymbols, forceRefresh, overrideStartDate);

            int successCount = 0;
            int failureCount = 0;
            long totalPayloadSize = 0;
            List<String> failedSymbols = new ArrayList<>();

            // 3. Process Buckets
            for (Map.Entry<LocalDate, List<String>> entry : buckets.entrySet()) {
                LocalDate fromDate = entry.getKey();
                List<String> symbolsInBucket = entry.getValue();

                log.info("syncHistoricalData", "Processing bucket for date {}: {} symbols", fromDate,
                        symbolsInBucket.size());
                addLog(jobLog, "Processing bucket for date " + fromDate + ": " + symbolsInBucket.size() + " symbols");

                ProcessingResult result = processBucket(symbolsInBucket, fromDate, jobLog, forceRefresh,
                        fetchIndexStocks);
                successCount += result.successCount;
                failureCount += result.failureCount;
                totalPayloadSize += result.totalPayloadSize;
                failedSymbols.addAll(result.failedSymbols);
            }

            // 4. Update Job Log
            jobLog.setEndTime(LocalDateTime.now());
            jobLog.setDurationMs(java.time.Duration.between(startTime, jobLog.getEndTime()).toMillis());
            jobLog.setSuccessCount(successCount);
            jobLog.setFailureCount(failureCount);
            jobLog.setFailedSymbols(failedSymbols);
            jobLog.setPayloadSize(totalPayloadSize);
            jobLog.setStatus(failureCount == 0 ? "SUCCESS" : (successCount > 0 ? "PARTIAL_SUCCESS" : "FAILED"));

            addLog(jobLog, "Job Completed. Saved: " + successCount + ", Failed: " + failureCount + ", Payload: "
                    + (totalPayloadSize / 1024) + " KB");

        } catch (Exception e) {
            log.error("syncHistoricalData", "Fatal error in Historical Data Sync Job", e);
            jobLog.setEndTime(LocalDateTime.now());
            jobLog.setStatus("FAILED");
            jobLog.setMessage(e.getMessage());
            addLog(jobLog, "Fatal Error: " + e.getMessage());
        } finally {
            ingestionJobLogRepository.save(jobLog);
            log.info("syncHistoricalData", "Historical Data Sync Job Completed. Status: {}", jobLog.getStatus());
        }
    }

    private LocalDate parseDurationStartDate(String durationStr) {
        if (durationStr == null || durationStr.trim().isEmpty()) {
            return null;
        }
        String d = durationStr.trim().toUpperCase();
        LocalDate now = LocalDate.now();
        try {
            if (d.endsWith("Y"))
                return now.minusYears(Long.parseLong(d.replace("Y", "")));
            if (d.endsWith("M"))
                return now.minusMonths(Long.parseLong(d.replace("M", "")));
            if (d.endsWith("W"))
                return now.minusWeeks(Long.parseLong(d.replace("W", "")));
            if (d.endsWith("D"))
                return now.minusDays(Long.parseLong(d.replace("D", "")));
        } catch (NumberFormatException e) {
            log.warn("Invalid duration format: {}", durationStr);
        }
        return null; // Fallback to default
    }

    private Set<String> getAllSymbolsToSync() {
        // Fetch NSE 500 Constitutents
        // Assuming "NIFTY 500" is the index we want. Or user mentioned "NSE 500 list".
        // Let's use NIFTY 500 if available, else NIFTY 50.
        Set<String> indices = new HashSet<>(Arrays.asList("NIFTY 50", "NIFTY BANK", "NIFTY 500"));

        // This resolves index to constituents
        Set<String> resolvedSymbols = instrumentUtils.resolveSymbols(new ArrayList<>(indices), true);

        // Add indices themselves
        resolvedSymbols.addAll(indices);

        return resolvedSymbols;
    }

    private Map<LocalDate, List<String>> groupSymbolsByStartDate(Set<String> symbols, boolean forceRefresh,
            LocalDate overrideStartDate) {
        Map<LocalDate, List<String>> buckets = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate defaultStart = today.minusYears(10);

        if (overrideStartDate != null) {
            defaultStart = overrideStartDate;
        }

        log.info("groupSymbolsByStartDate", "Grouping {} symbols (forceRefresh: {}, overrideDate: {})", symbols.size(),
                forceRefresh, overrideStartDate);

        List<MarketDataIngestionStatus> statuses = ingestionStatusRepository.findAllById(symbols);
        Map<String, LocalDate> statusMap = statuses.stream()
                .collect(Collectors.toMap(MarketDataIngestionStatus::getSymbol,
                        MarketDataIngestionStatus::getLastIngestionDate));

        log.debug("groupSymbolsByStartDate", "Found {} existing ingestion statuses", statusMap.size());

        List<String> skippedSymbols = new ArrayList<>();
        int processedSymbols = 0;

        for (String symbol : symbols) {
            LocalDate startDate;

            if (forceRefresh || overrideStartDate != null) {
                // Force refresh OR override provided: Use the target start date
                startDate = defaultStart;
            } else {
                // Normal mode: Use incremental sync from last sync date
                LocalDate lastDate = statusMap.get(symbol);
                LocalDate nextDate = (lastDate != null) ? lastDate.plusDays(1) : defaultStart;

                // If already up to date, skip
                if (!nextDate.isBefore(today)) {
                    skippedSymbols.add(symbol);
                    continue;
                }
                startDate = nextDate;
            }

            buckets.computeIfAbsent(startDate, k -> new ArrayList<>()).add(symbol);
            processedSymbols++;
        }

        if (!skippedSymbols.isEmpty()) {
            log.info("groupSymbolsByStartDate", "Skipped {} symbols that are already up to date",
                    skippedSymbols.size());
        }

        return buckets;
    }

    private ProcessingResult processBucket(List<String> symbols, LocalDate fromDate, IngestionJobLog jobLog,
            boolean forceRefresh, boolean fetchIndexStocks) {
        ProcessingResult totalResult = new ProcessingResult();
        LocalDate toDate = LocalDate.now(); // Up to current

        // Batching within bucket
        List<List<String>> batches = chunkList(symbols, BATCH_SIZE);
        List<CompletableFuture<ProcessingResult>> futures = new ArrayList<>();

        for (List<String> batch : batches) {
            CompletableFuture<ProcessingResult> future = CompletableFuture.supplyAsync(() -> {
                return fetchBatch(new HashSet<>(batch), fromDate, toDate, jobLog, forceRefresh, fetchIndexStocks);
            }, batchExecutor);
            futures.add(future);
        }

        // Wait and Combine
        for (CompletableFuture<ProcessingResult> f : futures) {
            try {
                ProcessingResult batchResult = f.join();
                totalResult.add(batchResult);
            } catch (Exception e) {
                log.error("processBucket", "Error processing batch", e);
            }
        }

        return totalResult;
    }

    private ProcessingResult fetchBatch(Set<String> batch, LocalDate fromDate, LocalDate toDate,
            IngestionJobLog jobLog, boolean forceRefresh, boolean fetchIndexStocks) {
        ProcessingResult result = new ProcessingResult();
        try {
            log.info("fetchBatch", "[BATCH_START] Processing batch of {} symbols from {} to {} (forceRefresh: {})",
                    batch.size(), fromDate, toDate, forceRefresh);
            log.info("fetchBatch", "[BATCH_SYMBOLS] Symbols in batch: {}", batch);

            addLogAsync(jobLog, "Fetching batch of " + batch.size() + " symbols from " + fromDate + " to " + toDate
                    + " (Force Refresh: " + forceRefresh + ")");

            Date from = Date.from(fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date to = Date.from(toDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

            log.info("fetchBatch", "[API_CALL] Calling marketDataFetchService.getHistoricalDataMultipleSymbols...");
            long apiStartTime = System.currentTimeMillis();

            // Use getHistoricalDataMultipleSymbols
            HistoricalDataResponseV1 response = marketDataFetchService.getHistoricalDataMultipleSymbols(
                    batch,
                    from,
                    to,
                    TimeFrame.DAY,
                    "STOCK", // Instrument Type
                    new HashMap<>(), // Additional Params
                    true, // Always force refresh from provider for Ingestion Job (we know data is
                          // missing/needed)
                    fetchIndexStocks // Fetch individual stocks from index symbols if true
            );

            long apiDuration = System.currentTimeMillis() - apiStartTime;
            log.info("fetchBatch", "[API_RESPONSE] API call completed in {} ms", apiDuration);

            // Access data from response
            Map<String, com.am.common.investment.model.historical.HistoricalData> dataMap = response.getData();

            // Log response metadata
            String dataSource = (response.getMetadata() != null && response.getMetadata().getSource() != null)
                    ? response.getMetadata().getSource()
                    : "UNKNOWN";
            log.info("fetchBatch", "[RESPONSE_SOURCE] Data source: {}", dataSource);

            if (dataMap == null) {
                log.warn("fetchBatch", "[RESPONSE_ERROR] Response data map is NULL for batch");
                addLogAsync(jobLog, "ERROR: Response data map is NULL for batch");
                result.failureCount += batch.size();
                result.failedSymbols.addAll(batch);
                return result;
            }

            log.info("fetchBatch", "[RESPONSE_DATA] Received data for {} symbols out of {} requested",
                    dataMap.size(), batch.size());

            // Track statistics
            int totalDataPoints = 0;
            Map<String, Integer> symbolDataCounts = new HashMap<>();

            for (String symbol : batch) {
                if (dataMap.containsKey(symbol) &&
                        dataMap.get(symbol).getDataPoints() != null &&
                        !dataMap.get(symbol).getDataPoints().isEmpty()) {

                    int dataPointCount = dataMap.get(symbol).getDataPoints().size();
                    totalDataPoints += dataPointCount;
                    symbolDataCounts.put(symbol, dataPointCount);

                    log.info("fetchBatch", "[SYMBOL_SUCCESS] {} - Retrieved {} data points from {}",
                            symbol, dataPointCount, dataSource);

                    // Log a sample data point at DEBUG level
                    if (!dataMap.get(symbol).getDataPoints().isEmpty()) {
                        var firstPoint = dataMap.get(symbol).getDataPoints().get(0);
                        log.debug("fetchBatch", "[SAMPLE_DATA] {} - First point: time={}, O={}, H={}, L={}, C={}, V={}",
                                symbol, firstPoint.getTime(), firstPoint.getOpen(), firstPoint.getHigh(),
                                firstPoint.getLow(), firstPoint.getClose(), firstPoint.getVolume());
                    }

                    // Success
                    result.successCount++;
                    updateStatus(symbol, toDate.minusDays(0)); // Start of today? or Yesterday?
                    // Usually we consider data synced up to "yesterday" if we ran in morning.
                    // But if market is open/closed, let's just mark the date we fetched.
                    // If we fetched "to" date, we mark "to". (Since toDate is NOT inclusive in some
                    // APIs, but here we passed it)
                    // MarketDataFetchService usually treats 'to' as inclusive.
                    // We mark 'toDate' as synced.
                    // Wait, if toDate is today, and market is open?
                    // The job runs at 7:15 AM. Market is closed. So "Today" effectively means "data
                    // up to yesterday close".
                    // Actually, if we fetch with to=Today 7:15 AM, provider gives up to Yesterday
                    // Close.
                    // So we can mark "Yesterday" as the last sync date.
                    // Let's use `toDate.minusDays(1)` to be safe if running early morning.
                    // Actually, let's mark the date of the LAST candle we got.
                    // But that requires parsing the candle.
                    // Simple logic: we asked for data up to `toDate`. If success, we mark `toDate`
                    // (or `toDate -1` since job is pre-market).
                    // Let's mark `toDate.minusDays(1)`.

                } else {
                    // Fail or Empty
                    String reason = !dataMap.containsKey(symbol) ? "NOT_IN_RESPONSE"
                            : (dataMap.get(symbol) == null ? "NULL_DATA"
                                    : (dataMap.get(symbol).getDataPoints() == null ? "NULL_DATAPOINTS"
                                            : "EMPTY_DATAPOINTS"));

                    log.warn("fetchBatch", "[SYMBOL_FAILED] {} - Reason: {}", symbol, reason);
                    addLogAsync(jobLog, "FAILED: " + symbol + " - " + reason);
                    result.failureCount++;
                    result.failedSymbols.add(symbol);
                }
            }

            // Summary log for the batch
            log.info("fetchBatch",
                    "[BATCH_SUMMARY] Processed {} symbols: {} succeeded, {} failed, {} total data points from {}",
                    batch.size(), result.successCount, result.failureCount, totalDataPoints, dataSource);
            addLogAsync(jobLog, "Batch Summary: " + result.successCount + " succeeded, " + result.failureCount +
                    " failed, " + totalDataPoints + " data points from " + dataSource);

            // Estimate Payload Size (very rough: JSON string length or object grap)
            // Since we don't have serialized size here easily without overhead,
            // we'll estimate based on data points count * avg size (e.g., 50 bytes per
            // candle)
            // or just rely on a simple heuristic if available.
            // Better: if 'response' came from HTTP, we might not have content length here.
            // Let's iterate data points to count.
            long batchSize = 0;
            if (dataMap != null) {
                for (var entry : dataMap.entrySet()) {
                    if (entry.getValue().getDataPoints() != null) {
                        // timestamp(8) + open(8) + high(8) + low(8) + close(8) + volume(8) ~ 48 bytes +
                        // overhead
                        // Let's assume ~100 bytes per record for JSON representation
                        batchSize += entry.getValue().getDataPoints().size() * 100L;
                    }
                }
            }
            result.totalPayloadSize += batchSize;
            addLogAsync(jobLog, "Batch processed. Estimated size: " + (batchSize / 1024) + " KB");

        } catch (Exception e) {
            log.error("fetchBatch", "Error fetching batch starting {}", fromDate, e);
            result.failureCount += batch.size();
            result.failedSymbols.addAll(batch);
        }
        return result;
    }

    private void updateStatus(String symbol, LocalDate date) {
        MarketDataIngestionStatus status = MarketDataIngestionStatus.builder()
                .symbol(symbol)
                .lastIngestionDate(date)
                .lastUpdateTimestamp(LocalDateTime.now())
                .lastStatus("SUCCESS")
                .build();
        ingestionStatusRepository.save(status);
    }

    private <T> List<List<T>> chunkList(List<T> list, int size) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            chunks.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return chunks;
    }

    private static class ProcessingResult {
        int successCount = 0;
        int failureCount = 0;
        long totalPayloadSize = 0; // Track payload size
        List<String> failedSymbols = new ArrayList<>();

        void add(ProcessingResult other) {
            this.successCount += other.successCount;
            this.failureCount += other.failureCount;
            this.totalPayloadSize += other.totalPayloadSize;
            this.failedSymbols.addAll(other.failedSymbols);
        }
    }

    private void addLog(IngestionJobLog jobLog, String message) {
        String timestamp = LocalDateTime.now().toString();
        String logEntry = "[" + timestamp + "] " + message;

        // Push to Redis List
        String key = "job:logs:" + jobLog.getJobId();
        redisTemplate.opsForList().rightPush(key, logEntry);
        // Set TTL to 24 hours (86400 seconds) if it doesn't exist or refresh it
        redisTemplate.expire(key, java.time.Duration.ofHours(24));
    }

    // Thread-safe log addition for async operations
    private void addLogAsync(IngestionJobLog jobLog, String message) {
        addLog(jobLog, message);
    }
}
