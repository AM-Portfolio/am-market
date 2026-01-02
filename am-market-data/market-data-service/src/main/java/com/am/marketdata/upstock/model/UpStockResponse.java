package com.am.marketdata.upstock.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.Map;

@Data
public class UpStockResponse {
    private String status;
    private Map<String, StockData> data;
}

@Data
class StockData {
    private OHLCData ohlc;
    private DepthData depth;
    private String timestamp;
    
    @JsonProperty("instrument_token")
    private String instrumentToken;
    private String symbol;
    
    @JsonProperty("last_price")
    private Double lastPrice;
    private Double volume;
    
    @JsonProperty("average_price")
    private Double averagePrice;
    private Double oi;
    
    @JsonProperty("net_change")
    private Double netChange;
    
    @JsonProperty("total_buy_quantity")
    private Double totalBuyQuantity;
    
    @JsonProperty("total_sell_quantity")
    private Double totalSellQuantity;
    
    @JsonProperty("lower_circuit_limit")
    private Double lowerCircuitLimit;
    
    @JsonProperty("upper_circuit_limit")
    private Double upperCircuitLimit;
    
    @JsonProperty("last_trade_time")
    private String lastTradeTime;
    
    @JsonProperty("oi_day_high")
    private Double oiDayHigh;
    
    @JsonProperty("oi_day_low")
    private Double oiDayLow;
} 