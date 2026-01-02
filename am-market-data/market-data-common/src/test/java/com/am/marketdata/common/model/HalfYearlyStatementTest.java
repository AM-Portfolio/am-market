package com.am.marketdata.common.model;

import com.am.marketdata.common.model.tradeB.financials.halfYearly.HalfYearlyStatementResponse;
import com.am.marketdata.common.util.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for half yearly statement deserialization
 */
class HalfYearlyStatementTest {

    @Test
    void shouldDeserializeHalfYearlyStatement() {
        // Given & When
        HalfYearlyStatementResponse response = JsonUtils.fromResource("halfyearly-result.json", HalfYearlyStatementResponse.class);

        // Then
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getStock(), "Stock data should not be null");
        assertEquals("Refineries", response.getType(), "Type should be Refineries");

        // Verify the latest quarter (202409)
        var metrics202409 = response.getStock().getMetrics("202409");
        assertNotNull(metrics202409, "Latest quarter metrics should not be null");
        assertEquals(472178.0, metrics202409.getTotalRevenue(), "Total revenue should match");
        assertEquals(7.74, metrics202409.getPatMargin(), "PAT margin should match");
        assertEquals(46.85, metrics202409.getAdjEpsInRsBasic(), "Adjusted EPS basic should match");
        assertEquals(46.85, metrics202409.getAdjEpsInRsDiluted(), "Adjusted EPS diluted should match");
        assertEquals(13.95, metrics202409.getOpmPercentage(), "OPM percentage should match");

        // Verify a previous quarter (202403)
        var metrics202403 = response.getStock().getMetrics("202403");
        assertNotNull(metrics202403, "Previous quarter metrics should not be null");
        assertEquals(447099.0, metrics202403.getTotalRevenue(), "Total revenue should match");
        assertEquals(8.53, metrics202403.getPatMargin(), "PAT margin should match");
        assertEquals(49.37, metrics202403.getAdjEpsInRsBasic(), "Adjusted EPS basic should match");
        assertEquals(49.37, metrics202403.getAdjEpsInRsDiluted(), "Adjusted EPS diluted should match");
        assertEquals(15.1, metrics202403.getOpmPercentage(), "OPM percentage should match");
    }
}
