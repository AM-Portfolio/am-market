package com.am.marketdata.api.model;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class MarketDataUpdate {
    private Long timestamp;
    private Map<String, QuoteChange> quotes;

    @Data
    @Builder
    public static class QuoteChange {
        private Double lastPrice;
        private Double open;
        private Double high;
        private Double low;
        private Double close;
        private Double previousClose;
        private Double change;
        private Double changePercent;
    }
}
