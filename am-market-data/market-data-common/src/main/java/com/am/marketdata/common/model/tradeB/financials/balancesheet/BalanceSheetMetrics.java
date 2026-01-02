package com.am.marketdata.common.model.tradeB.financials.balancesheet;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Model for quarterly balance sheet metrics
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BalanceSheetMetrics {
    private String yearEnd;
    private Double inventory;
    private Double fixedAssets;
    private Double capitalWorkInProgress;
    private Double intangibleAssets;
    private Double provisions;
    private Double shortTermBorrowings;
    private Double shareCapital;
    private Double preferenceCapital;
    private Double equityCapital;
    private Double assets;
    private Double currentAssets;
    private Double accountsReceivables;
    private Double shortTermInvestments;
    private Double cashAndBankBalances;
    private Double nonCurrentAssets;
    private Double intangibleAssetsUnderDev;
    private Double longTermInvestments;
    private Double liabilitiesEquity;
    private Double currentLiabilities;
    private Double accountPayables;
    private Double nonCurrentLiabilities;
    private Double totalDebits;
    private Double shareholdersFunds;
    private Double reserves;
    private Double otherCurrentAssets;
    private Double netBlock;
    private Double otherCurrentLiabilities;
}
