package com.am.marketdata.common.model;

import lombok.Data;
import java.util.List;

@Data
public class NseETFResponse {
    private List<NseETF> data;
    private Integer advances;
    private Integer declines;
    private Integer unchanged;
    private String navDate;
    private Double totalTradedValue;
    private Integer totalTradedVolume;
    private String timestamp;
    private MarketStatus marketStatus;

    @Data
    public static class MarketStatus {
        private String market;
        private String marketStatus;
        private String tradeDate;
        private String index;
        private Double last;
        private Double variation;
        private Double percentChange;
        private String marketStatusMessage;
    }
}
