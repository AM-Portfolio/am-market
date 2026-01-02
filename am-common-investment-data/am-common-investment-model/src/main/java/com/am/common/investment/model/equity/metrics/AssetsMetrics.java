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
public class AssetsMetrics {
    private Double inventory;
    private Double fixedAssets;
    private Double capitalWorkInProgress;
    private Double intangibleAssets;
    private Double intangibleAssetsUnderDev;
    private Double netBlock;
    
    // Current Assets
    private Double currentAssets;
    private Double accountsReceivables;
    private Double shortTermInvestments;
    private Double cashAndBankBalances;
    
    // Non-Current Assets
    private Double nonCurrentAssets;
    private Double longTermInvestments;
}
