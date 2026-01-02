package com.am.marketdata.common.model.tradeB.financials.results;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuarterlyFinancialMetrics {
    private String yearEnd;
    private Double totalRevenue;
    private Double netProfitMargin;
    private Double otherIncome;
    private Double totalExpenses;
    private Double employeeCost;
    private Double operatingRevenue;
    private Double depreciationAndAmortization;
    private Double interest;
    private Double profitBeforeTax;
    private Double tax;
    private Double patMargin;
    private Double netProfit;
    private Double profitAfterTax;
    private Double minorityShare;
    private Double profitFromAssociates;
    private Double adjEpsInRsBasic;
    private Double adjEpsInRsDiluted;
    private Double operatingExpenses;
    private Double operationProfit;
    private Double opmPercentage;
    private Double taxPer;
    private Double revenueGrowthPer;
    private Double netProfitGrowth;
    private Double netProfitMarginGrowth;
    private Double patGrowth;
    private Double patMarginGrowth;
}
