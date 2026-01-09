package com.am.marketdata.common.model.events;

import com.am.common.investment.model.historical.HistoricalData;
import com.am.marketdata.common.model.TimeFrame;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Event fired when Historical data is retrieved and needs to be ingested
 * asynchronously
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalDataIngestionEvent {
    /**
     * Map of Symbol -> HistoricalData
     */
    private Map<String, HistoricalData> data;

    /**
     * The timeframe/interval of the data
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
