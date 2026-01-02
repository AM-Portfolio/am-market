package com.am.marketdata.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.am.marketdata.common.util.CustomDoubleDeserializer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NSEStockInsidicesData {
    private String name;
    private Advance advance;
    private String timestamp;
    private List<StockData> data;
    private IndexMetadata metadata;
    private MarketStatus marketStatus;
    private String date30dAgo;
    private String date365dAgo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Advance {
        private String declines;
        private String advances;
        private String unchanged;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StockData {
        private int priority;
        private String symbol;
        private String identifier;
        private String series;
        @JsonDeserialize(using = CustomDoubleDeserializer.class)
        private Double open;
        @JsonDeserialize(using = CustomDoubleDeserializer.class)
        private Double dayHigh;
        @JsonDeserialize(using = CustomDoubleDeserializer.class)
        private Double dayLow;
        @JsonDeserialize(using = CustomDoubleDeserializer.class)
        private Double lastPrice;
        @JsonDeserialize(using = CustomDoubleDeserializer.class)
        private Double previousClose;
        @JsonDeserialize(using = CustomDoubleDeserializer.class)
        private Double change;
        @JsonProperty("pChange")
        @JsonDeserialize(using = CustomDoubleDeserializer.class)
        private Double pChange;
        private long totalTradedVolume;
        @JsonDeserialize(using = CustomDoubleDeserializer.class)
        private Double stockIndClosePrice;
        @JsonDeserialize(using = CustomDoubleDeserializer.class)
        private Double totalTradedValue;
        @JsonDeserialize(using = CustomDoubleDeserializer.class)
        private Double yearHigh;
        @JsonDeserialize(using = CustomDoubleDeserializer.class)
        private Double ffmc;
        @JsonDeserialize(using = CustomDoubleDeserializer.class)
        private Double yearLow;
        @JsonDeserialize(using = CustomDoubleDeserializer.class)
        private Double nearWKH;
        @JsonDeserialize(using = CustomDoubleDeserializer.class)
        private Double nearWKL;
        private String perChange365d;
        private String date365dAgo;
        private String chart365dPath;
        private String date30dAgo;
        @JsonDeserialize(using = CustomDoubleDeserializer.class)
        private Double perChange30d;
        private String chart30dPath;
        private String chartTodayPath;
        private Metadata meta;
    }

    @Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metadata {
        private String symbol;
        private String companyName;
        private String industry;
        private List<String> activeSeries;
        private String isin;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IndexMetadata {
        private String indexName;
        private Double open;
        private Double high;
        private Double low;
        private Double previousClose;
        private Double last;
        private Double percChange;
        private Double change;
        private String timeVal;
        private Double yearHigh;
        private Double yearLow;
        private Double indicativeClose;
        private long totalTradedVolume;
        private Double totalTradedValue;
        @JsonProperty("ffmc_sum")
        private double ffmcSum;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
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