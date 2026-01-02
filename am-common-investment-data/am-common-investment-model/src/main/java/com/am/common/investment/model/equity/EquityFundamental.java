package com.am.common.investment.model.equity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EquityFundamental {
    private String symbol;
    private String isin;
    private Instant time;
    private ValuationMetrics valuationMetrics;
    private FinancialRatios financialRatios;
    private GrowthMetrics growthMetrics;
    private DividendMetrics dividendMetrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValuationMetrics {
        private Double pe;
        private Double pb;
        private Double ps;
        private Double pcf;
        private BigDecimal marketCap;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialRatios {
        private Double currentRatio;
        private Double quickRatio;
        private Double debtToEquity;
        private Double roa;
        private Double roe;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrowthMetrics {
        private Double revenueGrowth;
        private Double profitGrowth;
        private Double epsgrowth;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DividendMetrics {
        private Double dividendYield;
        private Double payoutRatio;
    }
}
