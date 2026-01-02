package com.am.common.investment.model.equity.metrics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrowthMetrics {
    private Double revenueGrowthPer;
    private Double netProfitGrowth;
    private Double netProfitMarginGrowth;
    private Double netSalesGrowth;
    private Double ebitdaGrowth;
    private Double ebitGrowth;
    private Double patGrowth;
    private Double patMarginGrowth;
}
