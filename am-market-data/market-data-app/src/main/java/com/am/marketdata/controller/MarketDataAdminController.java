package com.am.marketdata.controller;

import com.am.marketdata.api.model.ipo.IpoApiMapper;
import com.am.marketdata.api.model.ipo.IpoSyncResponse;
import com.am.marketdata.api.model.nse.NseCookiesSetRequest;
import com.am.marketdata.api.model.nse.NseCookiesStatusResponse;
import com.am.marketdata.common.ipo.IpoFeedScope;
import com.am.marketdata.internal.model.IngestionJobLog;
import com.am.marketdata.internal.repository.IngestionJobLogRepository;
import com.am.marketdata.internal.service.MarketDataHistoricalSyncService;
import com.am.marketdata.internal.service.MarketDataIngestionService;
import com.am.marketdata.scraper.cookie.CookieCache;
import com.am.marketdata.scraper.cookie.CookieManager;
import com.am.marketdata.scraper.exception.CookieException;
import com.am.marketdata.service.ipo.IpoSyncTrigger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Profile("!isolated")
@Tag(name = "Admin", description = "Admin sync and ops endpoints")
public class MarketDataAdminController {

    private final IngestionJobLogRepository ingestionJobLogRepository;
    private final MarketDataHistoricalSyncService historicalSyncService;
    private final MarketDataIngestionService ingestionService;
    private final com.am.marketdata.scheduler.service.MarketDataOrchestrator orchestrator;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final com.am.marketdata.scheduler.PreviousCloseScheduler previousCloseScheduler;
    private final com.am.marketdata.scraper.service.MarketDataProcessingService marketDataProcessingService;
    private final com.am.marketdata.service.calendar.MarketCalendarSyncService marketCalendarSyncService;
    private final com.am.marketdata.service.ipo.IpoSyncService ipoSyncService;
    private final CookieManager cookieManager;

    @GetMapping("/logs/{jobId}")
    public ResponseEntity<IngestionJobLog> getJobDetails(@PathVariable String jobId) {
        return ingestionJobLogRepository.findByJobId(jobId)
                .map(job -> {
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
        }
        return ingestionJobLogRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime"))).getContent();
    }

    @PostMapping("/sync/historical")
    public ResponseEntity<String> triggerHistoricalSync(
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String duration,
            @RequestParam(defaultValue = "true") boolean forceRefresh,
            @RequestParam(defaultValue = "false") boolean fetchIndexStocks) {
        log.info(
                "Manual trigger: Historical Sync (Symbol: {}, Duration: {}, Force Refresh: {}, Fetch Index Stocks: {})",
                symbol, duration,
                forceRefresh, fetchIndexStocks);
        new Thread(() -> historicalSyncService.syncHistoricalData(symbol, duration, forceRefresh, fetchIndexStocks))
                .start();
        return ResponseEntity.ok("Historical Sync Triggered (Symbol: " + symbol + ", Duration: " + duration
                + ", Force: " + forceRefresh + ")");
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

    @PostMapping("/scheduler/indices/process")
    public ResponseEntity<String> triggerIndicesProcessing() {
        log.info("Manual trigger: Indices Data Processing");
        orchestrator.triggerIndicesDataProcessing();
        return ResponseEntity.ok("Triggered Indices Data Processing");
    }

    @PostMapping("/scheduler/indices/force-process")
    public ResponseEntity<String> forceTriggerIndicesProcessing() {
        log.info("Manual trigger: Force Indices Data Processing (bypassing trading hours)");
        new Thread(() -> {
            try {
                marketDataProcessingService.fetchAndProcessMarketData();
            } catch (Exception e) {
                log.error("Failed to run manually triggered force indices processing", e);
            }
        }, "manual-force-indices-thread").start();
        return ResponseEntity.ok("Force triggered indices processing in background");
    }

    @PostMapping("/scheduler/indices/retry")
    public ResponseEntity<String> triggerIndicesRetry() {
        log.info("Manual trigger: Stock Indices Retry");
        orchestrator.triggerStockIndicesRetry();
        return ResponseEntity.ok("Triggered Stock Indices Retry");
    }

    @PostMapping("/scheduler/cookie/refresh")
    @Operation(summary = "Trigger Selenium cookie refresh (writer)",
            description = "Scrapes NSE cookies and stores them in Redis for all pods")
    public ResponseEntity<String> triggerCookieRefresh() {
        log.info("Manual trigger: Cookie Refresh");
        orchestrator.triggerCookieRefresh();
        return ResponseEntity.ok("Triggered Cookie Refresh");
    }

    @PutMapping("/nse/cookies")
    @Operation(summary = "Set NSE cookies from browser Cookie header",
            description = "Writes shared Redis cookie store used by IPO sync and NSE API calls. Never returns raw cookie values.")
    public ResponseEntity<NseCookiesStatusResponse> setNseCookies(@RequestBody NseCookiesSetRequest request) {
        try {
            CookieCache.CookiePresenceStatus status =
                    cookieManager.setCookiesFromHeader(request.getCookieHeader(), request.getTtlMinutes());
            return ResponseEntity.ok(toStatusResponse(status, "ok", null));
        } catch (CookieException e) {
            log.warn("Failed to set NSE cookies: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(NseCookiesStatusResponse.builder()
                            .status("failed")
                            .error(e.getMessage())
                            .present(false)
                            .build());
        }
    }

    @GetMapping("/nse/cookies/status")
    @Operation(summary = "NSE cookie cache status",
            description = "Returns presence, cookie names, TTL. Never returns raw cookie values.")
    public ResponseEntity<NseCookiesStatusResponse> getNseCookiesStatus() {
        return ResponseEntity.ok(toStatusResponse(cookieManager.status(), "ok", null));
    }

    private static NseCookiesStatusResponse toStatusResponse(
            CookieCache.CookiePresenceStatus status, String outcome, String error) {
        return NseCookiesStatusResponse.builder()
                .present(status.present())
                .cookieNames(status.cookieNames())
                .storedAt(status.storedAt())
                .ttlSecondsRemaining(status.ttlSecondsRemaining())
                .redisBacked(status.redisBacked())
                .status(outcome)
                .error(error)
                .build();
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
        orchestrator.triggerIngestionStop();
        return ResponseEntity.ok("Triggered Market Close/Ingestion Stop");
    }

    @PostMapping("/scheduler/prev-close/trigger")
    public ResponseEntity<String> triggerPreviousCloseFetch() {
        log.info("Manual trigger: Previous Close Fetch and Cache");
        new Thread(() -> {
            try {
                previousCloseScheduler.fetchAndCachePreviousClose();
            } catch (Exception e) {
                log.error("Failed to run manually triggered previous close fetch", e);
            }
        }, "manual-prev-close-trigger-thread").start();
        return ResponseEntity.ok("Triggered previous close cache refresh in background");
    }

    @PostMapping("/sync/market-calendar")
    public ResponseEntity<String> syncMarketCalendar(
            @RequestParam(defaultValue = "NSE") String exchange) {
        log.info("Manual trigger: Market calendar sync exchange={}", exchange);
        new Thread(() -> {
            try {
                marketCalendarSyncService.sync(
                        exchange,
                        com.am.marketdata.service.calendar.MarketCalendarSyncTrigger.ADMIN);
            } catch (Exception e) {
                log.error("Failed market calendar sync for {}", exchange, e);
            }
        }, "manual-market-calendar-sync").start();
        return ResponseEntity.ok("Market calendar sync triggered for " + exchange);
    }

    @PostMapping("/sync/ipo")
    public ResponseEntity<IpoSyncResponse> syncIpo(
            @RequestParam(defaultValue = "all") String scope) {
        log.info("Manual trigger: IPO sync scope={}", scope);
        IpoFeedScope feedScope;
        try {
            feedScope = IpoFeedScope.valueOf(scope.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(IpoSyncResponse.builder()
                            .scope(scope)
                            .status("failed")
                            .error("Invalid scope. Use past|current|upcoming|subscription|all")
                            .build());
        }
        if (!ipoSyncService.isSourceAvailable()) {
            return ResponseEntity.status(503)
                    .body(IpoSyncResponse.builder()
                            .scope(feedScope.name())
                            .status("failed")
                            .error("IPO source not configured")
                            .build());
        }
        try {
            int n = ipoSyncService.sync(feedScope, IpoSyncTrigger.ADMIN);
            return ResponseEntity.ok(IpoSyncResponse.builder()
                    .scope(feedScope.name())
                    .status("ok")
                    .upserts(n)
                    .meta(IpoApiMapper.toSyncMeta(ipoSyncService.findSyncMeta(feedScope).orElse(null)))
                    .build());
        } catch (Exception e) {
            log.error("Failed IPO sync scope={}", scope, e);
            return ResponseEntity.status(500)
                    .body(IpoSyncResponse.builder()
                            .scope(feedScope.name())
                            .status("failed")
                            .error(e.getMessage())
                            .meta(IpoApiMapper.toSyncMeta(ipoSyncService.findSyncMeta(feedScope).orElse(null)))
                            .build());
        }
    }
}
