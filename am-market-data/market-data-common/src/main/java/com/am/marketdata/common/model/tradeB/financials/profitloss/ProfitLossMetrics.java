package com.am.marketdata.common.model.tradeB.financials.profitloss;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Model for quarterly profit and loss metrics
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfitLossMetrics {
    private String yearEnd;
    private Double totalRevenue;
    private Double netProfitMargin;
    private Double otherIncome;
    private Double depreciationAndAmortization;
    private Double operatingRevenue;
    private Double totalExpenditure;
    private Double rawMaterialCost;
    private Double manufacturingCost;
    private Double employeeCost;
    private Double interest;
    private Double tax;
    private Double netProfit;
    private Double minorityShare;
    private Double basicEpsRs;
    private Double dilutedEpsRs;
    private Double otherCost;
    private Double operatingExpenses;
    private Double operationProfit;
    private Double opmPercentage;
    private Double profitBeforeTax;
    private Double profitAfterTax;
    private Double taxPer;
    private Double revenueGrowthPer;
    private Double netProfitGrowth;
    private Double netProfitMarginGrowth;
}
