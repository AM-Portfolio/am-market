package com.am.marketdata.analysis.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TechnicalAnalysisResponse {
    private String symbol;
    private double currentPrice;
    private Double sma20;
    private Double sma50;
    private Double sma200;
    private Double rsi14;
    private String signal; // BUY, SELL, NEUTRAL
}
