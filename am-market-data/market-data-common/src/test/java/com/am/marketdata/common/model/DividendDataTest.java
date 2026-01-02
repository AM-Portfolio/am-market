package com.am.marketdata.common.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.am.marketdata.common.model.tradeB.financials.dividend.FactSheetDividendResponse;
import com.am.marketdata.common.util.JsonUtils;

public class DividendDataTest {
    
    @Test
    void shouldDeserializeDividendDataWithSpecialValues() {
        // Given & When
        FactSheetDividendResponse response = JsonUtils.fromResource("factsheet-dividend.json", FactSheetDividendResponse.class);

        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals("Refineries", response.getType(), "Type should be Refineries");
        assertNotNull(response.getStock(), "Stock data should not be null");

        // Verify the latest quarter (202403)
        var metrics202403 = response.getStock().getMetrics("202403");
        assertNotNull(metrics202403, "Latest quarter metrics should not be null");
        assertEquals(10.0, metrics202403.getDividendPerShare(), "Dividend per share should match");

    }
}
