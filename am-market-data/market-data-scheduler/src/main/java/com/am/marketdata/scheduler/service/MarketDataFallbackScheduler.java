package com.am.marketdata.scheduler.service;

import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.service.MarketDataService;
import com.am.marketdata.service.websocket.service.StreamerManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fallback data scheduler for non-market hours.
 * Fetches OHLC data via API when streaming is not active and publishes to
 * Kafka/WebSocket.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataFallbackScheduler {

    private final MarketDataService marketDataService;
    private final StreamerManager streamerManager;

    /**
     * Fallback data fetcher for non-market hours
     * Runs every 5 minutes to keep data fresh when market is closed
     */
    @Scheduled(cron = "0 */5 * * * *") // Every 5 minutes
    public void executeFallbackDataFetch() {

        // Check if streamer is connected (market hours)
        if (streamerManager.isStreaming()) {
            log.debug("Streamer is active - skipping fallback (market hours)");
            return;
        }

        log.info("⏰ Streamer inactive - fetching fallback data via API");

        try {
            // Get all subscribed symbols from StreamerManager
            Set<String> subscribedSymbols = streamerManager.getSubscribedSymbols();

            if (subscribedSymbols == null || subscribedSymbols.isEmpty()) {
                log.debug("No subscribed symbols - skipping fallback");
                return;
            }

            List<String> rawSymbols = new ArrayList<>(subscribedSymbols);
            log.info("Fetching fallback OHLC for {} symbols", rawSymbols.size());

            // Call MarketDataService with forceRefresh=false (cache first)
            Map<String, OHLCQuote> ohlcData = marketDataService.getOHLC(
                    rawSymbols,
                    TimeFrame.DAY,
                    false, // forceRefresh=false - use cache
                    "UPSTOX");

            if (ohlcData != null && !ohlcData.isEmpty()) {
                log.info("✅ Fetched {} OHLC quotes - publishing via StreamerManager", ohlcData.size());

                // Publish through StreamerManager (Kafka + WebSocket)
                streamerManager.publishFallbackData(ohlcData);
            } else {
                log.warn("No OHLC data returned from API");
            }

        } catch (Exception e) {
            log.error("Error in fallback data fetch: {}", e.getMessage(), e);
        }
    }
}
