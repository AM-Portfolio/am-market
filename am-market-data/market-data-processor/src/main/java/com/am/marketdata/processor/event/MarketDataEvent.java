package com.am.marketdata.processor.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic event model for market data events
 * Used as a common format for all market data events
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketDataEvent {
    
    /**
     * Event type (e.g., "INDICES", "ETF", "ERROR")
     */
    private String type;
    
    /**
     * Source of the event (e.g., "market-data-processor")
     */
    private String source;
    
    /**
     * Specific data type for more granular categorization
     */
    private String dataType;
    
    /**
     * Timestamp of the event (epoch millis)
     */
    private long timestamp;
    
    /**
     * Error message (if any)
     */
    private String error;
    
    /**
     * Actual data payload
     */
    private Object data;
}
