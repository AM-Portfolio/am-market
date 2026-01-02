package com.am.marketdata.common.model.tradeB.financials.dividend;

import java.util.Map;

import com.am.marketdata.common.model.tradeB.financials.AbstractFinancialData;
import com.am.marketdata.common.model.tradeB.financials.adapter.FinancialDataJsonAdapter;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Model for dividend data with dynamic quarterly data structure
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DividendData extends AbstractFinancialData<DividendMetrics> {
    
    /**
     * Convert a quarter's data into a DividendMetrics object
     * @param quarterKey The quarter key (e.g., "202403")
     * @return DividendMetrics object containing the quarter's data
     */
    @Override
    public DividendMetrics getMetrics(String quarterKey) {
        JsonNode quarterNode = data.get(quarterKey);
        if (quarterNode == null) {
            return null;
        }

        // Parse quarter data into a map
        Map<String, String> quarterData = FinancialDataJsonAdapter.parseQuarterDataToMap(quarterNode);

        DividendMetrics metrics = new DividendMetrics();    
        
        // Set all fields from the map to DividendMetrics
        if (!quarterData.isEmpty()) {
            metrics.populateFromMap(quarterData);
        }
        
        return metrics;
    }
}
