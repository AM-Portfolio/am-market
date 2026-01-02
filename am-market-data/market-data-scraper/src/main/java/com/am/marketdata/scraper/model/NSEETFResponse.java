package com.am.marketdata.scraper.model;

import lombok.Data;

import java.util.List;

@Data
public class NSEETFResponse {
    private MarketStatus marketStatus;
    private List<ETFData> data;

    @Data
    public static class MarketStatus {
        private String marketStatus;
        private String tradeDate;
        private String index;
    }

    @Data
    public static class ETFData {
        private String symbol;
        private String isinCode;
        private String lastPrice;
        private String change;
        private String pChange;
        private String previousClose;
        private String numberOfShares;
        private String totalTurnover;
    }
}
