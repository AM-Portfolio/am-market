package com.am.marketdata.analysis.service;

import com.am.common.investment.model.stockindice.StockData;
import com.am.common.investment.model.stockindice.StockIndicesMarketData;
import com.am.marketdata.analysis.dto.StockMoverDTO;
import com.am.marketdata.analysis.dto.TopMoversResponse;
import com.am.marketdata.analysis.util.StockDataEnricher;
import com.am.marketdata.analysis.util.StockDataEnricher.EnrichedStockData;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.service.SecurityService;
import com.am.marketdata.service.SmartStockService;
import com.am.marketdata.api.service.MarketDataFetchService;
import com.am.marketdata.api.service.StockIndicesService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MarketAnalyticsServiceMoversTest {

    @Test
    void unchangedStocksAreNotReturnedAsBothGainersAndLosers() {
        StockIndicesService indicesService = mock(StockIndicesService.class);
        SecurityService securityService = mock(SecurityService.class);
        MarketDataFetchService marketDataFetchService = mock(MarketDataFetchService.class);
        StockIndicesMarketData index = mock(StockIndicesMarketData.class);
        List<StockData> members = new ArrayList<>();
        List<EnrichedStockData> prices = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            String symbol = "STOCK" + i;
            StockData member = mock(StockData.class);
            when(member.getSymbol()).thenReturn(symbol);
            members.add(member);

            double lastPrice = i == 0 ? 110.0 : (i == 1 ? 90.0 : 100.0);
            OHLCQuote quote = OHLCQuote.builder().lastPrice(lastPrice).previousClose(100.0).build();
            prices.add(new EnrichedStockData(member, quote));
        }

        when(indicesService.getLatestIndexData("NIFTY 50")).thenReturn(index);
        when(index.getData()).thenReturn(members);

        StockDataEnricher enricher = spy(new StockDataEnricher(mock(SmartStockService.class)));
        doReturn(prices).when(enricher).enrichWithPrices(members, TimeFrame.DAY, false);
        MarketAnalyticsService service = new MarketAnalyticsService(
                indicesService, securityService, enricher, marketDataFetchService);

        TopMoversResponse result = service.getMoversUnified(10, "NIFTY 50", TimeFrame.DAY, false);

        assertEquals(List.of("STOCK0"), result.getGainers().stream().map(StockMoverDTO::getSymbol).toList());
        assertEquals(List.of("STOCK1"), result.getLosers().stream().map(StockMoverDTO::getSymbol).toList());
        assertEquals(10.0, result.getGainers().get(0).getPercentChange(), 0.000001);
        assertEquals(-10.0, result.getLosers().get(0).getPChange(), 0.000001);

        List<StockMoverDTO> gainersOnly = service.getMovers(10, "gainers", "NIFTY 50", TimeFrame.DAY, false);
        assertEquals(List.of("STOCK0"), gainersOnly.stream().map(StockMoverDTO::getSymbol).toList());

        List<StockMoverDTO> losersOnly = service.getMovers(10, "losers", "NIFTY 50", TimeFrame.DAY, false);
        assertEquals(List.of("STOCK1"), losersOnly.stream().map(StockMoverDTO::getSymbol).toList());
    }

    @Test
    void allowsSufficientCoverageAboveMinimumMembers() {
        StockIndicesService indicesService = mock(StockIndicesService.class);
        SecurityService securityService = mock(SecurityService.class);
        MarketDataFetchService marketDataFetchService = mock(MarketDataFetchService.class);
        StockIndicesMarketData index = mock(StockIndicesMarketData.class);
        List<StockData> members = new ArrayList<>();
        List<EnrichedStockData> prices = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            String symbol = "STOCK" + i;
            StockData member = mock(StockData.class);
            when(member.getSymbol()).thenReturn(symbol);
            members.add(member);

            if (i < 48) { // 48 out of 50 stocks (minimum is 45)
                double lastPrice = i == 0 ? 110.0 : (i == 1 ? 90.0 : 100.0);
                OHLCQuote quote = OHLCQuote.builder().lastPrice(lastPrice).previousClose(100.0).build();
                prices.add(new EnrichedStockData(member, quote));
            }
        }

        when(indicesService.getLatestIndexData("NIFTY 50")).thenReturn(index);
        when(index.getData()).thenReturn(members);

        StockDataEnricher enricher = spy(new StockDataEnricher(mock(SmartStockService.class)));
        doReturn(prices).when(enricher).enrichWithPrices(members, TimeFrame.DAY, false);
        MarketAnalyticsService service = new MarketAnalyticsService(
                indicesService, securityService, enricher, marketDataFetchService);

        TopMoversResponse result = service.getMoversUnified(10, "NIFTY 50", TimeFrame.DAY, false);
        assertEquals(List.of("STOCK0"), result.getGainers().stream().map(StockMoverDTO::getSymbol).toList());
        assertEquals(List.of("STOCK1"), result.getLosers().stream().map(StockMoverDTO::getSymbol).toList());
    }

    @Test
    void rejectsCoverageBelowMinimumMembers() {
        StockIndicesService indicesService = mock(StockIndicesService.class);
        SecurityService securityService = mock(SecurityService.class);
        MarketDataFetchService marketDataFetchService = mock(MarketDataFetchService.class);
        StockIndicesMarketData index = mock(StockIndicesMarketData.class);
        List<StockData> members = new ArrayList<>();
        List<EnrichedStockData> prices = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            String symbol = "STOCK" + i;
            StockData member = mock(StockData.class);
            when(member.getSymbol()).thenReturn(symbol);
            members.add(member);

            if (i < 30) { // 30 out of 50 stocks (minimum is 45)
                double lastPrice = i == 0 ? 110.0 : (i == 1 ? 90.0 : 100.0);
                OHLCQuote quote = OHLCQuote.builder().lastPrice(lastPrice).previousClose(100.0).build();
                prices.add(new EnrichedStockData(member, quote));
            }
        }

        when(indicesService.getLatestIndexData("NIFTY 50")).thenReturn(index);
        when(index.getData()).thenReturn(members);

        StockDataEnricher enricher = spy(new StockDataEnricher(mock(SmartStockService.class)));
        doReturn(prices).when(enricher).enrichWithPrices(members, TimeFrame.DAY, false);
        MarketAnalyticsService service = new MarketAnalyticsService(
                indicesService, securityService, enricher, marketDataFetchService);

        TopMoversResponse result = service.getMoversUnified(10, "NIFTY 50", TimeFrame.DAY, false);
        assertEquals(List.of(), result.getGainers());
        assertEquals(List.of(), result.getLosers());
    }
}
