package com.am.marketdata.common.model;

import com.am.marketdata.common.model.tradeB.financials.profitloss.ProfitLossStatementResponse;
import com.am.marketdata.common.util.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for profit and loss statement deserialization
 */
class ProfitLossStatementTest {

    @Test
    void shouldDeserializeProfitLossStatementWithSpecialValues() {
        // Given & When
        ProfitLossStatementResponse response = JsonUtils.fromResource("profit-loss.json", ProfitLossStatementResponse.class);

        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals("Refineries", response.getType(), "Type should be Refineries");
        assertNotNull(response.getStock(), "Stock data should not be null");

        // Verify the latest quarter (202403)
        var metrics202403 = response.getStock().getMetrics("202403");
        assertNotNull(metrics202403, "Latest quarter metrics should not be null");
        assertEquals("Y202403", metrics202403.getYearEnd(), "Year end should be Y202403");
        assertEquals(917121.0, metrics202403.getTotalRevenue(), "Total revenue should match");
        assertEquals(7.46, metrics202403.getNetProfitMargin(), "Net profit margin should match");
        assertEquals(102.9, metrics202403.getBasicEpsRs(), "Basic EPS should match");
        assertEquals(102.9, metrics202403.getDilutedEpsRs(), "Diluted EPS should match");
        assertEquals(17.39, metrics202403.getOpmPercentage(), "OPM percentage should match");
        assertEquals(3.1, metrics202403.getRevenueGrowthPer(), "Revenue growth percentage should match");

        // Verify a previous quarter (202303)
        var metrics202303 = response.getStock().getMetrics("202303");
        assertNotNull(metrics202303, "Previous quarter metrics should not be null");
        assertEquals("Y202303", metrics202303.getYearEnd(), "Year end should be Y202303");
        assertEquals(889569.0, metrics202303.getTotalRevenue(), "Total revenue should match");
        assertEquals(7.4, metrics202303.getNetProfitMargin(), "Net profit margin should match");
        assertEquals(98.59, metrics202303.getBasicEpsRs(), "Basic EPS should match");
        assertEquals(98.59, metrics202303.getDilutedEpsRs(), "Diluted EPS should match");
        assertEquals(15.77, metrics202303.getOpmPercentage(), "OPM percentage should match");

        // Verify that multiple quarters are present
        assertTrue(response.getStock().getQuarterKeys().contains("202403"), "202403 quarter should be present");
        assertTrue(response.getStock().getQuarterKeys().contains("202303"), "202303 quarter should be present");
    }

    @Test
    void shouldHandleEmptyResponse() {
        // Given
        String emptyJson = "{}";

        // When
        ProfitLossStatementResponse response = JsonUtils.fromJson(emptyJson, ProfitLossStatementResponse.class);

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
        ProfitLossStatementResponse response = JsonUtils.fromJson(invalidJson, ProfitLossStatementResponse.class);

        // Then
        assertNull(response, "Invalid JSON should return null");
    }

    @Test
    void shouldHandleMissingFields() {
        // Given
        String jsonWithMissingFields = "{\"type\":\"Refineries\"}";

        // When
        ProfitLossStatementResponse response = JsonUtils.fromJson(jsonWithMissingFields, ProfitLossStatementResponse.class);

        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals("Refineries", response.getType(), "Type should be Refineries");
        assertNull(response.getStock(), "Stock data should be null");
    }
}
