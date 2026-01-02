package com.am.common.investment.app.util;

import com.am.common.investment.model.equity.EquityPrice;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestAssertionUtil {
    
    public static void assertEquityPrice(Optional<EquityPrice> actual, EquityPrice expected) {
        assertThat(actual).isPresent();
        EquityPrice price = actual.get();
        
        assertThat(price.getSymbol()).isEqualTo(expected.getSymbol());
        assertThat(price.getIsin()).isEqualTo(expected.getIsin());
        assertThat(price.getOhlcv().getOpen()).isEqualTo(expected.getOhlcv().getOpen());
        assertThat(price.getOhlcv().getHigh()).isEqualTo(expected.getOhlcv().getHigh());
        assertThat(price.getOhlcv().getLow()).isEqualTo(expected.getOhlcv().getLow());
        assertThat(price.getOhlcv().getClose()).isEqualTo(expected.getOhlcv().getClose());
        assertThat(price.getOhlcv().getVolume()).isEqualTo(expected.getOhlcv().getVolume());
        assertThat(price.getExchange()).isEqualTo(expected.getExchange());
        assertThat(price.getCurrency()).isEqualTo(expected.getCurrency());
        assertThat(price.getTime()).isEqualTo(expected.getTime());
    }

    public static void assertEquityPriceList(List<EquityPrice> actual, List<EquityPrice> expected) {
        assertEquals("List sizes should match", expected.size(), actual.size());
        
        for (int i = 0; i < expected.size(); i++) {
            EquityPrice expectedPrice = expected.get(i);
            EquityPrice actualPrice = actual.get(i);
            
            assertEquals("Symbol should match", expectedPrice.getSymbol(), actualPrice.getSymbol());
            assertEquals("ISIN should match", expectedPrice.getIsin(), actualPrice.getIsin());
            assertEquals("Open price should match", expectedPrice.getOhlcv().getOpen(), actualPrice.getOhlcv().getOpen(), 0.001);
            assertEquals("High price should match", expectedPrice.getOhlcv().getHigh(), actualPrice.getOhlcv().getHigh(), 0.001);
            assertEquals("Low price should match", expectedPrice.getOhlcv().getLow(), actualPrice.getOhlcv().getLow(), 0.001);
            assertEquals("Close price should match", expectedPrice.getOhlcv().getClose(), actualPrice.getOhlcv().getClose(), 0.001);
            assertEquals("Volume should match", expectedPrice.getOhlcv().getVolume(), actualPrice.getOhlcv().getVolume());
            assertEquals("Exchange should match", expectedPrice.getExchange(), actualPrice.getExchange());
            assertEquals("Currency should match", expectedPrice.getCurrency(), actualPrice.getCurrency());
            assertNotNull("Time should not be null", actualPrice.getTime());
        }
    }

    public static void assertEquityPriceHistory(List<EquityPrice> actual, List<EquityPrice> expected) {
        assertThat(actual).hasSize(expected.size());
        
        for (int i = 0; i < actual.size(); i++) {
            EquityPrice actualPrice = actual.get(i);
            EquityPrice expectedPrice = expected.get(i);
            
            assertThat(actualPrice.getSymbol()).isEqualTo(expectedPrice.getSymbol());
            assertThat(actualPrice.getOhlcv().getClose()).isEqualTo(expectedPrice.getOhlcv().getClose());
            assertThat(actualPrice.getTime()).isEqualTo(expectedPrice.getTime());
        }
    }

    public static void assertEquityPriceCloseValues(List<EquityPrice> prices, Double... expectedCloseValues) {
        assertThat(prices).hasSize(expectedCloseValues.length);
        assertThat(prices).extracting("close").containsExactly(expectedCloseValues);
    }
}
