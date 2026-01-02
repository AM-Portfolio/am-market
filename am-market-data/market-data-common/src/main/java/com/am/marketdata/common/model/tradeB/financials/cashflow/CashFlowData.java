package com.am.marketdata.common.model.tradeB.financials.cashflow;

import java.util.Map;

import com.am.marketdata.common.model.tradeB.financials.AbstractFinancialData;
import com.am.marketdata.common.model.tradeB.financials.adapter.FinancialDataJsonAdapter;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Model for cash flow data with dynamic quarterly data structure
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CashFlowData extends AbstractFinancialData<CashFlowMetrics> {

    /**
     * Convert a quarter's data into a CashFlowMetrics object
     * @param quarterKey The quarter key (e.g., "202412")
     * @return CashFlowMetrics object containing the quarter's data
     */
    @Override
    public CashFlowMetrics getMetrics(String quarterKey) {
        JsonNode quarterNode = data.get(quarterKey);
        if (quarterNode == null) {
            return null;
        }

        // Parse quarter data into a map
        Map<String, String> quarterData = FinancialDataJsonAdapter.parseQuarterDataToMap(quarterNode);

        CashFlowMetrics metrics = new CashFlowMetrics();    
        
        // Set all fields from the map to CashFlowMetrics
        if (!quarterData.isEmpty()) {
            metrics.setYearEnd(quarterData.get("year_end"));
            metrics.setCashFromOperatingActivities(FinancialDataJsonAdapter.parseDouble(quarterData.get("cash_from_operating_activities")));
            metrics.setProfitBeforeTax(FinancialDataJsonAdapter.parseDouble(quarterData.get("profit_before_tax")));
            metrics.setDepreciationAndAmortization(FinancialDataJsonAdapter.parseDouble(quarterData.get("depreciation_and_amortization")));
            metrics.setInterestExpense(FinancialDataJsonAdapter.parseDouble(quarterData.get("interest_expense")));
            metrics.setChangeInWorkingCapital(FinancialDataJsonAdapter.parseDouble(quarterData.get("change_in_working_capital")));
            metrics.setTaxPaid(FinancialDataJsonAdapter.parseDouble(quarterData.get("tax_paid")));
            metrics.setCashFromInvestingActivities(FinancialDataJsonAdapter.parseDouble(quarterData.get("cash_from_investing_activities")));
            metrics.setCapitalExpenditure(FinancialDataJsonAdapter.parseDouble(quarterData.get("capital_expenditure")));
            metrics.setInvestments(FinancialDataJsonAdapter.parseDouble(quarterData.get("investments")));
            metrics.setCashFromFinancingActivities(FinancialDataJsonAdapter.parseDouble(quarterData.get("cash_from_financing_activities")));
            metrics.setDebtIssued(FinancialDataJsonAdapter.parseDouble(quarterData.get("debt_issued")));
            metrics.setDebtRepayment(FinancialDataJsonAdapter.parseDouble(quarterData.get("debt_repayment")));
            metrics.setDividendsPaid(FinancialDataJsonAdapter.parseDouble(quarterData.get("dividends_paid")));
            metrics.setNetChangeInCash(FinancialDataJsonAdapter.parseDouble(quarterData.get("net_change_in_cash")));
            metrics.setBeginningCashBalance(FinancialDataJsonAdapter.parseDouble(quarterData.get("beginning_cash_balance")));
            metrics.setEndingCashBalance(FinancialDataJsonAdapter.parseDouble(quarterData.get("ending_cash_balance")));
            metrics.setFreeCashFlow(FinancialDataJsonAdapter.parseDouble(quarterData.get("free_cash_flow")));
        }
        
        // Set the year end based on the quarter key
        metrics.setYearEnd("Y" + quarterKey);
        
        return metrics;
    }
}
