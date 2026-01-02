package com.am.common.investment.model.equity.financial.resultstatement;

import com.am.common.investment.model.equity.metrics.CostMetrics;
import com.am.common.investment.model.equity.metrics.EpsMetrics;
import com.am.common.investment.model.equity.metrics.GrowthMetrics;
import com.am.common.investment.model.equity.metrics.ProfitMetrics;
import com.am.common.investment.model.equity.metrics.TaxMetrics;
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
public class FinancialResult{
    private String yearEnd;
    private String quarter;
    
    // Revenue Metrics
    private Double totalRevenue;
    private Double operatingRevenue;
    private Double otherIncome;
    
    // Cost Metrics
    private CostMetrics costMetrics;
    
    // Profit Metrics
    private ProfitMetrics profitMetrics;
    
    // Tax Metrics
    private TaxMetrics taxMetrics;
    
    // EPS Metrics
    private EpsMetrics epsMetrics;
    
    // Growth Metrics
    private GrowthMetrics growthMetrics;
}
