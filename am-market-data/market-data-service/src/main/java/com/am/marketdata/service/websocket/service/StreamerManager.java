package com.am.marketdata.service.websocket.service;

import com.am.marketdata.common.MarketDataStreamer;
import com.am.marketdata.common.StreamerListener;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.log.AppLogger;
import com.am.marketdata.service.MarketDataPersistenceService;
import com.am.marketdata.service.websocket.processor.MarketDataProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Service to manage the Market Data Streamer.
 * Handles lifecycle (Connect/Disconnect) and processes incoming data.
 * Scheduled to run from 8:00 AM to 4:00 PM.
 */
@Service
public class StreamerManager implements StreamerListener {

    private final AppLogger log = AppLogger.getLogger();

    private final MarketDataStreamer streamer;
    private final MarketDataPersistenceService persistenceService;
    private final MarketDataProcessor processor;

    private Set<String> subscribedSymbols = new HashSet<>();
    private static final String DEFAULT_MODE = "full";

    @Autowired
    public StreamerManager(MarketDataStreamer streamer,
            MarketDataPersistenceService persistenceService,
            MarketDataProcessor processor) {
        this.streamer = streamer;
        this.persistenceService = persistenceService;
        this.processor = processor;
    }

    @PostConstruct
    public void init() {
        streamer.setListener(this);
        // Initial set of symbols to subscribe (could be loaded from DB/Config)
        subscribedSymbols.add("NSE_INDEX|Nifty 50");
        subscribedSymbols.add("NSE_INDEX|Nifty Bank");
    }

    /**
     * Start the streamer (Scheduled at 8:00 AM)
     */
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Kolkata")
    public void startStreaming() {
        log.info("StreamerManager", "Starting Market Data Streamer (Scheduled)...");
        connectAndSubscribe();
    }

    /**
     * Stop the streamer (Scheduled at 4:00 PM)
     */
    @Scheduled(cron = "0 0 16 * * *", zone = "Asia/Kolkata")
    public void stopStreaming() {
        log.info("StreamerManager", "Stopping Market Data Streamer (Scheduled)...");
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
            streamer.connect();
            // Subscription happens in onOpen usually, or we can try immediately if connect
            // is blocking (it's not).
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
        log.info("StreamerManager", "Connection Opened. Subscribing to default symbols.");
        subscribeToSymbols();
    }

    @Override
    public void onMessage(Object message) {
        // Delegate to processor to parse message
        try {
            // log.debug("StreamerManager", "Received message: " + message);
            Map<String, OHLCQuote> quotes = processor.processUpdate(message);

            if (quotes != null && !quotes.isEmpty()) {
                log.debug("StreamerManager",
                        "Processed " + quotes.size() + " quotes (published to Kafka via processor).");
            }
        } catch (Exception e) {
            log.error("StreamerManager", "Error handling message", e);
        }
    }

    @Override
    public void onError(Throwable error) {
        log.error("StreamerManager", "Streamer Error: " + error.getMessage(), error);
        // Implement auto-reconnect logic if not handled by streamer itself or if
        // specific error
    }

    @Override
    public void onClose() {
        log.info("StreamerManager", "Connection Closed.");
    }
}
