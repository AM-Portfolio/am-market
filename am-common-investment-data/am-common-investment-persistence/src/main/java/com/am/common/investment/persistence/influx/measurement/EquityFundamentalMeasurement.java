package com.am.common.investment.persistence.influx.measurement;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.Data;

import java.time.Instant;
import java.math.BigDecimal;

@Data
@Measurement(name = "equity_fundamentals")
public class EquityFundamentalMeasurement {
    @Column(tag = true)
    private String symbol;
    
    @Column(tag = true)
    private String isin;
    
    @Column(timestamp = true)
    private Instant time;
    
    // Valuation Metrics
    @Column
    private Double pe;
    
    @Column
    private Double pb;
    
    @Column
    private Double ps;
    
    @Column
    private Double pcf;
    
    @Column
    private BigDecimal marketCap;
    
    // Financial Ratios
    @Column
    private Double currentRatio;
    
    @Column
    private Double quickRatio;
    
    @Column
    private Double debtToEquity;
    
    @Column
    private Double roa;
    
    @Column
    private Double roe;
    
    // Growth Metrics
    @Column
    private Double revenueGrowth;
    
    @Column
    private Double profitGrowth;
    
    @Column
    private Double epsgrowth;
    
    // Dividend Metrics
    @Column
    private Double dividendYield;
    
    @Column
    private Double payoutRatio;
}
