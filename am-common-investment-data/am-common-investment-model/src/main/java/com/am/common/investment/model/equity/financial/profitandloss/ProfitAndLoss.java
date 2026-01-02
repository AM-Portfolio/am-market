package com.am.common.investment.model.equity.financial.profitandloss;

import com.am.common.investment.model.equity.financial.BaseModel;
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
public class ProfitAndLoss extends BaseModel{
    private String yearEnd;
    private String quarter;
    // Revenue Metrics
    private Double totalRevenue;
    private Double operatingRevenue;
    
    // Cost Metrics
    private CostMetrics costMetrics;
    
    // Profit Metrics
    private ProfitMetrics profitMetrics;
    
    // Growth Metrics
    private GrowthMetrics growthMetrics;
    
    // EPS Metrics
    private EpsMetrics epsMetrics;
    
    // Tax Metrics
    private TaxMetrics taxMetrics;
}
