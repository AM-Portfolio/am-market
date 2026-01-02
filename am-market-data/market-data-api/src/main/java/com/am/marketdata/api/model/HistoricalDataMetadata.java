package com.am.marketdata.api.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalDataMetadata {
    private String fromDate;
    private String toDate;
    private String interval;
    private String intervalEnum;
    private int totalSymbols;
    private int successfulSymbols;
    private int totalDataPoints;
    private int filteredDataPoints;
    private boolean filtered;
    private String filterType;
    private Integer filterFrequency;
    private long processingTimeMs;
    private String source;
}
