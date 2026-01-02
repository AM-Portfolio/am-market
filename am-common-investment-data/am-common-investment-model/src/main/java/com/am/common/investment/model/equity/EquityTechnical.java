package com.am.common.investment.model.equity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EquityTechnical {
    private String symbol;
    private String isin;
    private Instant time;
    
    // Moving Averages
    private Double sma20;
    private Double sma50;
    private Double sma200;
    private Double ema20;
    
    // Momentum Indicators
    private Double rsi14;
    private Double macd;
    private Double macdSignal;
    private Double macdHistogram;
    
    // Volatility Indicators
    private Double bollingerUpper;
    private Double bollingerMiddle;
    private Double bollingerLower;
    private Double atr14;
    
    // Volume Indicators
    private Double obv;
    private Double vwap;
}
