package com.am.marketdata.common.model.tradeB.financials.cashflow;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Model for quarterly cash flow metrics
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CashFlowMetrics {
    private String yearEnd;
    private Double cashFromOperatingActivities;
    private Double profitBeforeTax;
    private Double depreciationAndAmortization;
    private Double interestExpense;
    private Double changeInWorkingCapital;
    private Double taxPaid;
    private Double cashFromInvestingActivities;
    private Double capitalExpenditure;
    private Double investments;
    private Double cashFromFinancingActivities;
    private Double debtIssued;
    private Double debtRepayment;
    private Double dividendsPaid;
    private Double netChangeInCash;
    private Double beginningCashBalance;
    private Double endingCashBalance;
    private Double freeCashFlow;
}
