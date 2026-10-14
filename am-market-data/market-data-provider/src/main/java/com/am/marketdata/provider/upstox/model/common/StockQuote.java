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

    // Open interest is meaningful for derivatives; Upstox may return zero for cash equities.
    @JsonProperty("oi")
    private Long oi;

    @JsonProperty("oi_day_high")
    private Long oiDayHigh;

    @JsonProperty("oi_day_low")
    private Long oiDayLow;
    
    @JsonProperty("average_price")
    private Double averagePrice;
    
    // Market Depth
    @JsonProperty("total_buy_quantity")
    private Double totalBuyQuantity;
    @JsonProperty("total_sell_quantity")
    private Double totalSellQuantity;
    
    // Circuit Limits
    @JsonProperty("upper_circuit_limit")
    private Double upperCircuitLimit;
    
    @JsonProperty("lower_circuit_limit")
    private Double lowerCircuitLimit;
    
    // Timestamps
    @JsonProperty("timestamp")
    private ZonedDateTime lastUpdateTime;
    @JsonProperty("last_trade_time")
    private String lastTradeTime;
    
    // Market Depth Details
    @JsonProperty("depth")
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

    /**
     * Upstox V2 Quote API (GET /v2/market-quote/quotes) does not provide a direct
     * 'previous_close' field; it returns 'last_price' and 'net_change' (mapped to change).
     * If previousClose was not explicitly set or deserialized from upstream, dynamically
     * derive it from the same trading session snapshot: previousClose = lastPrice - change.
     * This avoids falling back to stale historical database records and ensures downstream
     * price enrichers (e.g., StockDataEnricher, Movers, Heatmap) have a valid baseline.
     */
    public Double getPreviousClose() {
        if (lastPrice != null && lastPrice > 0.0 && change != null) {
            double pc = lastPrice - change;
            if (Double.isFinite(pc) && pc > 0.0) {
                // Upstox net_change and last_price belong to the same quote snapshot.
                return Math.round(pc * 100.0) / 100.0;
            }
        }
        if (previousClose != null && Double.isFinite(previousClose) && previousClose > 0.0) {
            return previousClose;
        }
        return previousClose != null ? previousClose : 0.0;
    }

    /**
     * Returns net price change for the current session. If not explicitly provided,
     * derives it from lastPrice and the validated previousClose baseline.
     */
    public Double getChange() {
        if (change != null) {
            return change;
        }
        Double pc = getPreviousClose();
        if (pc != null && pc > 0.0 && lastPrice != null) {
            return Math.round((lastPrice - pc) * 100.0) / 100.0;
        }
        return null;
    }

    /**
     * Returns percentage change for the current session. If not explicitly provided,
     * derives it from change and the validated previousClose baseline: (change / previousClose) * 100.
     */
    public Double getChangePercent() {
        if (changePercent != null) {
            return changePercent;
        }
        Double pc = getPreviousClose();
        Double chg = getChange();
        if (pc != null && pc > 0.0 && chg != null) {
            return Math.round((chg / pc) * 10000.0) / 100.0;
        }
        return null;
    }
}
