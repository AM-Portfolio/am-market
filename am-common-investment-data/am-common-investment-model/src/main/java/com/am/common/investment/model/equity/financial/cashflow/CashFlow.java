package com.am.common.investment.model.equity.financial.cashflow;

import com.am.common.investment.model.equity.metrics.CashFlowOperatingMetrics;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.am.common.investment.model.equity.metrics.CashFlowInvestingMetrics;
import com.am.common.investment.model.equity.financial.BaseModel;
import com.am.common.investment.model.equity.metrics.CashFlowFinancingMetrics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CashFlow extends BaseModel{
    private String yearEnd;
    private String quarter;
    
    // Operating Activities
    private CashFlowOperatingMetrics operatingMetrics;
    
    // Investing Activities
    private CashFlowInvestingMetrics investingMetrics;
    
    // Financing Activities
    private CashFlowFinancingMetrics financingMetrics;
    
    // Summary Metrics
    private Double netCashFlow;
    private Double freeCashFlow;
}
