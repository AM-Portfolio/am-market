package com.am.marketdata.api.model;

import com.am.marketdata.common.model.TimeFrame;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * Request model for quotes endpoint with timeframe support
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuotesRequest {
    private String symbols;
    private String timeFrame = TimeFrame.FIVE_MINUTE.getApiValue(); // Default to 5-minute timeframe
    private boolean forceRefresh = false;
    private boolean indexSymbol = false;
}
