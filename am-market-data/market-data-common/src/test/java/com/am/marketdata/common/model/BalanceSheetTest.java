package com.am.marketdata.common.model;

import com.am.marketdata.common.model.tradeB.financials.balancesheet.BalanceSheetResponse;
import com.am.marketdata.common.util.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for balance sheet deserialization
 */
class BalanceSheetTest {

    @Test
    void shouldDeserializeBalanceSheetWithSpecialValues() {
        // Given & When
        BalanceSheetResponse response = JsonUtils.fromResource("balance-sheet.json", BalanceSheetResponse.class);

        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals("Refineries", response.getType(), "Type should be Refineries");
        assertNotNull(response.getStock(), "Stock data should not be null");

        // Verify the latest quarter (202403)
        var metrics202403 = response.getStock().getMetrics("202403");
        assertNotNull(metrics202403, "Latest quarter metrics should not be null");
        assertEquals("Y202403", metrics202403.getYearEnd(), "Year end should be Y202403");
        assertEquals(152770.0, metrics202403.getInventory(), "Inventory should match");
        assertEquals(966458.0, metrics202403.getFixedAssets(), "Fixed assets should match");
        assertEquals(6766.0, metrics202403.getShareCapital(), "Share capital should match");
        assertEquals(1755986.0, metrics202403.getAssets(), "Assets should match");
        assertEquals(793481.0, metrics202403.getShareholdersFunds(), "Shareholders funds should match");

        // Verify a previous quarter (202303)
        var metrics202303 = response.getStock().getMetrics("202303");
        assertNotNull(metrics202303, "Previous quarter metrics should not be null");
        assertEquals("Y202303", metrics202303.getYearEnd(), "Year end should be Y202303");
        assertEquals(140008.0, metrics202303.getInventory(), "Inventory should match");
        assertEquals(901298.0, metrics202303.getFixedAssets(), "Fixed assets should match");
        assertEquals(6766.0, metrics202303.getShareCapital(), "Share capital should match");
        assertEquals(1607431.0, metrics202303.getAssets(), "Assets should match");

        // Verify that multiple quarters are present
        assertTrue(response.getStock().getQuarterKeys().contains("202403"), "202403 quarter should be present");
        assertTrue(response.getStock().getQuarterKeys().contains("202303"), "202303 quarter should be present");
    }

    @Test
    void shouldHandleEmptyResponse() {
        // Given
        String emptyJson = "{}";

        // When
        BalanceSheetResponse response = JsonUtils.fromJson(emptyJson, BalanceSheetResponse.class);

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
        BalanceSheetResponse response = JsonUtils.fromJson(invalidJson, BalanceSheetResponse.class);

        // Then
        assertNull(response, "Invalid JSON should return null");
    }

    @Test
    void shouldHandleMissingFields() {
        // Given
        String jsonWithMissingFields = "{\"type\":\"Refineries\"}";

        // When
        BalanceSheetResponse response = JsonUtils.fromJson(jsonWithMissingFields, BalanceSheetResponse.class);

        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals("Refineries", response.getType(), "Type should be Refineries");
        assertNull(response.getStock(), "Stock data should be null");
    }
}
