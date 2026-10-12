package com.am.marketdata.api.service;

import com.am.common.investment.model.events.StockInsidicesEventData.IndexMetadata;
import com.am.common.investment.model.stockindice.StockData;
import com.am.common.investment.model.stockindice.StockIndicesMarketData;
import com.am.common.investment.service.StockIndicesMarketDataService;
import com.am.marketdata.scraper.service.MarketDataProcessingService;
import com.am.marketdata.service.MarketDataCacheService;
import com.am.marketdata.api.service.MarketDataFetchService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

class StockIndicesServiceMembershipTest {

    @Test
    void acceptsIndiaVixWithoutStockMembers() {
        StockIndicesService service = service();
        StockIndicesMarketData vix = StockIndicesMarketData.builder()
                .indexSymbol("INDIA VIX")
                .metadata(new IndexMetadata())
                .data(List.of())
                .build();

        assertTrue(invokeUsable(service, vix));
    }

    @Test
    void removesRepeatedMembersWhenIdentityMatches() {
        StockIndicesService service = service();
        StockData first = StockData.builder().symbol("TCS").isin("INE467B01029").build();
        StockData duplicate = StockData.builder().symbol(" tcs ").isin("INE467B01029").build();
        StockIndicesMarketData index = StockIndicesMarketData.builder()
                .indexSymbol("NIFTY IT")
                .data(List.of(first, duplicate,
                        StockData.builder().symbol("INFY").build(),
                        StockData.builder().symbol("WIPRO").build(),
                        StockData.builder().symbol("HCLTECH").build(),
                        StockData.builder().symbol("TECHM").build(),
                        StockData.builder().symbol("LTM").build(),
                        StockData.builder().symbol("MPHASIS").build(),
                        StockData.builder().symbol("COFORGE").build(),
                        StockData.builder().symbol("OFSS").build()))
                .build();

        assertTrue(invokeUsable(service, index));
        assertEquals(9, index.getData().size());
    }

    @Test
    void rejectsDuplicateTickerWithConflictingIsins() {
        StockIndicesService service = service();
        StockIndicesMarketData index = StockIndicesMarketData.builder()
                .indexSymbol("NIFTY IT")
                .data(List.of(
                        StockData.builder().symbol("TCS").isin("INE467B01029").build(),
                        StockData.builder().symbol("TCS").isin("INE467B01999").build()))
                .build();

        assertFalse(invokeUsable(service, index));
    }

    private static StockIndicesService service() {
        return new StockIndicesService(
                mock(MarketDataProcessingService.class),
                mock(StockIndicesMarketDataService.class),
                mock(MarketDataFetchService.class),
                mock(MarketDataCacheService.class));
    }

    private static boolean invokeUsable(StockIndicesService service, StockIndicesMarketData index) {
        Boolean result = ReflectionTestUtils.invokeMethod(service, "hasUsableMembership", index);
        return Boolean.TRUE.equals(result);
    }
}
