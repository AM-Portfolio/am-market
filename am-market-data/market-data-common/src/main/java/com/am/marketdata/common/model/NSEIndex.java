package com.am.marketdata.common.model;

import lombok.Data;

@Data
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
    private double percentChange365d;
    private String date365dAgo;
    private String chart365dPath;
    private String date30dAgo;
    private double percentChange30d;
    private String chart30dPath;
    private String chartTodayPath;
    private double previousDay;
    private double oneWeekAgo;
    private double oneMonthAgo;
    private double oneYearAgo;
}
