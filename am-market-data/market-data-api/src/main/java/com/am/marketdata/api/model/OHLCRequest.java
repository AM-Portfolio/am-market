package com.am.marketdata.api.model;

import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.api.deserializer.StringOrArrayDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
    @JsonDeserialize(using = StringOrArrayDeserializer.class)
    private String symbols;

    @JsonProperty("isIndexSymbol")
    private boolean indexSymbol = false;

    @JsonProperty("timeFrame")
    private String timeFrame = TimeFrame.FIVE_MINUTE.getApiValue();

    @JsonProperty("refresh")
    private boolean forceRefresh = false;

    /**
     * Target exchange for symbols (e.g. "NSE", "BSE", "NSE_FO").
     * Defaults to "NSE" when omitted.
     */
    @JsonProperty("exchange")
    private String exchange = "NSE";

    public boolean isIndexSymbol() {
        return indexSymbol;
    }

    public void setIndexSymbol(boolean indexSymbol) {
        this.indexSymbol = indexSymbol;
    }
}
