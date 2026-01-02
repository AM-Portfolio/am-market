package com.am.marketdata.common.model;

import com.am.marketdata.common.model.tradeB.financials.results.QuaterlyFinancialStatementResponse;
import com.am.marketdata.common.util.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuaterlyResultTest {

    @Test
    void shouldDeserializeQuaterlyResultWithSpecialValues() {
        // Given & When
        QuaterlyFinancialStatementResponse response = JsonUtils.fromResource("quaterly-result.json", QuaterlyFinancialStatementResponse.class);

        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals("Refineries", response.getType(), "Type should be Refineries");
        assertNotNull(response.getStock(), "Stock data should not be null");

        // Verify the latest quarter (202412)
        var metrics202412 = response.getStock().getMetrics("202412");
        assertNotNull(metrics202412, "Latest quarter metrics should not be null");
        assertEquals("Y202412", metrics202412.getYearEnd(), "Year end should be Y202412");
        assertEquals(244200.0, metrics202412.getTotalRevenue(), "Total revenue should match");
        assertEquals(7.46, metrics202412.getNetProfitMargin(), "Net profit margin should match");
        assertEquals(13.7, metrics202412.getAdjEpsInRsBasic(), "Adjusted EPS basic should match");
        assertEquals(13.7, metrics202412.getAdjEpsInRsDiluted(), "Adjusted EPS diluted should match");

        assertEquals(17.63, metrics202412.getOpmPercentage(), "OPM percentage should match");
        assertEquals(4.05, metrics202412.getPatMarginGrowth(), "PAT margin growth should match");

        // Verify a previous quarter (202409)
        var metrics202409 = response.getStock().getMetrics("202409");
        assertNotNull(metrics202409, "Previous quarter metrics should not be null");
        assertEquals("Y202409", metrics202409.getYearEnd(), "Year end should be Y202409");
        assertEquals(236411.0, metrics202409.getTotalRevenue(), "Total revenue should match");
        assertEquals(6.86, metrics202409.getNetProfitMargin(), "Net profit margin should match");
        assertEquals(24.48, metrics202409.getAdjEpsInRsBasic(), "Adjusted EPS basic should match");
        assertEquals(24.48, metrics202409.getAdjEpsInRsDiluted(), "Adjusted EPS diluted should match");
        assertEquals(16.19, metrics202409.getOpmPercentage(), "OPM percentage should match");
        assertEquals(-4.59, metrics202409.getPatMarginGrowth(), "PAT margin growth should match");

        // Verify that all quarters are present
        assertTrue(response.getStock().getQuarterKeys().contains("202412"), "202412 quarter should be present");
        assertTrue(response.getStock().getQuarterKeys().contains("202409"), "202409 quarter should be present");
        assertTrue(response.getStock().getQuarterKeys().contains("202406"), "202406 quarter should be present");
        assertTrue(response.getStock().getQuarterKeys().contains("202403"), "202403 quarter should be present");
        assertTrue(response.getStock().getQuarterKeys().contains("202312"), "202312 quarter should be present");
    }

    @Test
    void shouldHandleEmptyResponse() {
        // Given
        String emptyJson = "{}";

        // When
        QuaterlyFinancialStatementResponse response = JsonUtils.fromJson(emptyJson, QuaterlyFinancialStatementResponse.class);

        // Then
        assertNotNull(response, "Empty response should still create an object");
        assertNull(response.getType(), "Type should be null");
        assertNull(response.getStock(), "Stock data should be null");
    }

    @Test
    void shouldHandleInvalidJson() {
        // Given
        String invalidJson = "{ invalid: json }";

        // When
        QuaterlyFinancialStatementResponse response = JsonUtils.fromJson(invalidJson, QuaterlyFinancialStatementResponse.class);

        // Then
        assertNull(response, "Invalid JSON should return null");
    }

    @Test
    void shouldHandleMissingFields() {
        // Given
        String jsonWithMissingFields = "{\"type\":\"Refineries\"}";

        // When
        QuaterlyFinancialStatementResponse response = JsonUtils.fromJson(jsonWithMissingFields, QuaterlyFinancialStatementResponse.class);

        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals("Refineries", response.getType(), "Type should be Refineries");
        assertNull(response.getStock(), "Stock data should be null");
    }
}
