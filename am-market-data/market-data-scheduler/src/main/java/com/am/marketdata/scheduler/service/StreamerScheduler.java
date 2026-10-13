package com.am.marketdata.scheduler.service;

import com.am.marketdata.service.SymbolOrchestratorService;
import com.am.marketdata.service.websocket.service.StreamerManager;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for the Indian NSE market data stream.
 *
 * <p><b>WebSocket Voting Strategy (IMPORTANT):</b>
 * This scheduler does NOT directly call {@code startStreaming()} or {@code stopStreaming()}
 * on the {@code StreamerManager}. Instead, it only updates the Indian market vote flag:
 * <pre>
 *   market:websocket:vote:indian = "true"  → during NSE trading hours
 *   market:websocket:vote:indian = "false" → after NSE market close
 * </pre>
 *
 * <p>The actual WebSocket connect/disconnect decision is made exclusively by the
 * {@code StreamerManager.reconcileWebSocketConnection()} method, which reads BOTH:
 * <ul>
 *   <li>{@code market:websocket:vote:indian} (set here)</li>
 *   <li>{@code market:websocket:vote:global} (set by GlobalMarketScheduleService)</li>
 * </ul>
 * and connects if either is "true", disconnecting ONLY if BOTH are "false".
 *
 * <p><b>Why this matters:</b>
 * If this scheduler called {@code stopStreaming()} directly at 3:30 PM IST,
 * it would kill the WebSocket even when the US market is open (9:30 PM IST),
 * breaking global index streaming. The voting pattern prevents this conflict.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreamerScheduler {

    private final StreamerManager streamerManager;
    private final SymbolOrchestratorService symbolService;
    private final StringRedisTemplate redisTemplate;

    /**
     * Redis key for the Indian market WebSocket vote.
     * This must match the constant in StreamerManager.
     */
    private static final String INDIAN_VOTE_KEY = "market:websocket:vote:indian";

    /**
     * Marks the Indian market as open at 9:00 AM IST (Mon-Fri).
     *
     * <p>Sets the Indian vote flag to "true" and refreshes WebSocket subscriptions.
     * The {@code StreamerManager} reconciler will pick this up within 5 minutes
     * (or immediately if the market is the first to open today) and connect the WebSocket.
     *
     * <p>Also refreshes the subscription set to pick up any new symbols added to
     * the portfolio/watchlist overnight.
     */
    @Scheduled(cron = "${scheduler.stream.start-cron:0 0 9 ? * MON-FRI}", zone = "Asia/Kolkata")
    public void executeStartStreaming() {
        log.info("⏰ [StreamerScheduler] NSE market opening: setting vote:indian = true (9:00 AM IST)");

        // Set Indian vote to true — the StreamerManager reconciler will connect the WebSocket
        // within its next 5-minute cycle (or sooner if already scheduled to run)
        redisTemplate.opsForValue().set(INDIAN_VOTE_KEY, "true");

        // Refresh subscriptions so any new portfolio/watchlist symbols added overnight
        // are included in the WebSocket subscription set when the reconciler connects
        Set<String> instrumentKeys = symbolService.findDistinctSymbols();
        log.info("[StreamerScheduler] Refreshing subscriptions for {} instrument keys", 
                instrumentKeys != null ? instrumentKeys.size() : 0);
        streamerManager.refreshSubscriptions();

        log.info("✅ [StreamerScheduler] vote:indian = true set. WebSocket reconciler will connect shortly.");
    }

    /**
     * During NSE cash hours (9AM-3PM), pick up newly added portfolio/watchlist symbols.
     *
     * <p>Runs every 2 minutes during market hours to subscribe to any symbols added
     * to the portfolio or watchlist after the market opened at 9:00 AM.
     */
    @Scheduled(cron = "${scheduler.stream.refresh-cron:0 */2 9-15 * * MON-FRI}", zone = "Asia/Kolkata")
    public void executeRefreshSubscriptions() {
        if (!streamerManager.isStreaming()) {
            return;
        }
        log.info("[StreamerScheduler] Refreshing Upstox subscriptions for newly added portfolio/watchlist symbols");
        streamerManager.refreshSubscriptions();
    }

    /**
     * Marks the Indian market as closed at 3:30 PM IST (Mon-Fri).
     *
     * <p>Sets the Indian vote flag to "false". The {@code StreamerManager} reconciler
     * will evaluate both vote keys and ONLY disconnect the WebSocket if the global
     * market is ALSO closed. If global markets (US, Europe) are still open,
     * the WebSocket remains connected for global index streaming.
     *
     * <p><b>CRITICAL:</b> This method must NOT call {@code streamerManager.stopStreaming()}
     * directly. Doing so would kill the WebSocket even during US/European trading hours.
     */
    @Scheduled(cron = "${scheduler.stream.stop-cron:0 30 15 ? * MON-FRI}", zone = "Asia/Kolkata")
    public void executeStopStreaming() {
        log.info("⏰ [StreamerScheduler] NSE market closing: setting vote:indian = false (3:30 PM IST)");

        // Set Indian vote to false — the reconciler will disconnect ONLY if global vote is also false
        // If US market is open (9:30 PM - 4:00 AM IST), the WebSocket stays connected
        redisTemplate.opsForValue().set(INDIAN_VOTE_KEY, "false");

        log.info("✅ [StreamerScheduler] vote:indian = false set. Reconciler will disconnect if no global market is open.");
    }
}
