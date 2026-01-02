package com.am.marketdata.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.am.marketdata.common.util.NumberDeserializer;
import lombok.Data;

@Data
public class NseETF {
    private String symbol;
    private String assets;
    
    @JsonDeserialize(using = NumberDeserializer.class)
    private Double open;
    
    @JsonDeserialize(using = NumberDeserializer.class)
    private Double high;
    
    @JsonDeserialize(using = NumberDeserializer.class)
    private Double low;
    
    @JsonProperty("ltP")
    @JsonDeserialize(using = NumberDeserializer.class)
    private Double lastTradedPrice;
    
    @JsonProperty("chn")
    @JsonDeserialize(using = NumberDeserializer.class)
    private Double change;
    
    @JsonProperty("per")
    @JsonDeserialize(using = NumberDeserializer.class)
    private Double percentChange;
    
    @JsonProperty("qty")
    private Integer quantity;
    
    @JsonProperty("trdVal")
    @JsonDeserialize(using = NumberDeserializer.class)
    private Double tradedValue;
    
    @JsonDeserialize(using = NumberDeserializer.class)
    private Double nav;
    
    @JsonProperty("wkhi")
    @JsonDeserialize(using = NumberDeserializer.class)
    private Double weekHigh;
    
    @JsonProperty("wklo")
    @JsonDeserialize(using = NumberDeserializer.class)
    private Double weekLow;
    
    @JsonProperty("xDt")
    private String exDate;
    
    @JsonProperty("cAct")
    private String corporateAction;
    
    @JsonProperty("yPC")
    @JsonDeserialize(using = NumberDeserializer.class)
    private Double yearlyPercentageChange;
    
    @JsonProperty("mPC")
    @JsonDeserialize(using = NumberDeserializer.class)
    private Double monthlyPercentageChange;
    
    @JsonDeserialize(using = NumberDeserializer.class)
    private Double prevClose;
    
    @JsonDeserialize(using = NumberDeserializer.class)
    private Double stockIndClosePrice;
    
    @JsonDeserialize(using = NumberDeserializer.class)
    private Double nearWKH;
    
    @JsonDeserialize(using = NumberDeserializer.class)
    private Double nearWKL;
    
    private String chartTodayPath;
    
    @JsonDeserialize(using = NumberDeserializer.class)
    private Double perChange365d;
    
    private String date365dAgo;
    private String chart365dPath;
    private String date30dAgo;
    
    @JsonDeserialize(using = NumberDeserializer.class)
    private Double perChange30d;
    
    private String chart30dPath;
    private EtfMetadata meta;

    @Data
    public static class EtfMetadata {
        private String symbol;
        private String companyName;
        private String[] activeSeries;
        private String[] debtSeries;
        private Boolean isFNOSec;
        private Boolean isCASec;
        private Boolean isSLBSec;
        private Boolean isDebtSec;
        private Boolean isSuspended;
        private String[] tempSuspendedSeries;
        private Boolean isETFSec;
        private Boolean isDelisted;
        private String isin;
        private String listingDate;
        private Boolean isMunicipalBond;
        private Boolean isHybridSymbol;
        private QuotePreOpenStatus quotePreOpenStatus;
    }

    @Data
    public static class QuotePreOpenStatus {
        private String equityTime;
        private String preOpenTime;
        private Boolean quotePreOpenFlag;
    }
}
