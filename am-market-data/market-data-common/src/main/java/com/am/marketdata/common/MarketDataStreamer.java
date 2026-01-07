package com.am.marketdata.common;

import java.util.List;
import java.util.Set;

/**
 * Interface defining the contract for a Market Data Streamer.
 * This allows for different implementations (Upstox, Zerodha, etc.).
 */
public interface MarketDataStreamer {

    /**
     * connect to the WebSocket.
     */
    void connect();

    /**
     * disconnect from the WebSocket.
     */
    void disconnect();

    /**
     * Subscribe to a list of symbols/instrument keys.
     * 
     * @param instrumentKeys List of keys to subscribe to.
     * @param mode           Data mode (e.g., "full", "ltpc").
     */
    void subscribe(Set<String> instrumentKeys, String mode);

    /**
     * Unsubscribe from a list of symbols/instrument keys.
     * 
     * @param instrumentKeys List of keys to unsubscribe from.
     */
    void unsubscribe(Set<String> instrumentKeys);

    /**
     * Change the subscription mode for existing keys.
     * 
     * @param instrumentKeys List of keys to change mode for.
     * @param mode           New data mode.
     */
    void changeMode(Set<String> instrumentKeys, String mode);

    /**
     * Check if connected.
     * 
     * @return true if connected, false otherwise.
     */
    boolean isConnected();

    /**
     * Set the listener for streamer events.
     * 
     * @param listener The listener to handle callbacks.
     */
    void setListener(StreamerListener listener);
}
