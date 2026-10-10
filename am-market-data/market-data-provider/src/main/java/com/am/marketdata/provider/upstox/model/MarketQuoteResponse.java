package com.am.marketdata.provider.upstox.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.am.marketdata.provider.upstox.model.common.StockQuote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketQuoteResponse {
    private String status;
    
    @JsonProperty("data")
    private Map<String, StockQuote> data;
}