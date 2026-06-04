package com.am.marketdata.api.model;

import com.am.marketdata.common.model.TimeFrame;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Request model for OHLC data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OHLCRequest {
    private String symbols;

    @JsonProperty("isIndexSymbol")
    private boolean indexSymbol = false;

    @JsonProperty("timeFrame")
    private String timeFrame = TimeFrame.FIVE_MINUTE.getApiValue();

    @JsonProperty("refresh")
    private boolean forceRefresh = false;

    public boolean isIndexSymbol() {
        return indexSymbol;
    }

    public void setIndexSymbol(boolean indexSymbol) {
        this.indexSymbol = indexSymbol;
    }
}
