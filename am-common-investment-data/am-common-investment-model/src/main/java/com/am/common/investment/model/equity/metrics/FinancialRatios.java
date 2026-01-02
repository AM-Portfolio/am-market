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
public class FinancialRatios {
    // Market Ratios
    private Double pe;
    private Double evEbitda;
    private Double priceBookValue;
    private Double priceToCashflow;
    private Double priceToFreeCashflow;
    private Double priceSalesRatio;
    
    // Liquidity Ratios
    private Double currentRatio;
    private Double quickRatio;
    
    // Efficiency Ratios
    private Double receivableDays;
    private Double inventoryDays;
    private Double payableDays;
    private Double workingCapitalDays;
    private Double inventoryTurnoverRatio;
    private Double assetTurnoverRatio;
    private Double cashConversionCycle;
    
    // Profitability Ratios
    private Double pbditMargin;
    private Double pbtMargin;
    private Double ebitMargin;
    private Double netProfitMargin;
    private Double contributionProfitMargin;
    private Double pbitMargin;
    
    // Return Ratios
    private Double returnOnEquity;
    private Double returnOnAssets;
    private Double returnOnCapEmployed;
    
    // Leverage Ratios
    private Double interestCoverageRatio;
    private Double debtToEquityRatio;
    private Double totalDebtToMarketCap;
    private Double fixedCapitalToSalesRatio;
    
    // Growth Ratios
    private Double marketCapToSales;
    private Double pegRatio;
}
