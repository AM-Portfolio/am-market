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
public class CashFlowInvestingMetrics {
    private Double cashFromInvestingActivities;
    
    // Fixed Assets
    private Double fixedAssetsPurchased;
    private Double fixedAssetsSold;
    
    // Investments
    private Double investmentsPurchased;
    private Double investmentsSold;
    private Double otherInvestingItems;
    
    // Other Investments
    private Double acquisitionOfCompanies;
    private Double interCorporateDeposits;
    private Double investmentInGroupCos;
    private Double investmentInSubsidiaries;
}
