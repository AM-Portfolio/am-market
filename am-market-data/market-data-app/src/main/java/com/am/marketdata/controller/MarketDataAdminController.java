package com.am.marketdata.controller;

import com.am.marketdata.internal.model.IngestionJobLog;
import com.am.marketdata.internal.repository.IngestionJobLogRepository;
import com.am.marketdata.internal.service.MarketDataHistoricalSyncService;
import com.am.marketdata.internal.service.MarketDataIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.context.annotation.Profile;

@Slf4j
@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Profile("!isolated")
public class MarketDataAdminController {

    private final IngestionJobLogRepository ingestionJobLogRepository;
    private final MarketDataHistoricalSyncService historicalSyncService;
    private final MarketDataIngestionService ingestionService;
    private final com.am.marketdata.scheduler.service.MarketDataOrchestrator orchestrator;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @GetMapping("/logs/{jobId}")
    public ResponseEntity<IngestionJobLog> getJobDetails(@PathVariable String jobId) {
        return ingestionJobLogRepository.findByJobId(jobId)
                .map(job -> {
                    // Fetch transient logs from Redis
                    String key = "job:logs:" + jobId;
                    List<String> logs = redisTemplate.opsForList().range(key, 0, -1);
                    job.setLogs(logs);
                    return ResponseEntity.ok(job);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/logs")
    public List<IngestionJobLog> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate != null && endDate != null) {
            return ingestionJobLogRepository.findByStartTimeBetween(
                    startDate.atStartOfDay(),
                    endDate.plusDays(1).atStartOfDay(),
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime")))
                    .getContent();
        } else {
            return ingestionJobLogRepository.findAll(
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime"))).getContent();
        }
    }

    @PostMapping("/sync/historical")
    public ResponseEntity<String> triggerHistoricalSync(
            @RequestParam(required = false) String symbol,
            @RequestParam(defaultValue = "true") boolean forceRefresh,
            @RequestParam(defaultValue = "false") boolean fetchIndexStocks) {
        log.info("Manual trigger: Historical Sync (Symbol: {}, Force Refresh: {}, Fetch Index Stocks: {})", symbol,
                forceRefresh, fetchIndexStocks);
        // Running asynchronously to avoid blocking
        new Thread(() -> historicalSyncService.syncHistoricalData(symbol, forceRefresh, fetchIndexStocks)).start();
        return ResponseEntity.ok("Historical Sync Triggered (Force: " + forceRefresh + ", Fetch Index Stocks: "
                + fetchIndexStocks + ")");
    }

    @PostMapping("/ingestion/start")
    public ResponseEntity<String> startIngestion(@RequestParam(defaultValue = "UPSTOX") String provider,
            @RequestParam(defaultValue = "NIFTY 50,NIFTY BANK") List<String> symbols) {
        log.info("Manual trigger: Start Ingestion");
        ingestionService.startIngestion(symbols, provider, "1D", true, true);
        return ResponseEntity.ok("Ingestion Started");
    }

    @PostMapping("/ingestion/stop")
    public ResponseEntity<String> stopIngestion(@RequestParam String provider) {
        log.info("Manual trigger: Stop Ingestion");
        ingestionService.stopIngestion(provider);
        return ResponseEntity.ok("Ingestion Stopped");
    }

    // --- Scheduler Manual Triggers ---

    @PostMapping("/scheduler/indices/process")
    public ResponseEntity<String> triggerIndicesProcessing() {
        log.info("Manual trigger: Indices Data Processing");
        orchestrator.triggerIndicesDataProcessing();
        return ResponseEntity.ok("Triggered Indices Data Processing");
    }

    @PostMapping("/scheduler/indices/retry")
    public ResponseEntity<String> triggerIndicesRetry() {
        log.info("Manual trigger: Stock Indices Retry");
        orchestrator.triggerStockIndicesRetry();
        return ResponseEntity.ok("Triggered Stock Indices Retry");
    }

    @PostMapping("/scheduler/cookie/refresh")
    public ResponseEntity<String> triggerCookieRefresh() {
        log.info("Manual trigger: Cookie Refresh");
        orchestrator.triggerCookieRefresh();
        return ResponseEntity.ok("Triggered Cookie Refresh");
    }

    @PostMapping("/scheduler/streamer/start")
    public ResponseEntity<String> triggerStreamerStart() {
        log.info("Manual trigger: Streamer Start");
        orchestrator.triggerStreamerStart();
        return ResponseEntity.ok("Triggered Streamer Start");
    }

    @PostMapping("/scheduler/streamer/stop")
    public ResponseEntity<String> triggerStreamerStop() {
        log.info("Manual trigger: Streamer Stop");
        orchestrator.triggerStreamerStop();
        return ResponseEntity.ok("Triggered Streamer Stop");
    }

    @PostMapping("/scheduler/indices/morning")
    public ResponseEntity<String> triggerMorningIndicesFetch() {
        log.info("Manual trigger: Morning Stock Indices Fetch");
        orchestrator.triggerMorningStockIndicesFetch();
        return ResponseEntity.ok("Triggered Morning Stock Indices Fetch");
    }

    @PostMapping("/scheduler/indices/evening")
    public ResponseEntity<String> triggerEveningIndicesFetch() {
        log.info("Manual trigger: Evening Stock Indices Fetch");
        orchestrator.triggerEveningStockIndicesFetch();
        return ResponseEntity.ok("Triggered Evening Stock Indices Fetch");
    }

    @PostMapping("/scheduler/redis/cleanup")
    public ResponseEntity<String> triggerRedisCleanup() {
        log.info("Manual trigger: Redis Cleanup");
        orchestrator.triggerRedisCleanup();
        return ResponseEntity.ok("Triggered Redis Cleanup");
    }

    @PostMapping("/scheduler/market/open")
    public ResponseEntity<String> triggerMarketOpen() {
        log.info("Manual trigger: Market Open Jobs");
        orchestrator.triggerMarketOpenJobs();
        return ResponseEntity.ok("Triggered Market Open Jobs");
    }

    @PostMapping("/scheduler/market/close")
    public ResponseEntity<String> triggerMarketClose() {
        log.info("Manual trigger: Market Close/Ingestion Stop");
        orchestrator.triggerIngestionStop(); // Orchestrator method for market close/stop ingestion
        return ResponseEntity.ok("Triggered Market Close/Ingestion Stop");
    }
}
