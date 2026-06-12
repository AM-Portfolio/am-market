package com.am.marketdata.scheduler;

import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.marketdata.common.MarketDataProvider;
import com.am.marketdata.service.MarketDataCacheService;
import com.am.marketdata.service.SymbolOrchestratorService;
import com.am.marketdata.common.log.AppLogger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Scheduler responsible for pre-populating and warming up the Redis cache with
 * previousClose prices for all active portfolio stock symbols and indices from Upstox.
 *
 * This mitigates previous close mismatch issues by caching reliable broker-sourced close
 * prices. The scheduler:
 * 1. Runs automatically daily at 8:00 AM IST.
 * 2. Bootstraps asynchronously on application startup.
 * 3. Skips execution on weekends (Saturday/Sunday in Indian Timezone) to save API calls.
 */
@Component
public class PreviousCloseScheduler {

    private final AppLogger log = AppLogger.getLogger();
    private final SymbolOrchestratorService symbolService;
    private final MarketDataProvider upstoxMarketDataProvider;
    private final MarketDataCacheService cacheService;

    public PreviousCloseScheduler(
            SymbolOrchestratorService symbolService,
            @Qualifier("upstoxMarketDataProvider") MarketDataProvider upstoxMarketDataProvider,
            MarketDataCacheService cacheService) {
        this.symbolService = symbolService;
        this.upstoxMarketDataProvider = upstoxMarketDataProvider;
        this.cacheService = cacheService;
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void init() {
        // Asynchronously bootstrap the cache on application startup so we don't block
        // the main startup thread.
        log.info("PreviousCloseScheduler", "Bootstrapping previous close cache on startup...");
        new Thread(this::fetchAndCachePreviousClose, "prev-close-bootstrap-thread").start();
    }

    /**
     * Helper method to determine if today is a weekend in India (Asia/Kolkata timezone).
     * Prevents calling upstream broker APIs unnecessarily when the market is closed.
     */
    private boolean isWeekend() {
        java.time.DayOfWeek day = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).getDayOfWeek();
        return day == java.time.DayOfWeek.SATURDAY || day == java.time.DayOfWeek.SUNDAY;
    }

    // Run at 8:00 AM IST daily
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Kolkata")
    public void fetchAndCachePreviousClose() {
        // Skip fetching if it's a weekend (Saturday or Sunday) in India
        if (isWeekend()) {
            log.info("PreviousCloseScheduler", "Skipping previous close fetch: Today is a weekend.");
            return;
        }

        log.info("PreviousCloseScheduler", "Starting daily previous close fetch job...");
        try {
            // 1. Retrieve distinct active stock symbols from portfolio/watchlists
            Set<String> symbols = symbolService.findDistinctSymbols();
            if (symbols == null) {
                symbols = new java.util.HashSet<>();
            }

            // 2. Append index symbols explicitly so we cache Nifty/BankNifty closes as well
            List<String> indexSymbols = java.util.Arrays.asList(
                "NIFTY 50", "NIFTY BANK", "NIFTY IT", "NIFTY NEXT 50", "NIFTY MIDCAP 50",
                "NIFTY INFRA", "NIFTY FMCG", "NIFTY METAL", "NIFTY REALTY", "NIFTY ENERGY"
            );
            symbols.addAll(indexSymbols);

            if (symbols.isEmpty()) {
                log.info("PreviousCloseScheduler", "No symbols or indices found to fetch previous close.");
                return;
            }

            List<String> symbolList = new ArrayList<>(symbols);
            
            // 3. Request daily OHLC quotes from Upstox (this utilizes our rate-limiting sleep internally)
            Map<String, OHLCQuote> quotes = upstoxMarketDataProvider.getOHLC(symbolList, TimeFrame.DAY);

            // 4. Cache retrieved previousClose values to Redis (TTL 26 hours)
            if (quotes != null && !quotes.isEmpty()) {
                int count = 0;
                for (Map.Entry<String, OHLCQuote> entry : quotes.entrySet()) {
                    String symbol = entry.getKey();
                    OHLCQuote quote = entry.getValue();
                    if (quote != null && quote.getPreviousClose() != 0.0) {
                        cacheService.setPreviousClose(symbol, quote.getPreviousClose());
                        count++;
                    }
                }
                log.info("PreviousCloseScheduler", "Successfully cached previous close for " + count + " symbols.");
            } else {
                log.warn("PreviousCloseScheduler", "Failed to fetch previous close: API returned empty or null.");
            }
        } catch (Exception e) {
            log.error("PreviousCloseScheduler", "Error executing previous close scheduler", e);
        }
    }
}
