package com.am.common.investment.model.equity.financial.factsheetdividend;

import com.am.common.investment.model.equity.metrics.FinancialRatios;
import com.am.common.investment.model.equity.financial.BaseModel;
import com.am.common.investment.model.equity.metrics.DividendMetrics;
import com.am.common.investment.model.equity.metrics.GrowthMetrics;
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
public class FactSheetDividend extends BaseModel{

    private String yearEnd;
    private String quarter;
    
    // Growth Metrics
    private GrowthMetrics growthMetrics;
    
    // Financial Ratios
    private FinancialRatios financialRatios;
    
    // Dividend Metrics
    private DividendMetrics dividendMetrics;
    
    // Additional Metrics
    private Double assetTurnoverRatio;
    private Double workingCapitalDays;
    private Double inventoryTurnoverRatio;
    private Double adjEarningsPerShare;
    private Double enterpriseValue;
    private Double pegRatio;
    private Double priceSalesRatio;
    private Double adjDividendPerShare;
    private Double freeCashFlowPerShare;
    private Double cashConversionCycle;
    private Double freeCashFlowYield;
}
