package com.am.marketdata.common.model;

import com.am.marketdata.common.model.tradeB.financials.cashflow.CashFlowResponse;
import com.am.marketdata.common.model.tradeB.financials.profitloss.ProfitLossStatementResponse;
import com.am.marketdata.common.util.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for cash flow statement deserialization
 */
class CashFlowStatementTest {

    @Test
    void shouldDeserializeCashFlowStatementWithSpecialValues() {
        // Given & When
        CashFlowResponse response = JsonUtils.fromResource("cash-flow.json", CashFlowResponse.class);

        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals("Refineries", response.getType(), "Type should be Refineries");
        assertNotNull(response.getStock(), "Stock data should not be null");

        // Verify the latest quarter (202403)
        var metrics202403 = response.getStock().getMetrics("202403");
        assertNotNull(metrics202403, "Latest quarter metrics should not be null");
        assertEquals("Y202403", metrics202403.getYearEnd(), "Year end should be Y202403");
        assertEquals(5905.0, metrics202403.getFreeCashFlow(), "Free cash flow should match");

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
