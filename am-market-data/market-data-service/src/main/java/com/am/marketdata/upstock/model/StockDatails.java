package com.am.marketdata.upstock.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class StockDatails {
   @JsonProperty("lastPrice")
        private Double last_price;

        @JsonProperty("instrumentToken")
        private String instrument_token;

        private String symbol;
        private Double volume;

        @JsonProperty("averagePrice")
        private Double average_price;

        private Double oi;

        @JsonProperty("net_change")
        private Double netChange;

        @JsonProperty("totalBuyQuantity")
        private String total_buy_quantity;

        @JsonProperty("totalSellQuantity")
        private String total_sell_quantity;

        @JsonProperty("lowerCircuitLimit")
        private String lower_circuit_limit;

        @JsonProperty("upperCircuitLimit")
        private String upper_circuit_limit;

        @JsonProperty("lastTradeTime")
        private String last_trade_time;

        @JsonAlias("oiDayHigh")
        private Double oi_day_high;

        @JsonProperty("oiDayLow")
        private Double oi_day_low;

        private String timestamp;
        private OHLCData ohlc;
        private DepthData depth;
}