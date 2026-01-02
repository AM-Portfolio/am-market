package com.am.marketdata.common.model.tradeB.financials.results;

import com.am.marketdata.common.model.tradeB.financials.AbstractFinancialData;
import com.am.marketdata.common.model.tradeB.financials.adapter.FinancialDataJsonAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class StockFinancialData extends AbstractFinancialData<QuarterlyFinancialMetrics> {

    /**
     * Convert a quarter's data into a QuarterlyFinancialMetrics object
     * @param quarterKey The quarter key (e.g., "202412")
     * @return QuarterlyFinancialMetrics object containing the quarter's data
     */
    @Override
    public QuarterlyFinancialMetrics getMetrics(String quarterKey) {
        JsonNode quarterNode = data.get(quarterKey);
        if (quarterNode == null) {
            return null;
        }

        // Parse quarter data into a map
        Map<String, String> quarterData = FinancialDataJsonAdapter.parseQuarterDataToMap(quarterNode);

        QuarterlyFinancialMetrics metrics = new QuarterlyFinancialMetrics();
        
        // Set all fields from the map to QuarterlyFinancialMetrics
        if (!quarterData.isEmpty()) {
            metrics.setYearEnd(quarterData.get("year_end"));
            metrics.setTotalRevenue(FinancialDataJsonAdapter.parseDouble(quarterData.get("total_revenue")));
            metrics.setNetProfitMargin(FinancialDataJsonAdapter.parseDouble(quarterData.get("net_profit_margin")));
            metrics.setOtherIncome(FinancialDataJsonAdapter.parseDouble(quarterData.get("other_income")));
            metrics.setTotalExpenses(FinancialDataJsonAdapter.parseDouble(quarterData.get("total_expenses")));
            metrics.setEmployeeCost(FinancialDataJsonAdapter.parseDouble(quarterData.get("employee_cost")));
            metrics.setOperatingRevenue(FinancialDataJsonAdapter.parseDouble(quarterData.get("operating_revenue")));
            metrics.setDepreciationAndAmortization(FinancialDataJsonAdapter.parseDouble(quarterData.get("depreciation_and_amortization")));
            metrics.setInterest(FinancialDataJsonAdapter.parseDouble(quarterData.get("interest")));
            metrics.setProfitBeforeTax(FinancialDataJsonAdapter.parseDouble(quarterData.get("profit_before_tax")));
            metrics.setTax(FinancialDataJsonAdapter.parseDouble(quarterData.get("tax")));
            metrics.setPatMargin(FinancialDataJsonAdapter.parseDouble(quarterData.get("pat_margin")));
            metrics.setNetProfit(FinancialDataJsonAdapter.parseDouble(quarterData.get("net_profit")));
            metrics.setProfitAfterTax(FinancialDataJsonAdapter.parseDouble(quarterData.get("profit_after_tax")));
            metrics.setMinorityShare(FinancialDataJsonAdapter.parseDouble(quarterData.get("minority_share")));
            metrics.setProfitFromAssociates(FinancialDataJsonAdapter.parseDouble(quarterData.get("profit_from_associates")));
            metrics.setAdjEpsInRsBasic(FinancialDataJsonAdapter.parseDouble(quarterData.get("adj_eps_in_rs_basic")));
            metrics.setAdjEpsInRsDiluted(FinancialDataJsonAdapter.parseDouble(quarterData.get("adj_eps_in_rs_diluted")));
            metrics.setOperatingExpenses(FinancialDataJsonAdapter.parseDouble(quarterData.get("operating_expenses")));
            metrics.setOperationProfit(FinancialDataJsonAdapter.parseDouble(quarterData.get("operation_profit")));
            metrics.setOpmPercentage(FinancialDataJsonAdapter.parseDouble(quarterData.get("opm_percentage")));
            metrics.setTaxPer(FinancialDataJsonAdapter.parseDouble(quarterData.get("tax_per")));
            metrics.setRevenueGrowthPer(FinancialDataJsonAdapter.parseDouble(quarterData.get("revenue_growth_per")));
            metrics.setNetProfitGrowth(FinancialDataJsonAdapter.parseDouble(quarterData.get("net_profit_growth")));
            metrics.setNetProfitMarginGrowth(FinancialDataJsonAdapter.parseDouble(quarterData.get("net_profit_margin_growth")));
            metrics.setPatGrowth(FinancialDataJsonAdapter.parseDouble(quarterData.get("pat_growth")));
            metrics.setPatMarginGrowth(FinancialDataJsonAdapter.parseDouble(quarterData.get("pat_margin_growth")));
        }

        // If yearEnd is not set, set it based on the quarter key
        if (metrics.getYearEnd() == null) {
            metrics.setYearEnd("Y" + quarterKey);
        }

        return metrics;
    }
}
