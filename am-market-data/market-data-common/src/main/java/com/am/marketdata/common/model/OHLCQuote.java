package com.am.marketdata.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OHLCQuote {

    private double lastPrice;

    private OHLC ohlc;

    private double previousClose;

    // Optional real-time details. Older providers and Redis entries may omit them.
    private Double change;
    private Double changePercent;
    private Long volume;
    private Double averagePrice;
    private Double totalBuyQuantity;
    private Double totalSellQuantity;
    private Double lowerCircuitLimit;
    private Double upperCircuitLimit;
    private Long oi;
    private Long oiDayHigh;
    private Long oiDayLow;
    private QuoteDepth marketDepth;
    private Instant quoteTimestamp;
    private LocalDate quoteTradeDate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OHLC {
        private double high;
        private double low;
        private double close;
        private double open;

    }

}
