package com.am.marketdata.provider.upstox;

import com.am.marketdata.common.MarketDataStreamer;
import com.am.marketdata.common.StreamerListener;
import com.am.marketdata.common.log.AppLogger;
import com.am.marketdata.provider.upstox.config.UpstoxConfig;
import com.upstox.ApiClient;
import com.upstox.Configuration;
import com.upstox.feeder.MarketDataStreamerV3;
import com.upstox.feeder.MarketUpdateV3;
import com.upstox.feeder.constants.Mode;
import com.upstox.feeder.listener.OnCloseListener;
import com.upstox.feeder.listener.OnErrorListener;
import com.upstox.feeder.listener.OnMarketUpdateV3Listener;
import com.upstox.feeder.listener.OnOpenListener;
import com.upstox.auth.OAuth;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Upstox implementation of the MarketDataStreamer.
 * Wraps the official MarketDataStreamerV3 from Upstox SDK.
 */
@Component
public class UpstoxMarketDataStreamer implements MarketDataStreamer {

    private final AppLogger log = AppLogger.getLogger();
    private final UpstoxConfig upstoxConfig;

    private MarketDataStreamerV3 streamer;
    private StreamerListener listener;
    private boolean isConnected = false;

    public UpstoxMarketDataStreamer(UpstoxConfig upstoxConfig) {
        this.upstoxConfig = upstoxConfig;
    }

    @Override
    public void connect() {
        try {
            log.info("UpstoxStreamer", "Initializing Upstox Market Data Streamer connection...");

            // Get Access Token
            String accessToken = upstoxConfig.getAccessToken();
            if (accessToken == null || accessToken.isEmpty()) {
                log.error("UpstoxStreamer", "Cannot connect: Access Token is missing in configuration.");
                if (listener != null)
                    listener.onError(new IllegalStateException("Missing Access Token"));
                return;
            }

            // Configure ApiClient
            ApiClient apiClient = Configuration.getDefaultApiClient();
            OAuth oAuth = (OAuth) apiClient.getAuthentication("OAUTH2");
            oAuth.setAccessToken(accessToken);

            // Initialize Streamer
            streamer = new MarketDataStreamerV3(apiClient);

            // Set Listeners
            streamer.setOnOpenListener(new OnOpenListener() {
                @Override
                public void onOpen() {
                    log.info("UpstoxStreamer", "Connection Established");
                    isConnected = true;
                    if (listener != null)
                        listener.onOpen();
                }
            });

            streamer.setOnMarketUpdateListener(new OnMarketUpdateV3Listener() {
                @Override
                public void onUpdate(MarketUpdateV3 marketUpdate) {
                    if (listener != null)
                        listener.onMessage(marketUpdate);
                }
            });

            streamer.setOnErrorListener(new OnErrorListener() {
                @Override
                public void onError(Throwable error) {
                    log.error("UpstoxStreamer", "Error in streamer: " + error.getMessage(), error);
                    if (listener != null)
                        listener.onError(error);
                }
            });

            streamer.setOnCloseListener(new OnCloseListener() {
                @Override
                public void onClose(int code, String reason) {
                    log.info("UpstoxStreamer", "Connection Closed: " + code + " " + reason);
                    isConnected = false;
                    if (listener != null)
                        listener.onClose();
                }
            });

            // Connect
            streamer.connect();

        } catch (Exception e) {
            log.error("UpstoxStreamer", "Failed to connect: " + e.getMessage(), e);
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
    public void subscribe(Set<String> instrumentKeys, String mode) {
        if (streamer == null) {
            log.warn("UpstoxStreamer", "Streamer not initialized. Cannot subscribe.");
            return;
        }

        Mode sdkMode = parseMode(mode);
        log.info("UpstoxStreamer", "Subscribing to " + instrumentKeys.size() + " keys in " + sdkMode + " mode.");
        streamer.subscribe(new HashSet<>(instrumentKeys), sdkMode);
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
}
