package com.am.marketdata.provider.upstox;

import com.am.marketdata.common.MarketDataStreamer;
import com.am.marketdata.common.StreamerListener;
import com.am.marketdata.common.log.AppLogger;
import com.am.marketdata.provider.common.InstrumentContext;
import com.am.marketdata.provider.upstox.config.UpstoxConfig;
import com.am.marketdata.provider.upstox.converter.UpstoxV3FeedConverter;
import com.am.marketdata.provider.upstox.resolver.UpstoxSymbolResolver;
import com.upstox.ApiClient;
import com.upstox.Configuration;
import com.upstox.feeder.MarketDataStreamerV3;
import com.upstox.feeder.MarketUpdate;
import com.upstox.feeder.MarketUpdateV3;
import com.upstox.feeder.constants.Mode;
import com.upstox.feeder.listener.OnCloseListener;
import com.upstox.feeder.listener.OnErrorListener;
import com.upstox.feeder.listener.OnMarketUpdateV3Listener;
import com.upstox.feeder.listener.OnOpenListener;
import com.upstox.auth.OAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Upstox implementation of the MarketDataStreamer.
 * Wraps the official MarketDataStreamerV3 from Upstox SDK.
 */
@Component
public class UpstoxMarketDataStreamer implements MarketDataStreamer {

    private final AppLogger log = AppLogger.getLogger();
    private final UpstoxConfig upstoxConfig;
    private final StringRedisTemplate redisTemplate;
    private final UpstoxV3FeedConverter v3Converter;
    private final UpstoxSymbolResolver symbolResolver;

    private static final String REDIS_KEY_ACCESS_TOKEN = "market_data:upstox:access_token";

    private MarketDataStreamerV3 streamer;
    private StreamerListener listener;
    private boolean isConnected = false;

    @Autowired
    public UpstoxMarketDataStreamer(
            UpstoxConfig upstoxConfig,
            StringRedisTemplate redisTemplate,
            UpstoxV3FeedConverter v3Converter,
            UpstoxSymbolResolver symbolResolver) {
        this.upstoxConfig = upstoxConfig;
        this.redisTemplate = redisTemplate;
        this.v3Converter = v3Converter;
        this.symbolResolver = symbolResolver;
    }

    @Override
    public void connect() {
        try {
            log.info("UpstoxStreamer", "=== STARTING Upstox Market Data Streamer connection ===");

            // Get Access Token - Try Redis cache first, then config
            log.info("UpstoxStreamer", "Step 1: Fetching access token...");
            String accessToken = getAccessTokenFromCacheOrConfig();
            if (accessToken == null || accessToken.isEmpty()) {
                log.error("UpstoxStreamer", "Cannot connect: Access Token is missing in configuration.");
                if (listener != null)
                    listener.onError(new IllegalStateException("Missing Access Token"));
                return;
            }
            log.info("UpstoxStreamer",
                    "Step 1: Access token retrieved successfully (length: " + accessToken.length() + ")");

            // Configure ApiClient
            log.info("UpstoxStreamer", "Step 2: Configuring ApiClient...");
            ApiClient apiClient = Configuration.getDefaultApiClient();
            OAuth oAuth = (OAuth) apiClient.getAuthentication("OAUTH2");
            oAuth.setAccessToken(accessToken);
            log.info("UpstoxStreamer", "Step 2: ApiClient configured successfully");

            // Initialize Streamer
            log.info("UpstoxStreamer", "Step 3: Initializing MarketDataStreamerV3...");
            streamer = new MarketDataStreamerV3(apiClient);
            log.info("UpstoxStreamer", "Step 3: Streamer initialized successfully");

            // Set Listeners
            log.info("UpstoxStreamer", "Step 4: Setting up listeners...");
            streamer.setOnOpenListener(new OnOpenListener() {
                @Override
                public void onOpen() {
                    log.info("UpstoxStreamer", "✅ *** CONNECTION ESTABLISHED *** ✅");
                    isConnected = true;
                    if (listener != null) {
                        log.info("UpstoxStreamer", "Notifying listener of connection open");
                        listener.onOpen();
                    } else {
                        log.warn("UpstoxStreamer", "No listener set - onOpen notification skipped");
                    }
                }
            });

            streamer.setOnMarketUpdateListener(new OnMarketUpdateV3Listener() {
                @Override
                public void onUpdate(MarketUpdateV3 marketUpdate) {
                    log.debug("UpstoxStreamer", "📊 Received market update");
                    if (listener != null) {
                        // Convert proto to common DTO (Map<String, OHLCQuote>) before passing to
                        // listener
                        // Service layer should ONLY receive org-level common models
                        Map<String, com.am.marketdata.common.model.OHLCQuote> commonQuotes = v3Converter
                                .convert(marketUpdate);
                        if (commonQuotes != null && !commonQuotes.isEmpty()) {
                            log.debug("UpstoxStreamer",
                                    "Converted " + commonQuotes.size() + " quotes, sending to listener");
                            listener.onMessage(commonQuotes);
                        } else {
                            log.debug("UpstoxStreamer",
                                    "Converter returned empty/null quotes (markets closed or LTP=0)");
                        }
                    } else {
                        log.warn("UpstoxStreamer", "No listener set - market update dropped");
                    }
                }
            });

            streamer.setOnErrorListener(new OnErrorListener() {
                @Override
                public void onError(Throwable error) {
                    log.error("UpstoxStreamer", "❌ ERROR in streamer: " + error.getMessage(), error);
                    if (listener != null)
                        listener.onError(error);
                }
            });

            streamer.setOnCloseListener(new OnCloseListener() {
                @Override
                public void onClose(int code, String reason) {
                    log.info("UpstoxStreamer", "Connection Closed: " + code + " | Reason: " + reason);
                    isConnected = false;
                    if (listener != null)
                        listener.onClose();
                }
            });
            log.info("UpstoxStreamer", "Step 4: All listeners set successfully");

            // Connect
            log.info("UpstoxStreamer", "Step 5: Calling streamer.connect()...");
            streamer.connect();
            log.info("UpstoxStreamer", "Step 5: streamer.connect() called - waiting for onOpen callback...");

        } catch (Exception e) {
            log.error("UpstoxStreamer", "❌ FATAL: Failed to connect: " + e.getMessage(), e);
            isConnected = false;
            if (listener != null)
                listener.onError(e);
        }
    }

    @Override
    public void disconnect() {
        if (streamer != null) {
            log.info("UpstoxStreamer", "Disconnecting...");
            streamer.disconnect();
            isConnected = false;
        }
    }

    @Override
    public void subscribe(Set<String> rawSymbols, String mode) {
        if (streamer == null) {
            log.warn("UpstoxStreamer", "Streamer not initialized. Cannot subscribe.");
            return;
        }

        if (rawSymbols == null || rawSymbols.isEmpty()) {
            log.warn("UpstoxStreamer", "No symbols provided for subscription");
            return;
        }

        log.info("UpstoxStreamer", "Subscribing to " + rawSymbols.size() + " raw symbols");

        // NEW: Resolve raw symbols to Upstox instrument keys using resolver
        InstrumentContext context = symbolResolver.resolveContext(new java.util.ArrayList<>(rawSymbols));

        Set<String> instrumentKeys = new HashSet<>(context.getInstrumentKeys());

        if (instrumentKeys.isEmpty()) {
            log.warn("UpstoxStreamer", "Symbol resolution returned no instrument keys for symbols: " + rawSymbols);
            return;
        }

        log.info("UpstoxStreamer",
                String.format("Resolved %d symbols to %d instrument keys", rawSymbols.size(), instrumentKeys.size()));

        Mode sdkMode = parseMode(mode);

        log.info("UpstoxStreamer", "Subscribing to instrument keys: " + instrumentKeys);
        streamer.subscribe(instrumentKeys, sdkMode);
        log.info("UpstoxStreamer", "Subscription successful for " + instrumentKeys.size() + " instruments");
    }

    @Override
    public void unsubscribe(Set<String> instrumentKeys) {
        if (streamer == null)
            return;
        log.info("UpstoxStreamer", "Unsubscribing from " + instrumentKeys.size() + " keys.");
        streamer.unsubscribe(new HashSet<>(instrumentKeys));
    }

    @Override
    public void changeMode(Set<String> instrumentKeys, String mode) {
        if (streamer == null)
            return;
        Mode sdkMode = parseMode(mode);
        streamer.changeMode(new HashSet<>(instrumentKeys), sdkMode);
    }

    @Override
    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public void setListener(StreamerListener listener) {
        this.listener = listener;
    }

    private Mode parseMode(String mode) {
        if (mode == null)
            return Mode.FULL;
        switch (mode.toLowerCase()) {
            case "ltpc":
                return Mode.LTPC;
            case "full":
                return Mode.FULL;
            // Add other modes if supported by SDK enum but mostly likely just these two for
            // now
            default:
                return Mode.FULL;
        }
    }

    private String getAccessTokenFromCacheOrConfig() {
        try {
            // Try Redis cache first
            String cachedToken = redisTemplate.opsForValue().get(REDIS_KEY_ACCESS_TOKEN);
            if (cachedToken != null && !cachedToken.isEmpty()) {
                log.info("UpstoxStreamer", "Using cached Access Token from Redis");
                return cachedToken;
            }
        } catch (Exception e) {
            log.warn("UpstoxStreamer", "Failed to load token from Redis: " + e.getMessage());
        }

        // Fallback to configuration
        String configToken = upstoxConfig.getAccessToken();
        if (configToken != null && !configToken.isEmpty()) {
            log.info("UpstoxStreamer", "Using Access Token from configuration");
            return configToken;
        }

        return null;
    }
}
