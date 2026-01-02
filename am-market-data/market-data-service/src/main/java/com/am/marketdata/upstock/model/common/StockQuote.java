package com.am.marketdata.upstock.model.common;

import lombok.Data;
import lombok.Builder;
import java.time.ZonedDateTime;

@Data
@Builder
public class StockQuote {
   
    private String symbol;
    private String isin;
    private String exchange;
    private String instrumentId;
    private String instrument_token;
    
    // Price Information
    private Double lastPrice;
    private Double previousClose;
    private Double change;
    private Double changePercent;
    private Double change5Min;
    private Double change10Min;
    private Double change15Min;
    private Double change1Hour;
    private Double change1Day;
    
    // OHLC
    private Double openPrice;
    private Double highPrice;
    private Double lowPrice;
    private Double closePrice;
    
    // Volume Information
    private Long volume;
    private Double averagePrice;
    
    // Market Depth
    private Double totalBuyQuantity;
    private Double totalSellQuantity;
    
    // Circuit Limits
    private Double upperCircuitLimit;
    private Double lowerCircuitLimit;
    
    // Timestamps
    private ZonedDateTime lastUpdateTime;
    private ZonedDateTime lastTradeTime;
    
    // Market Depth Details
    private MarketDepth marketDepth;
    
    // Closing Prices for Different Timeframes
    private Double closePrice5Min;
    private Double closePrice10Min;
    private Double closePrice15Min;
    private Double closePrice1Hour;
    private Double closePrice1Day;
} 