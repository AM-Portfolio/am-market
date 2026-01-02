package com.am.marketdata.common.model;

import com.am.marketdata.common.util.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NseETFResponseTest {

    @Test
    void shouldDeserializeETFResponseWithSpecialValues() {
        // Given & When
        NseETFResponse response = JsonUtils.fromResource("etf.json", NseETFResponse.class);

        // Then
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getData(), "ETF data list should not be null");
        assertEquals(1, response.getData().size(), "Should have one ETF entry");

        NseETF etf = response.getData().get(0);
        assertEquals("MAHKTECH", etf.getSymbol());
        assertEquals("Hang Seng TECH Total Return Index", etf.getAssets());
        
        // Test numeric values
        assertEquals(26.33, etf.getOpen());
        assertEquals(26.74, etf.getHigh());
        assertEquals(25.00, etf.getLow());
        assertEquals(26.15, etf.getLastTradedPrice());
        assertEquals(0.39, etf.getChange());
        assertEquals(1.51, etf.getPercentChange());
        assertEquals(2853791, etf.getQuantity());
        assertEquals(73884648.99, etf.getTradedValue());
        assertEquals(22.4962, etf.getNav());
        
        // Test special "-" values that should be null
        assertEquals(106.55608214849919, etf.getYearlyPercentageChange());
        assertEquals(0.0, etf.getStockIndClosePrice());
        assertEquals(-117.01244813278005, etf.getNearWKL());
        assertEquals(103.48, etf.getPerChange365d());
        
        // Test metadata
        assertNotNull(etf.getMeta(), "ETF metadata should not be null");
        assertEquals("Mirae Asset Mutual Fund - Mirae Asset Hang Seng TECH ETF", etf.getMeta().getCompanyName());
        assertTrue(etf.getMeta().getIsETFSec(), "Should be marked as ETF security");
        assertFalse(etf.getMeta().getIsDelisted(), "Should not be marked as delisted");
        
        // Test market status
        assertNotNull(response.getMarketStatus(), "Market status should not be null");
        assertEquals("Capital Market", response.getMarketStatus().getMarket());
        assertEquals("Open", response.getMarketStatus().getMarketStatus());
        assertEquals("10-Mar-2025 14:20", response.getMarketStatus().getTradeDate());
        assertEquals("NIFTY 50", response.getMarketStatus().getIndex());
        assertEquals(-46.099999999998545, response.getMarketStatus().getVariation());
        assertEquals(-0.2, response.getMarketStatus().getPercentChange());
        assertEquals("Normal Market is Open", response.getMarketStatus().getMarketStatusMessage());
        
        // Test additional fields
        assertEquals(48, response.getAdvances());
        assertEquals(177, response.getDeclines());
        assertEquals(9, response.getUnchanged());
        assertEquals("09-Mar-2025", response.getNavDate());
        assertEquals(9162694697.04, response.getTotalTradedValue());
        assertEquals(82587192, response.getTotalTradedVolume());
    }

    @Test
    void shouldHandleEmptyResponse() {
        // Given
        String emptyJson = "{}";

        // When
        NseETFResponse response = JsonUtils.fromJson(emptyJson, NseETFResponse.class);

        // Then
        assertNotNull(response, "Empty response should still create an object");
        assertNull(response.getData(), "Data list should be null");
        assertNull(response.getMarketStatus(), "Market status should be null");
    }

    @Test
    void shouldHandleInvalidJson() {
        // Given
        String invalidJson = "{invalid:json}";

        // When
        NseETFResponse response = JsonUtils.fromJson(invalidJson, NseETFResponse.class);

        // Then
        assertNull(response, "Invalid JSON should return null");
    }
}
