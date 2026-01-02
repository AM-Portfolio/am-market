package com.am.marketdata.common.model.tradeB.financials.halfYearly;

import java.util.Map;

import com.am.marketdata.common.model.tradeB.financials.AbstractFinancialData;
import com.am.marketdata.common.model.tradeB.financials.adapter.FinancialDataJsonAdapter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockHalfYearlyData extends AbstractFinancialData<HalfYearlyFinancialMetrics> {
    /**
     * Convert a quarter's data into a CashFlowMetrics object
     * @param quarterKey The quarter key (e.g., "202412")
     * @return CashFlowMetrics object containing the quarter's data
     */
    @Override
    public HalfYearlyFinancialMetrics getMetrics(String quarterKey) {
        JsonNode quarterNode = data.get(quarterKey);
        if (quarterNode == null) {
            return null;
        }

        // Parse quarter data into a map
        Map<String, String> quarterData = FinancialDataJsonAdapter.parseQuarterDataToMap(quarterNode);

        HalfYearlyFinancialMetrics metrics = new HalfYearlyFinancialMetrics();    
        
        // Set all fields from the map to CashFlowMetrics
        if (!quarterData.isEmpty()) {
            metrics.setTotalRevenue(FinancialDataJsonAdapter.parseDouble(quarterData.get("total_revenue")));
            metrics.setOperatingRevenue(FinancialDataJsonAdapter.parseDouble(quarterData.get("operating_revenue")));
            metrics.setTotalExpenses(FinancialDataJsonAdapter.parseDouble(quarterData.get("total_expenses")));
            metrics.setOtherIncome(FinancialDataJsonAdapter.parseDouble(quarterData.get("other_income")));
            metrics.setTax(FinancialDataJsonAdapter.parseDouble(quarterData.get("tax")));
            metrics.setInterest(FinancialDataJsonAdapter.parseDouble(quarterData.get("interest")));
            metrics.setEmployeeCost(FinancialDataJsonAdapter.parseDouble(quarterData.get("employee_cost")));
            metrics.setDepreciationAndAmortization(FinancialDataJsonAdapter.parseDouble(quarterData.get("depreciation_and_amortization")));
            metrics.setProfitAfterTax(FinancialDataJsonAdapter.parseDouble(quarterData.get("profit_after_tax")));
            metrics.setProfitBeforeTax(FinancialDataJsonAdapter.parseDouble(quarterData.get("profit_before_tax")));
            metrics.setAdjEpsInRsBasic(FinancialDataJsonAdapter.parseDouble(quarterData.get("adj_eps_in_rs_basic")));
            metrics.setAdjEpsInRsDiluted(FinancialDataJsonAdapter.parseDouble(quarterData.get("adj_eps_in_rs_diluted")));
            metrics.setPatMargin(FinancialDataJsonAdapter.parseDouble(quarterData.get("pat_margin")));
            metrics.setOperatingExpenses(FinancialDataJsonAdapter.parseDouble(quarterData.get("operating_expenses")));
            metrics.setOperatingProfit(FinancialDataJsonAdapter.parseDouble(quarterData.get("operating_profit")));
            metrics.setOpmPercentage(FinancialDataJsonAdapter.parseDouble(quarterData.get("opm_percentage")));
            metrics.setTaxPer(FinancialDataJsonAdapter.parseDouble(quarterData.get("tax_per")));
            metrics.setRevenueGrowthPer(FinancialDataJsonAdapter.parseDouble(quarterData.get("revenue_growth_per")));
            metrics.setPatGrowth(FinancialDataJsonAdapter.parseDouble(quarterData.get("pat_growth")));
            metrics.setPatMarginGrowth(FinancialDataJsonAdapter.parseDouble(quarterData.get("pat_margin_growth")));
        }
        
        return metrics;
    }
}
