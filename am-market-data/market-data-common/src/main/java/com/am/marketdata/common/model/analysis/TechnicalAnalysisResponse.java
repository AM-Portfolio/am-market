package com.am.marketdata.common.model.analysis;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalAnalysisResponse {
    private String symbol;
    private double currentPrice;
    private Double sma20;
    private Double sma50;
    private Double sma200;
    private Double rsi14;
    private String signal; // BUY, SELL, NEUTRAL
}
