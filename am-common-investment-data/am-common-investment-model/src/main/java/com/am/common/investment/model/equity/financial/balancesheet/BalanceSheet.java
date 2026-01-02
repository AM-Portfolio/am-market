package com.am.common.investment.model.equity.financial.balancesheet;

import com.am.common.investment.model.equity.financial.BaseModel;
import com.am.common.investment.model.equity.metrics.AssetsMetrics;
import com.am.common.investment.model.equity.metrics.EquityMetrics;
import com.am.common.investment.model.equity.metrics.LiabilitiesMetrics;
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
public class BalanceSheet extends BaseModel{
    private String yearEnd;
    private String quarter;
    
    // Total Metrics
    private Double assets;
    private Double liabilitiesEquity;
    private Double totalDebits;
    
    // Assets Metrics
    private AssetsMetrics assetsMetrics;
    
    // Liabilities Metrics
    private LiabilitiesMetrics liabilitiesMetrics;
    
    // Equity Metrics
    private EquityMetrics equityMetrics;
}
