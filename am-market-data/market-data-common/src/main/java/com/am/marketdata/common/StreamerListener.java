package com.am.marketdata.common;

/**
 * Interface for receiving callbacks from the MarketDataStreamer.
 */
public interface StreamerListener {

    /**
     * Called when the connection is successfully established.
     */
    void onOpen();

    /**
     * Called when a market update message is received.
     * 
     * @param message The raw message object (can be cast based on implementation)
     *                or a common wrapper.
     *                For now, we use Object to keep it generic, effectively handing
     *                off the SDK's response object.
     */
    void onMessage(Object message);

    /**
     * Called when an error occurs.
     * 
     * @param error The error/exception.
     */
    void onError(Throwable error);

    /**
     * Called when the connection is closed.
     */
    void onClose();
}
