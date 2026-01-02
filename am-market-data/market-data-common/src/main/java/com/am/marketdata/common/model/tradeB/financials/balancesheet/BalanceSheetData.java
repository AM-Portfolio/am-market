package com.am.marketdata.common.model.tradeB.financials.balancesheet;

import java.util.Map;

import com.am.marketdata.common.model.tradeB.financials.AbstractFinancialData;
import com.am.marketdata.common.model.tradeB.financials.adapter.FinancialDataJsonAdapter;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Model for balance sheet data with dynamic quarterly data structure
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BalanceSheetData extends AbstractFinancialData<BalanceSheetMetrics> {

    /**
     * Convert a quarter's data into a BalanceSheetMetrics object
     * @param quarterKey The quarter key (e.g., "202412")
     * @return BalanceSheetMetrics object containing the quarter's data
     */
    @Override
    public BalanceSheetMetrics getMetrics(String quarterKey) {
        JsonNode quarterNode = data.get(quarterKey);
        if (quarterNode == null) {
            return null;
        }

        // Parse quarter data into a map
        Map<String, String> quarterData = FinancialDataJsonAdapter.parseQuarterDataToMap(quarterNode);

        BalanceSheetMetrics metrics = new BalanceSheetMetrics();    
        
        // Set all fields from the map to CashFlowMetrics
        if (!quarterData.isEmpty()) {
            metrics.setYearEnd(quarterData.get("year_end"));
            metrics.setInventory(FinancialDataJsonAdapter.parseDouble(quarterData.get("inventory")));
            metrics.setFixedAssets(FinancialDataJsonAdapter.parseDouble(quarterData.get("fixed_assets")));
            metrics.setCapitalWorkInProgress(FinancialDataJsonAdapter.parseDouble(quarterData.get("capital_work_in_progress")));
            metrics.setIntangibleAssets(FinancialDataJsonAdapter.parseDouble(quarterData.get("intangible_assets")));
            metrics.setProvisions(FinancialDataJsonAdapter.parseDouble(quarterData.get("provisions")));
            metrics.setShortTermBorrowings(FinancialDataJsonAdapter.parseDouble(quarterData.get("short_term_borrowings")));
            metrics.setShareCapital(FinancialDataJsonAdapter.parseDouble(quarterData.get("share_capital")));
            metrics.setPreferenceCapital(FinancialDataJsonAdapter.parseDouble(quarterData.get("preference_capital")));
            metrics.setEquityCapital(FinancialDataJsonAdapter.parseDouble(quarterData.get("equity_capital")));
            metrics.setAssets(FinancialDataJsonAdapter.parseDouble(quarterData.get("assests")));
            metrics.setCurrentAssets(FinancialDataJsonAdapter.parseDouble(quarterData.get("current_assests")));
            metrics.setAccountsReceivables(FinancialDataJsonAdapter.parseDouble(quarterData.get("accounts_receivables")));
            metrics.setShortTermInvestments(FinancialDataJsonAdapter.parseDouble(quarterData.get("short_term_investments")));
            metrics.setCashAndBankBalances(FinancialDataJsonAdapter.parseDouble(quarterData.get("cash_and_bank_balances")));
            metrics.setNonCurrentAssets(FinancialDataJsonAdapter.parseDouble(quarterData.get("non_current_assests")));
            metrics.setIntangibleAssetsUnderDev(FinancialDataJsonAdapter.parseDouble(quarterData.get("intangible_assets_under_dev")));
            metrics.setLongTermInvestments(FinancialDataJsonAdapter.parseDouble(quarterData.get("long_term_investments")));
            metrics.setLiabilitiesEquity(FinancialDataJsonAdapter.parseDouble(quarterData.get("liabilities_equity")));
            metrics.setCurrentLiabilities(FinancialDataJsonAdapter.parseDouble(quarterData.get("current_liabilities")));
            metrics.setAccountPayables(FinancialDataJsonAdapter.parseDouble(quarterData.get("account_payables")));
            metrics.setNonCurrentLiabilities(FinancialDataJsonAdapter.parseDouble(quarterData.get("non_current_liabilities")));
            metrics.setTotalDebits(FinancialDataJsonAdapter.parseDouble(quarterData.get("total_debits")));
            metrics.setShareholdersFunds(FinancialDataJsonAdapter.parseDouble(quarterData.get("shareholders_funds")));
            metrics.setReserves(FinancialDataJsonAdapter.parseDouble(quarterData.get("reserves")));
            metrics.setOtherCurrentAssets(FinancialDataJsonAdapter.parseDouble(quarterData.get("other_current_assets")));
            metrics.setNetBlock(FinancialDataJsonAdapter.parseDouble(quarterData.get("net_block")));
            metrics.setOtherCurrentLiabilities(FinancialDataJsonAdapter.parseDouble(quarterData.get("other_current_liabilities")));
        }
        
        // Set the year end based on the quarter key
        metrics.setYearEnd("Y" + quarterKey);
        
        return metrics;
    }
}
