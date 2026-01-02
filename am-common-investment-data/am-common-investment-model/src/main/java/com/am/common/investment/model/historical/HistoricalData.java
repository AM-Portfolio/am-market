package com.am.common.investment.model.historical;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Model class representing historical data for a financial instrument
 * with metadata about the query parameters used to retrieve it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HistoricalData {
    // Identifier information
    private String tradingSymbol;
    private String isin;
    
    // Query parameters
    private Instant fromDate;
    private Instant toDate;
    private String interval;
    
    // Result data - OHLCVT time series
    private List<OHLCVTPoint> dataPoints;
    
    // Metadata 
    private int dataPointCount;
    private String exchange;
    private String currency;
    private Instant retrievalTime;
}
