package com.am.marketdata.provider.upstox.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.am.marketdata.provider.upstox.model.common.StockQuote;

import lombok.Data;
import java.util.Map;

@Data
public class MarketQuoteResponse {
    private String status;
    
    @JsonProperty("data")
    private Map<String, StockQuote> data;
}