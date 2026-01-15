package com.am.marketdata.service.websocket.service;

import com.am.marketdata.common.MarketDataStreamer;
import com.am.marketdata.common.StreamerListener;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.log.AppLogger;
import com.am.marketdata.common.service.MarketDataPublisher;
import com.am.marketdata.service.MarketDataPersistenceService;
import com.am.marketdata.service.SymbolOrchestratorService;
import com.am.marketdata.service.websocket.processor.MarketDataProcessor;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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

    private Set<String> subscribedSymbols = new HashSet<>();
    private static final String DEFAULT_MODE = "full";

    @Autowired
    public StreamerManager(MarketDataStreamer streamer,
            MarketDataPersistenceService persistenceService,
            MarketDataProcessor processor,
            SymbolOrchestratorService symbolService,
            MarketDataPublisher publisher) {
        this.streamer = streamer;
        this.persistenceService = persistenceService;
        this.processor = processor;
        this.symbolService = symbolService;
        this.publisher = publisher;
    }

    // @PostConstruct
    public void init() {
        // Auto-start disabled to allow controlled start via API/Scheduler
        // login logic will trigger connection via scheduler or user action
    }

    public void refreshSubscriptions() {
        List<String> symbols = symbolService.findDistinctIsins();
        if (symbols != null) {
            this.subscribedSymbols.clear();
            this.subscribedSymbols.addAll(symbols);
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

        // 1. Publish to WebSocket (UI) - expects MarketUpdateV3 for now
        if (message instanceof MarketUpdateV3) {
            try {
                processUpdateForPublisher((MarketUpdateV3) message);
            } catch (Exception e) {
                log.error("StreamerManager", "Error broadcasting to UI", e);
            }
        }

        // 2. Process for Kafka/Persistence - expects UpstoxFeedResponse
        try {
            Map<String, OHLCQuote> quotes = processor.processUpdate(message);
            if (quotes != null && !quotes.isEmpty()) {
                // log.debug("StreamerManager", "Processed " + quotes.size() + " quotes
                // (Kafka).");
            }
        } catch (Exception e) {
            log.error("StreamerManager", "Error handling message for persistence", e);
        }
    }

    @Override
    public void onError(Throwable error) {
        log.error("StreamerManager", "Streamer Error: " + error.getMessage(), error);
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
}
