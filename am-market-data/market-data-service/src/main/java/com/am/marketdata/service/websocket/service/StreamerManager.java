package com.am.marketdata.service.websocket.service;

import com.am.marketdata.common.MarketDataStreamer;
import com.am.marketdata.common.StreamerListener;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.log.AppLogger;
import com.am.marketdata.common.service.MarketDataPublisher;
import com.am.marketdata.service.MarketDataPersistenceService;
import com.am.marketdata.service.MarketDataCacheService;
import com.am.marketdata.service.SymbolOrchestratorService;
import com.am.marketdata.service.websocket.processor.MarketDataProcessor;
import com.am.marketdata.service.global.GlobalMarketScheduleService;
import com.am.common.investment.persistence.document.global.GlobalIndexConfigRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.am.marketdata.common.model.MarketDataUpdate;
import com.upstox.feeder.MarketUpdateV3;
import com.am.marketdata.provider.upstox.converter.UpstoxV3FeedConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.util.HashMap;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.annotation.PreDestroy;

/**
 * Service to manage the Market Data Streamer (Upstox).
 * Handles lifecycle (Connect/Disconnect) and processes incoming data.
 * Acts as the centralized Source of Truth for market data streaming.
 */
@Service
public class StreamerManager implements StreamerListener {

    private final AppLogger log = AppLogger.getLogger();

    private final MarketDataStreamer streamer;
    private final MarketDataPersistenceService persistenceService;
    private final MarketDataProcessor processor;
    private final SymbolOrchestratorService symbolService;
    private final MarketDataPublisher publisher; // For WebSocket broadcasting
    private final MarketDataCacheService cacheService;
    private final StringRedisTemplate redisTemplate;
    private final com.am.marketdata.service.global.GlobalIndexCacheWriter globalIndexCacheWriter;
    private final ObjectMapper objectMapper;
    private final GlobalIndexConfigRepository globalIndexConfigRepository;
    private final GlobalMarketScheduleService globalMarketScheduleService;
    private final com.am.marketdata.service.MarketHoursService marketHoursService;
    private final Executor cacheBackfillExecutor;
    private final UpstoxV3FeedConverter v3FeedConverter;

    @Value("${market.websocket.reconnect-initial-delay-ms:5000}")
    private long reconnectInitialDelayMs;

    @Value("${market.websocket.reconnect-max-delay-ms:120000}")
    private long reconnectMaxDelayMs;

    @Value("${market.websocket.no-tick-timeout-ms:90000}")
    private long noTickTimeoutMs;

    @Value("${market.websocket.connection-attempt-timeout-ms:20000}")
    private long connectionAttemptTimeoutMs;

    @Value("${scheduler.ingestion.enabled:true}")
    private boolean ingestionEnabled;
    private static final DefaultRedisScript<Long> WRITE_NEWER_GLOBAL_TICK = new DefaultRedisScript<>(
            "local old=redis.call('get', KEYS[2]); " +
                    "if old and tonumber(old) and tonumber(old) >= tonumber(ARGV[1]) then return 0 end; " +
                    "redis.call('set', KEYS[1], ARGV[2]); redis.call('set', KEYS[2], ARGV[1]); return 1",
            Long.class);

    /**
     * Redis key written by the Indian market hours scheduler.
     * Set to "true" during NSE trading hours (9:15 AM - 3:45 PM IST, Mon-Fri).
     * Set to "false" after market close.
     */
    private static final String INDIAN_VOTE_KEY = "market:websocket:vote:indian";

    /**
     * Redis key written by GlobalMarketScheduleService.
     * Set to "true" when any configured global exchange is open.
     * Set to "false" when all global exchanges are closed.
     */
    private static final String GLOBAL_VOTE_KEY = GlobalMarketScheduleService.GLOBAL_VOTE_KEY;

    /**
     * Redis key prefix for global index live prices.
     * Format: market:global-latest:<INSTRUMENT_KEY>
     * Written by the global tick handler in onMessage().
     * Read by GlobalIndexService for the latest price lookup.
     */
    private static final String GLOBAL_LATEST_PREFIX = "market:global-latest:";

    /**
     * Redis key prefix for global index tick timestamps.
     * Format: market:global-timestamp:<INSTRUMENT_KEY>
     * Used by the circuit breaker to detect 30+ min of no-tick during market hours.
     */
    private static final String GLOBAL_TIMESTAMP_PREFIX = "market:global-timestamp:";

    private Set<String> subscribedSymbols = new HashSet<>();
    private static final String DEFAULT_MODE = "full";
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "market-stream-reconnect");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);
    private final AtomicBoolean connectionAttemptInFlight = new AtomicBoolean(false);
    private final AtomicBoolean streamReady = new AtomicBoolean(false);
    private volatile boolean streamingDesired;
    private volatile long lastStreamActivityAt;
    private volatile long connectionAttemptStartedAt;
    private volatile long reconnectDelayMs;

    @Autowired
    public StreamerManager(MarketDataStreamer streamer,
            MarketDataPersistenceService persistenceService,
            MarketDataProcessor processor,
            SymbolOrchestratorService symbolService,
            MarketDataPublisher publisher,
            MarketDataCacheService cacheService,
            StringRedisTemplate redisTemplate,
            com.am.marketdata.service.global.GlobalIndexCacheWriter globalIndexCacheWriter,
            ObjectMapper objectMapper,
            GlobalIndexConfigRepository globalIndexConfigRepository,
            GlobalMarketScheduleService globalMarketScheduleService,
            com.am.marketdata.service.MarketHoursService marketHoursService,
            @Qualifier("cacheBackfillExecutor") Executor cacheBackfillExecutor,
            UpstoxV3FeedConverter v3FeedConverter) {
        this.streamer = streamer;
        this.persistenceService = persistenceService;
        this.processor = processor;
        this.symbolService = symbolService;
        this.publisher = publisher;
        this.cacheService = cacheService;
        this.redisTemplate = redisTemplate;
        this.globalIndexCacheWriter = globalIndexCacheWriter;
        this.objectMapper = objectMapper;
        this.globalIndexConfigRepository = globalIndexConfigRepository;
        this.globalMarketScheduleService = globalMarketScheduleService;
        this.marketHoursService = marketHoursService;
        this.cacheBackfillExecutor = cacheBackfillExecutor;
        this.v3FeedConverter = v3FeedConverter;
    }

    // @PostConstruct
    public void init() {
        // Auto-start disabled to allow controlled start via API/Scheduler
        // login logic will trigger connection via scheduler or user action
    }

    /**
     * When a new pod is deployed / restarted during active market hours, reconcile
     * immediately instead of waiting up to 5 minutes for the scheduled fixedRate task.
     */
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (isMarketWindowOpen()) {
            log.info("StreamerManager", "New pod ready during active market window; reconciling stream connection immediately.");
            reconcileWebSocketConnection();
        }
    }

    public void refreshSubscriptions() {
        Set<String> symbols = symbolService.findDistinctSymbols();
        if (symbols == null) {
            symbols = new HashSet<>();
        }
        
        // Load and merge index symbols from the configuration file so we stream index ticks too
        symbols.addAll(loadIndicesFromYaml());
        try {
            globalIndexConfigRepository.findAll().stream()
                    .map(com.am.common.investment.persistence.document.global.GlobalIndexConfigDocument::getInstrumentKey)
                    .filter(key -> key != null && !key.isBlank())
                    .forEach(symbols::add);
        } catch (Exception e) {
            // A transient global-config DB failure must not prevent existing NSE/equity subscriptions.
            log.warn("StreamerManager", "Could not load global index subscriptions; retaining existing subscriptions", e);
        }

        if (!symbols.isEmpty()) {
            this.subscribedSymbols.clear();
            this.subscribedSymbols.addAll(symbols);

            log.info("StreamerManager",
                    "Refreshed subscriptions with " + subscribedSymbols.size() + " instrument keys (including indices)");

            // Actually subscribe if connected
            if (streamer.isConnected()) {
                log.info("StreamerManager",
                        "Streamer connected - subscribing to " + subscribedSymbols.size() + " symbols");
                try {
                    streamer.subscribe(subscribedSymbols, DEFAULT_MODE);
                } catch (Exception e) {
                    log.warn("StreamerManager", "Failed to subscribe symbols: " + e.getMessage(), e);
                }
            } else {
                log.info("StreamerManager", "Streamer not connected - will subscribe when connection opens");
            }
        } else {
            log.warn("StreamerManager", "No instrument keys found to subscribe");
        }
    }

    /**
     * Subscribe to a specific set of symbols (External Trigger)
     */
    public void subscribe(Set<String> symbols) {
        if (symbols == null || symbols.isEmpty())
            return;

        log.info("StreamerManager", "Received external subscription request for " + symbols.size() + " symbols.");

        // Add to local cache if we want to persist them across reconnects?
        // For now, just pass through to streamer.
        this.subscribedSymbols.addAll(symbols);

        if (!streamer.isConnected()) {
            log.info("StreamerManager", "Streamer not connected. Connecting through the guarded path...");
            streamingDesired = true;
            connectAndSubscribe();
        } else {
            streamer.subscribe(symbols, DEFAULT_MODE);
        }
    }

    /**
     * Start the streamer
     */
    public void startStreaming() {
        log.info("StreamerManager", "Starting Market Data Streamer (Manual/External Trigger)...");
        streamingDesired = true;
        connectAndSubscribe();
    }

    /**
     * Stop the streamer
     */
    public void stopStreaming() {
        log.info("StreamerManager", "Stopping Market Data Streamer (Manual/External Trigger)...");
        streamingDesired = false;
        reconnectScheduled.set(false);
        connectionAttemptInFlight.set(false);
        streamReady.set(false);
        if (streamer.isConnected()) {
            streamer.disconnect();
        }
    }

    // Exposed method for manual control
    public void manualStart() {
        log.info("StreamerManager", "Manual start triggered.");
        streamingDesired = true;
        connectAndSubscribe();
    }

    public void manualStop() {
        log.info("StreamerManager", "Manual stop triggered.");
        stopStreaming();
    }

    /**
     * Get currently subscribed symbols
     * Used by scheduler for fallback data fetch
     */
    public Set<String> getSubscribedSymbols() {
        return new java.util.HashSet<>(subscribedSymbols);
    }

    /**
     * Check if streaming is active
     * 
     * @return true if streamer is connected
     */
    public boolean isStreaming() {
        if (streamer == null || !streamer.isConnected() || !streamReady.get()) {
            return false;
        }
        long lastActivity = lastStreamActivityAt;
        return lastActivity > 0 && System.currentTimeMillis() - lastActivity <= noTickTimeoutMs;
    }

    /**
     * Publish fallback data to Kafka and WebSocket
     * Called by MarketDataFallbackScheduler during non-market hours
     */
    public void publishFallbackData(Map<String, com.am.marketdata.common.model.OHLCQuote> ohlcData) {
        if (ohlcData == null || ohlcData.isEmpty()) {
            return;
        }

        log.info("StreamerManager", "Fallback data received: " + ohlcData.size() + " quotes");

        try {
            // Build QuoteChange map from OHLC data and broadcast to all connected WS clients
            Map<String, MarketDataUpdate.QuoteChange> quotes = new HashMap<>();
            for (Map.Entry<String, com.am.marketdata.common.model.OHLCQuote> entry : ohlcData.entrySet()) {
                String symbol = entry.getKey();
                com.am.marketdata.common.model.OHLCQuote quote = entry.getValue();
                if (quote != null) {
                    double lastPrice = quote.getLastPrice();
                    double open = quote.getOhlc() != null ? quote.getOhlc().getOpen() : 0.0;
                    double high = quote.getOhlc() != null ? quote.getOhlc().getHigh() : 0.0;
                    double low = quote.getOhlc() != null ? quote.getOhlc().getLow() : 0.0;
                    double close = quote.getOhlc() != null ? quote.getOhlc().getClose() : 0.0;
                    double prevClose = quote.getPreviousClose();
                    Double change = null;
                    Double changePercent = null;
                    if (Double.isFinite(prevClose) && prevClose > 0.0) {
                        change = lastPrice - prevClose;
                        changePercent = (change / prevClose) * 100;
                        quote.setChange(change);
                        quote.setChangePercent(changePercent);
                    } else {
                        quote.setChange(null);
                        quote.setChangePercent(null);
                    }

                    quotes.put(symbol, MarketDataUpdate.QuoteChange.builder()
                            .lastPrice(lastPrice)
                            .open(open)
                            .high(high)
                            .low(low)
                            .close(close)
                            .previousClose(prevClose > 0.0 ? prevClose : null)
                            .change(change)
                            .changePercent(changePercent)
                            .build());
                }
            }

            // Broadcast to all connected WebSocket clients (frontend price widgets)
            if (!quotes.isEmpty()) {
                MarketDataUpdate update = MarketDataUpdate.builder()
                        .timestamp(System.currentTimeMillis())
                        .quotes(quotes)
                        .build();
                publisher.publish(update);
            }

            // Also process for Kafka / persistence with explicit FALLBACK provider so closed-market fallback data is never saved as live ticks
            processor.processUpdate(ohlcData, "FALLBACK");
        } catch (Exception e) {
            log.error("StreamerManager", "Error processing fallback data for persistence/Kafka", e);
        }

        log.info("StreamerManager", "Fallback data processed and broadcast successfully");
    }

    private void connectAndSubscribe() {
        if (streamer.isConnected()) {
            log.info("StreamerManager", "Already connected. Ensuring subscriptions.");
            subscribeToSymbols();
            return;
        }

        if (!connectionAttemptInFlight.compareAndSet(false, true)) {
            log.debug("StreamerManager", "Connection attempt already in flight; skipping duplicate connect request.");
            return;
        }

        connectionAttemptStartedAt = System.currentTimeMillis();
        try {
            streamer.setListener(this);
            streamer.connect();
        } catch (Exception e) {
            connectionAttemptInFlight.set(false);
            log.error("StreamerManager", "Connection attempt failed before the provider callback", e);
            scheduleReconnect("connection attempt failed");
        }
    }

    private void subscribeToSymbols() {
        if (subscribedSymbols.isEmpty()) {
            log.warn("StreamerManager", "No symbols to subscribe.");
            return;
        }
        log.info("StreamerManager", "Subscribing to " + subscribedSymbols.size() + " symbols.");
        streamer.subscribe(subscribedSymbols, DEFAULT_MODE);
    }

    // --- WebSocket Voting Reconciler ---

    /**
     * Reconciles the WebSocket connection state based on the two Redis voting flags:
     * <ul>
     *   <li>{@code market:websocket:vote:indian} — set by the Indian market hours scheduler</li>
     *   <li>{@code market:websocket:vote:global} — set by GlobalMarketScheduleService</li>
     * </ul>
     *
     * <p><b>Connect rule:</b> Connect if EITHER vote is "true".
     * <p><b>Disconnect rule:</b> Disconnect ONLY if BOTH votes are "false".
     *
     * <p>This design prevents the Indian market close (3:45 PM IST) from killing the
     * WebSocket while the US market is still open (9:30 PM - 4:00 AM IST).
     * Runs every 5 minutes to align with the scheduler cadence.
     */
    @Scheduled(fixedRateString = "${market.websocket.reconcile-interval-ms:300000}")
    public void reconcileWebSocketConnection() {
        if (!ingestionEnabled) {
            streamingDesired = false;
            reconnectScheduled.set(false);
            connectionAttemptInFlight.set(false);
            streamReady.set(false);
            if (streamer.isConnected()) {
                log.info("StreamerManager", "[Reconciler] Ingestion is disabled; disconnecting the stream.");
                streamer.disconnect();
            }
            return;
        }

        VoteSnapshot votes = readVotesSafely();
        boolean indianMarketOpen = marketHoursService.isMarketOpen();
        boolean globalMarketOpen = globalMarketScheduleService.isAnyMarketCurrentlyOpen(java.time.Instant.now());
        boolean shouldConnect = indianMarketOpen || globalMarketOpen || votes.anyOpen();
        boolean canConfirmClosed = votes.readSuccessfully() && !votes.anyOpen()
                && !indianMarketOpen && !globalMarketOpen;

        log.info("StreamerManager",
                String.format("[Reconciler] localIndian=%s, localGlobal=%s, voteIndian=%s, voteGlobal=%s, redisHealthy=%s → shouldConnect=%s",
                        indianMarketOpen, globalMarketOpen, votes.indianOpen(), votes.globalOpen(),
                        votes.readSuccessfully(), shouldConnect));

        if (shouldConnect) {
            streamingDesired = true;
            if (!streamer.isConnected()) {
                log.info("StreamerManager", "[Reconciler] Connecting WebSocket (market open).");
                connectAndSubscribe();
            }
        } else if (canConfirmClosed) {
            streamingDesired = false;
            reconnectScheduled.set(false);
            if (streamer.isConnected()) {
                log.info("StreamerManager",
                        "[Reconciler] All markets closed. Disconnecting WebSocket.");
                streamer.disconnect();
            }
        } else if (!streamer.isConnected()) {
            scheduleReconnect("market state could not be confirmed");
        }
    }

    /** Redis vote keys are advisory; a failed read must not look like market close. */
    private VoteSnapshot readVotesSafely() {
        try {
            String indian = redisTemplate.opsForValue().get(INDIAN_VOTE_KEY);
            String global = redisTemplate.opsForValue().get(GLOBAL_VOTE_KEY);
            return new VoteSnapshot(
                    "true".equalsIgnoreCase(indian),
                    "true".equalsIgnoreCase(global),
                    true);
        } catch (Exception e) {
            log.warn("StreamerManager", "Redis vote read failed; retaining current stream state and using local schedules", e);
            return new VoteSnapshot(false, false, false);
        }
    }

    @Scheduled(fixedRateString = "${market.websocket.health-interval-ms:30000}")
    public void monitorStreamHealth() {
        if (!streamingDesired) {
            return;
        }

        if (!streamer.isConnected()) {
            if (connectionAttemptInFlight.get()
                    && System.currentTimeMillis() - connectionAttemptStartedAt < connectionAttemptTimeoutMs) {
                return;
            }
            connectionAttemptInFlight.set(false);
            scheduleReconnect("stream is disconnected");
            return;
        }

        long lastActivity = lastStreamActivityAt;
        if (lastActivity > 0 && System.currentTimeMillis() - lastActivity > noTickTimeoutMs
                && isMarketWindowOpen()) {
            log.warn("StreamerManager", "No market tick received for " + noTickTimeoutMs + " ms; resetting the stream");
            try {
                streamer.disconnect();
            } catch (Exception e) {
                log.warn("StreamerManager", "Failed to reset stale stream", e);
            }
            scheduleReconnect("no tick watchdog");
        }
    }

    public boolean isMarketWindowOpen() {
        return marketHoursService.isMarketOpen()
                || globalMarketScheduleService.isAnyMarketCurrentlyOpen(java.time.Instant.now());
    }

    private void scheduleReconnect(String reason) {
        if (!streamingDesired || reconnectScheduled.getAndSet(true)) {
            return;
        }

        long configuredInitialDelay = reconnectInitialDelayMs > 0 ? reconnectInitialDelayMs : 5000L;
        long delay = Math.max(1000L, reconnectDelayMs > 0 ? reconnectDelayMs : configuredInitialDelay);
        log.warn("StreamerManager", "Scheduling stream reconnect in " + delay + " ms: " + reason);
        reconnectExecutor.schedule(() -> {
            reconnectScheduled.set(false);
            if (!streamingDesired || streamer.isConnected()) {
                return;
            }
            connectAndSubscribe();
            reconnectDelayMs = Math.min(Math.max(delay * 2, delay), reconnectMaxDelayMs);
        }, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Clean graceful shutdown hook triggered when Kubernetes sends SIGTERM to this pod.
     * 
     * Key responsibilities:
     * 1. Explicitly disconnect the Upstox WebSocket stream so the active TCP socket is closed.
     *    This releases the Upstox broker connection session immediately, allowing the newly
     *    deployed replacement pod to establish its WebSocket connection without "concurrent connection" conflicts.
     * 2. Prevent Kubernetes container rollout hangs where the terminating pod remains stuck in 'Terminating'
     *    status waiting for open background network sockets to time out.
     * 3. Cancel any pending reconnect schedules and terminate the reconnect executor thread pool cleanly.
     */
    @PreDestroy
    public void shutdown() {
        log.info("StreamerManager", "PreDestroy hook invoked: cleanly shutting down market data streamer.");
        streamingDesired = false;
        reconnectScheduled.set(false);
        connectionAttemptInFlight.set(false);
        streamReady.set(false);

        // Explicitly close the WebSocket connection to Upstox to release the network socket immediately
        if (streamer != null && streamer.isConnected()) {
            try {
                log.info("StreamerManager", "Disconnecting active Upstox WebSocket session before pod termination.");
                streamer.disconnect();
            } catch (Exception e) {
                log.warn("StreamerManager", "Error while disconnecting streamer during shutdown: " + e.getMessage());
            }
        }

        // Terminate background reconnect executor threads
        if (reconnectExecutor != null) {
            try {
                reconnectExecutor.shutdownNow();
            } catch (Exception e) {
                log.warn("StreamerManager", "Error shutting down reconnect executor: " + e.getMessage());
            }
        }
    }

    // --- StreamerListener Implementation ---

    @Override
    public void onOpen() {
        log.info("StreamerManager", "Connection Opened. Subscribing to tracked symbols.");
        connectionAttemptInFlight.set(false);
        streamReady.set(false);

        // A provider callback can arrive after stopStreaming() or after ingestion
        // has been disabled. Do not let a late callback resurrect the stream.
        if (!streamingDesired || !ingestionEnabled) {
            lastStreamActivityAt = 0;
            try {
                streamer.disconnect();
            } catch (Exception e) {
                log.warn("StreamerManager", "Failed to close a late connection callback", e);
            }
            return;
        }

        streamingDesired = true;
        reconnectDelayMs = Math.max(1000L, reconnectInitialDelayMs);
        lastStreamActivityAt = System.currentTimeMillis();
        subscribeToSymbols();
    }

    @Override
    public void onMessage(Object message) {
        // Service layer expects ONLY common DTOs (UpstoxFeedResponse)
        // Provider layer is responsible for converting proto → common DTO

        // 1. Publish to WebSocket (UI) - expects MarketUpdateV3 or Map<String, OHLCQuote>
        if (message instanceof MarketUpdateV3) {
            try {
                // Normalize provider-specific V3 feeds into the existing common
                // OHLC path. This keeps one enrichment, cache, publish, and
                // persistence flow for every WebSocket message type.
                Map<String, OHLCQuote> converted = v3FeedConverter.convert((MarketUpdateV3) message);
                if (!converted.isEmpty()) {
                    onMessage(converted);
                }
            } catch (Exception e) {
                log.error("StreamerManager", "Error broadcasting to UI", e);
            }
            return;
        } else if (message instanceof Map) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, OHLCQuote> ohlcMap = (Map<String, OHLCQuote>) message;
                if (!hasUsableLiveTick(ohlcMap)) {
                    log.warn("StreamerManager", "Ignoring a market update without a valid, timestamped quote");
                    return;
                }
                Map<String, MarketDataUpdate.QuoteChange> quotes = new HashMap<>();
                for (Map.Entry<String, OHLCQuote> entry : ohlcMap.entrySet()) {
                    String symbol = entry.getKey();
                    OHLCQuote quote = entry.getValue();
                    if (quote != null) {

                        // ---------------------------------------------------------
                        // GLOBAL INDEX ROUTING
                        // If the instrument key is a GLOBAL_* key (from Upstox Global
                        // Instruments), route it to the isolated global Redis cache.
                        // This keeps global tick data strictly separate from Indian
                        // index data (market:latest-price:*) to prevent any cross-pollution.
                        // ---------------------------------------------------------
                        if (symbol != null && symbol.startsWith("GLOBAL_")) {
                            handleGlobalIndexTick(symbol, quote);
                            // Do NOT add global ticks to the Indian UI broadcast — they
                            // have different refresh rates and display components.
                            continue;
                        }
                        // ---------------------------------------------------------
                        // END GLOBAL INDEX ROUTING — below is the existing Indian index path
                        // ---------------------------------------------------------

                        double lastPrice = quote.getLastPrice();
                        double open = quote.getOhlc() != null ? quote.getOhlc().getOpen() : 0.0;
                        double high = quote.getOhlc() != null ? quote.getOhlc().getHigh() : 0.0;
                        double low = quote.getOhlc() != null ? quote.getOhlc().getLow() : 0.0;
                        double close = quote.getOhlc() != null ? quote.getOhlc().getClose() : 0.0;
                        // 1. Prioritize the official adjusted previousClose from the Redis cache
                        Double prevClose = cacheService.getPreviousClose(symbol);
                        
                        // 2. Fall back to the streaming tick's previousClose if the cache is empty
                        if (prevClose == null || !Double.isFinite(prevClose) || prevClose <= 0.0) {
                            prevClose = quote.getPreviousClose();
                            // Some provider paths expose the session move but not the
                            // baseline. Derive the baseline from that verified move;
                            // never use today's OHLC close as yesterday's close.
                            if ((prevClose == null || !Double.isFinite(prevClose) || prevClose <= 0.0)
                                    && quote.getChange() != null
                                    && Double.isFinite(quote.getChange())) {
                                double derivedPreviousClose = lastPrice - quote.getChange();
                                if (Double.isFinite(derivedPreviousClose) && derivedPreviousClose > 0.0) {
                                    prevClose = derivedPreviousClose;
                                }
                            }
                            // Dynamically populate Redis so subsequent updates don't hit the fallback logic
                            if (prevClose != null && Double.isFinite(prevClose) && prevClose > 0.0) {
                                cacheService.setPreviousClose(symbol, prevClose);
                            }
                        }
                        if (prevClose != null && Double.isFinite(prevClose) && prevClose > 0.0) {
                            // The same common quote proceeds to Redis/Mongo and Kafka below.
                            // Set the resolved baseline before either path can serialize it.
                            quote.setPreviousClose(prevClose);
                        }

                        Double change = null;
                        Double changePercent = null;
                        if (prevClose != null && Double.isFinite(prevClose) && prevClose > 0.0) {
                            change = lastPrice - prevClose;
                            changePercent = (change / prevClose) * 100;
                            quote.setChange(change);
                            quote.setChangePercent(changePercent);
                        } else {
                            quote.setChange(null);
                            quote.setChangePercent(null);
                            log.warn("StreamerManager",
                                    "No session previousClose for live symbol {}; publishing price without daily change",
                                    symbol);
                        }

                        MarketDataUpdate.QuoteChange changeObj = MarketDataUpdate.QuoteChange.builder()
                                .lastPrice(lastPrice)
                                .open(open)
                                .high(high)
                                .low(low)
                                .close(close)
                                .previousClose(prevClose)
                                .change(change)
                                .changePercent(changePercent)
                                .build();
                        quotes.put(symbol, changeObj);
                    }
                }

                // Cache live tick prices (lastPrice + previousClose) to Redis Path 2
                // market:latest-price:<SYMBOL> so that page refreshes pick up the latest price.
                Map<String, OHLCQuote> cacheBatch = new HashMap<>(ohlcMap);
                cacheBackfillExecutor.execute(() -> {
                    try {
                        boolean persisted = cacheService.cacheLatestPrices(cacheBatch);
                        if (persisted) {
                            lastStreamActivityAt = System.currentTimeMillis();
                            streamReady.set(true);
                            connectionAttemptInFlight.set(false);
                        } else {
                            log.warn("StreamerManager", "Market update was valid but could not be persisted to the live-price cache");
                        }
                    } catch (Exception e) {
                        log.error("StreamerManager", "Error caching latest prices to Redis", e);
                    }
                });

                if (!quotes.isEmpty()) {
                    MarketDataUpdate data = MarketDataUpdate.builder()
                            .timestamp(System.currentTimeMillis())
                            .quotes(quotes)
                            .build();
                    publisher.publish(data);
                }
            } catch (Exception e) {
                log.error("StreamerManager", "Error broadcasting map updates to UI", e);
            }
        }

        // 2. Process for Kafka/Persistence - expects UpstoxFeedResponse
        try {
            Map<String, OHLCQuote> quotes = processor.processUpdate(message);
            if (quotes != null && !quotes.isEmpty()) {
                // log.debug("StreamerManager", "Processed " + quotes.size() + " quotes (Kafka).");
            }
        } catch (Exception e) {
            log.error("StreamerManager", "Error handling message for persistence", e);
        }
    }

    private boolean hasUsableLiveTick(Map<String, OHLCQuote> quotes) {
        if (quotes == null || quotes.isEmpty()) {
            return false;
        }

        Instant now = Instant.now();
        long maxAgeMillis = Math.max(TimeUnit.SECONDS.toMillis(30), noTickTimeoutMs);
        return quotes.values().stream()
                .filter(java.util.Objects::nonNull)
                .anyMatch(quote -> {
                    Instant timestamp = quote.getQuoteTimestamp();
                    if (quote.getLastPrice() <= 0.0 || timestamp == null) {
                        return false;
                    }
                    long ageMillis = now.toEpochMilli() - timestamp.toEpochMilli();
                    return ageMillis >= 0 && ageMillis <= maxAgeMillis;
                });
    }

    @Override
    public void onError(Throwable error) {
        connectionAttemptInFlight.set(false);
        streamReady.set(false);
        log.error("StreamerManager", "Streamer Error: " + error.getMessage(), error);
        if (streamingDesired) {
            scheduleReconnect("provider error");
        }
    }

    @Override
    public void onClose() {
        log.info("StreamerManager", "Connection Closed.");
        connectionAttemptInFlight.set(false);
        streamReady.set(false);
        lastStreamActivityAt = 0;
        if (streamingDesired) {
            scheduleReconnect("provider closed connection");
        }
    }

    private record VoteSnapshot(boolean indianOpen, boolean globalOpen, boolean readSuccessfully) {
        boolean anyOpen() {
            return indianOpen || globalOpen;
        }
    }

    /**
     * Handles an incoming WebSocket tick for a GLOBAL_* instrument key.
     *
     * <p>Writes the price and timestamp to their dedicated Redis keys:
     * <ul>
     *   <li>{@code market:global-latest:<INSTRUMENT_KEY>} — the latest price JSON</li>
     *   <li>{@code market:global-timestamp:<INSTRUMENT_KEY>} — epoch ms of last tick</li>
     * </ul>
     *
     * <p>These keys are read exclusively by {@code GlobalIndexService}.
     * They are intentionally NOT mixed with the Indian price cache
     * ({@code market:latest-price:*}) to keep the storage layers strictly separated.
     *
     * @param instrumentKey the full Upstox key (e.g., "GLOBAL_INDEX|DJI")
     * @param quote         the OHLC quote received from the WebSocket tick
     */
    private void handleGlobalIndexTick(String instrumentKey, OHLCQuote quote) {
        try {
            if (quote == null || quote.getQuoteTimestamp() == null || !Double.isFinite(quote.getLastPrice())
                    || quote.getLastPrice() <= 0 || quote.getQuoteTimestamp().isAfter(java.time.Instant.now().plusSeconds(120))) {
                log.warn("StreamerManager", "[GlobalTick] Rejected invalid or untimed quote for instrumentKey=" + instrumentKey);
                return;
            }
            String json = objectMapper.writeValueAsString(quote);
            String sourceTime = String.valueOf(quote.getQuoteTimestamp().toEpochMilli());

            Long written = redisTemplate.execute(WRITE_NEWER_GLOBAL_TICK,
                    java.util.List.of(GLOBAL_LATEST_PREFIX + instrumentKey, GLOBAL_TIMESTAMP_PREFIX + instrumentKey),
                    sourceTime, json);
            if (written == null || written == 0L) {
                log.debug("StreamerManager", "[GlobalTick] Ignored out-of-order tick for instrumentKey=" + instrumentKey);
                return;
            }

            // Write tick to InfluxDB for historical/intraday charts
            globalIndexCacheWriter.writeTick(instrumentKey, quote);

            log.debug("StreamerManager",
                    "[GlobalTick] Cached global tick for instrumentKey=" + instrumentKey +
                    ", lastPrice=" + quote.getLastPrice());
        } catch (Exception e) {
            log.error("StreamerManager",
                    "[GlobalTick] Failed to cache global tick for instrumentKey=" + instrumentKey, e);
        }
    }

    /**
     * Dynamically reads all index symbols configured in nseindices.yml on the classpath.
     * Parses standard YAML list elements formatted as `- "INDEX NAME"`.
     * If the file cannot be read, falls back to a list of major benchmark indices.
     */
    private List<String> loadIndicesFromYaml() {
        List<String> indices = new java.util.ArrayList<>();
        try {
            org.springframework.core.io.ClassPathResource resource = new org.springframework.core.io.ClassPathResource("nseindices.yml");
            if (resource.exists()) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        // Look for list items: - "NIFTY BANK" or - NIFTY BANK
                        if (line.startsWith("-")) {
                            String index = line.substring(1).trim()
                                    .replace("\"", "")
                                    .replace("'", "");
                            if (!index.isEmpty()) {
                                indices.add(index);
                            }
                        }
                    }
                }
                log.info("StreamerManager", "Loaded " + indices.size() + " indices dynamically from nseindices.yml for streaming.");
            } else {
                log.warn("StreamerManager", "nseindices.yml not found on classpath, using default fallback list.");
            }
        } catch (Exception e) {
            log.error("StreamerManager", "Failed to parse nseindices.yml dynamically for streaming: " + e.getMessage(), e);
        }

        // Fallback list of major indices if the file read fails
        if (indices.isEmpty()) {
            indices.addAll(java.util.Arrays.asList(
                "NIFTY 50", "NIFTY BANK", "NIFTY IT", "NIFTY NEXT 50", "NIFTY MIDCAP 50",
                "NIFTY INFRA", "NIFTY FMCG", "NIFTY METAL", "NIFTY REALTY", "NIFTY ENERGY"
            ));
        }
        return indices;
    }
}
