package com.am.marketdata.common.model.tradeB.financials.profitloss;

import java.util.Map;

import com.am.marketdata.common.model.tradeB.financials.AbstractFinancialData;
import com.am.marketdata.common.model.tradeB.financials.adapter.FinancialDataJsonAdapter;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Model for profit and loss statement data with dynamic quarterly data structure
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProfitLossData extends AbstractFinancialData<ProfitLossMetrics> {

    /**
     * Convert a quarter's data into a ProfitLossMetrics object
     * @param quarterKey The quarter key (e.g., "202412")
     * @return ProfitLossMetrics object containing the quarter's data
     */
    @Override
    public ProfitLossMetrics getMetrics(String quarterKey) {
         JsonNode quarterNode = data.get(quarterKey);
        if (quarterNode == null) {
            return null;
        }

        // Parse quarter data into a map
        Map<String, String> quarterData = FinancialDataJsonAdapter.parseQuarterDataToMap(quarterNode);

        ProfitLossMetrics metrics = new ProfitLossMetrics();
        
        // Set all fields from the map to ProfitLossMetrics
        if (!quarterData.isEmpty()) {
            metrics.setYearEnd(quarterData.get("year_end"));
            metrics.setTotalRevenue(FinancialDataJsonAdapter.parseDouble(quarterData.get("total_revenue")));
            metrics.setNetProfitMargin(FinancialDataJsonAdapter.parseDouble(quarterData.get("net_profit_margin")));
            metrics.setOtherIncome(FinancialDataJsonAdapter.parseDouble(quarterData.get("other_income")));
            metrics.setDepreciationAndAmortization(FinancialDataJsonAdapter.parseDouble(quarterData.get("depreciation_and_amortization")));
            metrics.setOperatingRevenue(FinancialDataJsonAdapter.parseDouble(quarterData.get("operating_revenue")));
            metrics.setTotalExpenditure(FinancialDataJsonAdapter.parseDouble(quarterData.get("total_expenditure")));
            metrics.setRawMaterialCost(FinancialDataJsonAdapter.parseDouble(quarterData.get("raw_material_cost")));
            metrics.setManufacturingCost(FinancialDataJsonAdapter.parseDouble(quarterData.get("manufacturing_cost")));
            metrics.setEmployeeCost(FinancialDataJsonAdapter.parseDouble(quarterData.get("employee_cost")));
            metrics.setInterest(FinancialDataJsonAdapter.parseDouble(quarterData.get("interest")));
            metrics.setTax(FinancialDataJsonAdapter.parseDouble(quarterData.get("tax")));
            metrics.setNetProfit(FinancialDataJsonAdapter.parseDouble(quarterData.get("net_profit")));
            metrics.setMinorityShare(FinancialDataJsonAdapter.parseDouble(quarterData.get("minority_share")));
            metrics.setBasicEpsRs(FinancialDataJsonAdapter.parseDouble(quarterData.get("basic_eps_rs")));
            metrics.setDilutedEpsRs(FinancialDataJsonAdapter.parseDouble(quarterData.get("diluted_eps_rs")));
            metrics.setOtherCost(FinancialDataJsonAdapter.parseDouble(quarterData.get("other_cost")));
            metrics.setOperatingExpenses(FinancialDataJsonAdapter.parseDouble(quarterData.get("operating_expenses")));
            metrics.setOperationProfit(FinancialDataJsonAdapter.parseDouble(quarterData.get("operation_profit")));
            metrics.setOpmPercentage(FinancialDataJsonAdapter.parseDouble(quarterData.get("opm_percentage")));
            metrics.setProfitBeforeTax(FinancialDataJsonAdapter.parseDouble(quarterData.get("profit_before_tax")));
            metrics.setProfitAfterTax(FinancialDataJsonAdapter.parseDouble(quarterData.get("profit_after_tax")));
            metrics.setTaxPer(FinancialDataJsonAdapter.parseDouble(quarterData.get("tax_per")));
            metrics.setRevenueGrowthPer(FinancialDataJsonAdapter.parseDouble(quarterData.get("revenue_growth_per")));
            metrics.setNetProfitGrowth(FinancialDataJsonAdapter.parseDouble(quarterData.get("net_profit_growth")));
            metrics.setNetProfitMarginGrowth(FinancialDataJsonAdapter.parseDouble(quarterData.get("net_profit_margin_growth")));
        }
        
        // Set the year end based on the quarter key
        metrics.setYearEnd("Y" + quarterKey);
        
        return metrics;
    }
}
