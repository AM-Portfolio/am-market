package com.am.marketdata.common.model;

import lombok.Data;

@Data
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class NSEIndex {
    private String key;
    private String index;
    private String indexSymbol;
    private double last;
    private double variation;
    private double percentChange;
    private double open;
    private double high;
    private double low;
    private double previousClose;
    private double yearHigh;
    private double yearLow;
    private double indicativeClose;
    private String pe;
    private String pb;
    private String dy;
    private String declines;
    private String advances;
    private String unchanged;

    @com.fasterxml.jackson.annotation.JsonProperty("perChange365d")
    private double percentChange365d;

    private String date365dAgo;
    private String chart365dPath;
    private String date30dAgo;

    @com.fasterxml.jackson.annotation.JsonProperty("perChange30d")
    private double percentChange30d;

    private String chart30dPath;
    private String chartTodayPath;
    private String previousDay;
    private String oneWeekAgo;
    private String oneMonthAgo;
    private String oneYearAgo;
}
