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
public class CashFlowFinancingMetrics {
    private Double cashFromFinancialActivities;
    private Double netCashFlow;
    
    // Borrowings
    private Double proceedsFromBorrowings;
    private Double repaymentBorrowings;
    private Double interestPaidFin;
    
    // Share Capital
    private Double proceedsFromShares;
    private Double redemptionAndCancellationOfShares;
    
    // Financial Liabilities
    private Double financialLiabilities;
    private Double otherFinancingItems;
    
    // Dividends
    private Double dividendPaid;
    private Double dividendReceived;
}
