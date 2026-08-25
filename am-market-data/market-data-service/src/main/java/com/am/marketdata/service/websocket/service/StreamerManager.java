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
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.am.marketdata.common.model.MarketDataUpdate;
import com.upstox.feeder.MarketUpdateV3;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    @Autowired
    public StreamerManager(MarketDataStreamer streamer,
            MarketDataPersistenceService persistenceService,
            MarketDataProcessor processor,
            SymbolOrchestratorService symbolService,
            MarketDataPublisher publisher,
            MarketDataCacheService cacheService,
            StringRedisTemplate redisTemplate,
            com.am.marketdata.service.global.GlobalIndexCacheWriter globalIndexCacheWriter) {
        this.streamer = streamer;
        this.persistenceService = persistenceService;
        this.processor = processor;
        this.symbolService = symbolService;
        this.publisher = publisher;
        this.cacheService = cacheService;
        this.redisTemplate = redisTemplate;
        this.globalIndexCacheWriter = globalIndexCacheWriter;
    }

    // @PostConstruct
    public void init() {
        // Auto-start disabled to allow controlled start via API/Scheduler
        // login logic will trigger connection via scheduler or user action
    }

    public void refreshSubscriptions() {
        Set<String> symbols = symbolService.findDistinctSymbols();
        if (symbols == null) {
            symbols = new HashSet<>();
        }
        
        // Load and merge index symbols from the configuration file so we stream index ticks too
        symbols.addAll(loadIndicesFromYaml());

        if (!symbols.isEmpty()) {
            this.subscribedSymbols.clear();
            this.subscribedSymbols.addAll(symbols);

            log.info("StreamerManager",
                    "Refreshed subscriptions with " + subscribedSymbols.size() + " instrument keys (including indices)");

            // Actually subscribe if connected
            if (streamer.isConnected()) {
                log.info("StreamerManager",
                        "Streamer connected - subscribing to " + subscribedSymbols.size() + " symbols");
                streamer.subscribe(subscribedSymbols, DEFAULT_MODE);
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
            log.info("StreamerManager", "Streamer not connected. Connecting...");
            streamer.setListener(this);
            streamer.connect();
            // Subscription depends on onOpen if connect is async.
            // But assume we track symbols and subscribe in onOpen too.
        } else {
            streamer.subscribe(symbols, DEFAULT_MODE);
        }
    }

    /**
     * Start the streamer
     */
    public void startStreaming() {
        log.info("StreamerManager", "Starting Market Data Streamer (Manual/External Trigger)...");
        connectAndSubscribe();
    }

    /**
     * Stop the streamer
     */
    public void stopStreaming() {
        log.info("StreamerManager", "Stopping Market Data Streamer (Manual/External Trigger)...");
        if (streamer.isConnected()) {
            streamer.disconnect();
        }
    }

    // Exposed method for manual control
    public void manualStart() {
        log.info("StreamerManager", "Manual start triggered.");
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
        return streamer != null && streamer.isConnected();
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
                    double change = lastPrice - prevClose;
                    double changePercent = prevClose > 0 ? (change / prevClose) * 100 : 0.0;

                    quotes.put(symbol, MarketDataUpdate.QuoteChange.builder()
                            .lastPrice(lastPrice)
                            .open(open)
                            .high(high)
                            .low(low)
                            .close(close)
                            .previousClose(prevClose)
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

            // Also process for Kafka / persistence
            processor.processUpdate(ohlcData);
        } catch (Exception e) {
            log.error("StreamerManager", "Error processing fallback data for persistence/Kafka", e);
        }

        log.info("StreamerManager", "Fallback data processed and broadcast successfully");
    }

    private void connectAndSubscribe() {
        if (!streamer.isConnected()) {
            streamer.setListener(this);
            streamer.connect();
        } else {
            log.info("StreamerManager", "Already connected. Ensuring subscriptions.");
            subscribeToSymbols();
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
        boolean indianVote = "true".equalsIgnoreCase(
                redisTemplate.opsForValue().get(INDIAN_VOTE_KEY));
        boolean globalVote = "true".equalsIgnoreCase(
                redisTemplate.opsForValue().get(GLOBAL_VOTE_KEY));

        boolean shouldConnect = indianVote || globalVote;

        log.info("StreamerManager",
                String.format("[Reconciler] vote:indian=%s, vote:global=%s → shouldConnect=%s",
                        indianVote, globalVote, shouldConnect));

        if (shouldConnect) {
            if (!streamer.isConnected()) {
                log.info("StreamerManager", "[Reconciler] Connecting WebSocket (market open).");
                connectAndSubscribe();
            }
        } else {
            if (streamer.isConnected()) {
                log.info("StreamerManager",
                        "[Reconciler] All markets closed. Disconnecting WebSocket.");
                streamer.disconnect();
            }
        }
    }

    // --- StreamerListener Implementation ---

    @Override
    public void onOpen() {
        log.info("StreamerManager", "Connection Opened. Subscribing to tracked symbols.");
        subscribeToSymbols();
    }

    @Override
    public void onMessage(Object message) {
        // Service layer expects ONLY common DTOs (UpstoxFeedResponse)
        // Provider layer is responsible for converting proto → common DTO

        // 1. Publish to WebSocket (UI) - expects MarketUpdateV3 or Map<String, OHLCQuote>
        if (message instanceof MarketUpdateV3) {
            try {
                processUpdateForPublisher((MarketUpdateV3) message);
            } catch (Exception e) {
                log.error("StreamerManager", "Error broadcasting to UI", e);
            }
        } else if (message instanceof Map) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, OHLCQuote> ohlcMap = (Map<String, OHLCQuote>) message;
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
                        if (prevClose == null || prevClose == 0.0) {
                            prevClose = quote.getPreviousClose();
                            // Dynamically populate Redis so subsequent updates don't hit the fallback logic
                            if (prevClose != null && prevClose != 0.0) {
                                cacheService.setPreviousClose(symbol, prevClose);
                            }
                        } else {
                            // Sync the correct close value back into the quote object
                            quote.setPreviousClose(prevClose);
                        }

                        double change = lastPrice - prevClose;
                        double changePercent = prevClose > 0 ? (change / prevClose) * 100 : 0.0;

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
                try {
                    cacheService.cacheLatestPrices(ohlcMap);
                } catch (Exception e) {
                    log.error("StreamerManager", "Error caching latest prices to Redis", e);
                }

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

    @Override
    public void onError(Throwable error) {
        log.error("StreamerManager", "Streamer Error: " + error.getMessage(), error);
        // Introduce a cooling period so it doesn't spin in a rapid loop
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void onClose() {
        log.info("StreamerManager", "Connection Closed.");
    }

    private void processUpdateForPublisher(MarketUpdateV3 update) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            // Use ObjectMapper to convert to Map to avoid direct getter dependency
            java.util.Map<String, Object> map = mapper.convertValue(update, java.util.Map.class);

            String symbol = (String) map.getOrDefault("instrument_key", map.get("instrumentKey"));
            Object ltpObj = map.getOrDefault("ltp", map.get("lastPrice"));
            Double ltp = null;
            if (ltpObj instanceof Number) {
                ltp = ((Number) ltpObj).doubleValue();
            }

            if (symbol != null && ltp != null) {
                Map<String, MarketDataUpdate.QuoteChange> quotes = new HashMap<>();
                MarketDataUpdate.QuoteChange set = MarketDataUpdate.QuoteChange.builder()
                        .lastPrice(ltp)
                        .build();
                quotes.put(symbol, set);

                MarketDataUpdate data = MarketDataUpdate.builder()
                        .timestamp(System.currentTimeMillis())
                        .quotes(quotes)
                        .build();

                publisher.publish(data);
            }
        } catch (Exception e) {
            // log.warn("StreamerManager", "Error processing update for publisher", e);
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
            // Serialize the OHLCQuote to JSON for Redis storage
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(quote);

            // Write price to the global-specific Redis key (NOT the Indian market:latest-price key)
            redisTemplate.opsForValue().set(GLOBAL_LATEST_PREFIX + instrumentKey, json);

            // Write timestamp for the 30-min circuit breaker check
            redisTemplate.opsForValue().set(
                    GLOBAL_TIMESTAMP_PREFIX + instrumentKey,
                    String.valueOf(System.currentTimeMillis()));

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
