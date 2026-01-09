package com.am.marketdata.common.model.events;

import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Event fired when OHLC data is retrieved and needs to be ingested
 * asynchronously
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OHLCDataIngestionEvent {
    /**
     * Map of Symbol -> OHLCQuote
     */
    private Map<String, OHLCQuote> data;

    /**
     * The timeframe of the data
     */
    private TimeFrame timeFrame;

    /**
     * The source provider name
     */
    private String provider;

    /**
     * Timestamp of the event creation
     */
    private long timestamp;
}
