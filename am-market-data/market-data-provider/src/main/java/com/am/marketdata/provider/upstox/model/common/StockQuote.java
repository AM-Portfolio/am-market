package com.am.marketdata.provider.upstox.model.common;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockQuote {
   
    private String symbol;
    private String isin;
    private String exchange;
    private String instrumentId;
    
    @JsonProperty("instrument_token")
    private String instrument_token;
    
    // Price Information
    @JsonProperty("last_price")
    private Double lastPrice;
    
    @JsonProperty("previous_close")
    private Double previousClose;
    
    @JsonProperty("net_change")
    private Double change;
    
    @JsonProperty("change_percent")
    private Double changePercent;
    
    private Double change5Min;
    private Double change10Min;
    private Double change15Min;
    private Double change1Hour;
    private Double change1Day;
    
    // OHLC
    @JsonProperty("ohlc")
    private Ohlc ohlc;
    
    private Double openPrice;
    private Double highPrice;
    private Double lowPrice;
    private Double closePrice;
    
    // Volume Information
    private Long volume;
    
    @JsonProperty("average_price")
    private Double averagePrice;
    
    // Market Depth
    private Double totalBuyQuantity;
    private Double totalSellQuantity;
    
    // Circuit Limits
    @JsonProperty("upper_circuit_limit")
    private Double upperCircuitLimit;
    
    @JsonProperty("lower_circuit_limit")
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Ohlc {
        private Double open;
        private Double high;
        private Double low;
        private Double close;
    }

    public Double getOpenPrice() {
        return ohlc != null ? ohlc.getOpen() : openPrice;
    }

    public Double getHighPrice() {
        return ohlc != null ? ohlc.getHigh() : highPrice;
    }

    public Double getLowPrice() {
        return ohlc != null ? ohlc.getLow() : lowPrice;
    }

    public Double getClosePrice() {
        return ohlc != null ? ohlc.getClose() : closePrice;
    }
} 