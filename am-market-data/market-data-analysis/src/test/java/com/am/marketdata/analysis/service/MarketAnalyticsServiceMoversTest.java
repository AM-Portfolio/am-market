package com.am.marketdata.analysis.service;

import com.am.common.investment.model.stockindice.StockData;
import com.am.common.investment.model.stockindice.StockIndicesMarketData;
import com.am.marketdata.analysis.dto.StockMoverDTO;
import com.am.marketdata.analysis.dto.TopMoversResponse;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.redis.cache.AnalysisRedisCache;
import com.am.marketdata.service.MarketDataService;
import com.am.common.investment.model.historical.HistoricalData;
import com.am.common.investment.model.historical.OHLCVTPoint;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void longTimeframeHeatmapUsesVerifiedBoundaryWindowsAndRejectsFutureCandles() {
        MarketDataService marketDataService = mock(MarketDataService.class);
        AnalysisRedisCache cache = mock(AnalysisRedisCache.class);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        LocalDate boundary = today.minusMonths(1);
        when(cache.getIndexHeatmap("NIFTY 50", "1M")).thenReturn(null);
        when(marketDataService.getIndexConstituents("NIFTY 50")).thenReturn(List.of("TCS"));
        when(marketDataService.getHistoricalDataBatch(
                eq(List.of("TCS")), any(Date.class), any(Date.class), eq(TimeFrame.DAY),
                eq(true), isNull(), isNull(), eq(false), eq(false), eq(false)))
                .thenReturn(Map.of("TCS", history(
                                candle(boundary.minusDays(3), 100.0), candle(boundary.plusDays(1), 200.0))),
                        Map.of("TCS", history(
                                candle(today.minusDays(2), 120.0), candle(today.plusDays(1), 900.0))));

        Map<String, Double> result = new AnalysisService(marketDataService, cache)
                .getHeatmap("NIFTY 50", "1M");

        assertEquals(20.0, result.get("TCS"), 0.001);
        verify(marketDataService, times(2)).getHistoricalDataBatch(
                eq(List.of("TCS")), any(Date.class), any(Date.class), eq(TimeFrame.DAY),
                eq(true), isNull(), isNull(), eq(false), eq(false), eq(false));
    }

    @Test
    void overlappingOneWeekHeatmapWindowsUseOneHistoryBatch() {
        MarketDataService marketDataService = mock(MarketDataService.class);
        AnalysisRedisCache cache = mock(AnalysisRedisCache.class);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        when(cache.getIndexHeatmap("NIFTY 50", "1W")).thenReturn(null);
        when(marketDataService.getIndexConstituents("NIFTY 50")).thenReturn(List.of("TCS"));
        when(marketDataService.getHistoricalDataBatch(
                eq(List.of("TCS")), any(Date.class), any(Date.class), eq(TimeFrame.DAY),
                eq(true), isNull(), isNull(), eq(false), eq(false), eq(false)))
                .thenReturn(Map.of("TCS", history(
                        candle(today.minusDays(10), 100.0), candle(today.minusDays(1), 110.0))));

        Map<String, Double> result = new AnalysisService(marketDataService, cache)
                .getHeatmap("NIFTY 50", "1W");

        assertEquals(10.0, result.get("TCS"), 0.001);
        verify(marketDataService, times(1)).getHistoricalDataBatch(
                eq(List.of("TCS")), any(Date.class), any(Date.class), eq(TimeFrame.DAY),
                eq(true), isNull(), isNull(), eq(false), eq(false), eq(false));
    }

    @Test
    void heatmapOmitsSymbolWhenEndingWindowContainsOnlyFutureCandles() {
        MarketDataService marketDataService = mock(MarketDataService.class);
        AnalysisRedisCache cache = mock(AnalysisRedisCache.class);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        LocalDate boundary = today.minusMonths(1);
        when(cache.getIndexHeatmap("NIFTY 50", "1M")).thenReturn(null);
        when(marketDataService.getIndexConstituents("NIFTY 50")).thenReturn(List.of("TCS"));
        when(marketDataService.getHistoricalDataBatch(
                eq(List.of("TCS")), any(Date.class), any(Date.class), eq(TimeFrame.DAY),
                eq(true), isNull(), isNull(), eq(false), eq(false), eq(false)))
                .thenReturn(Map.of("TCS", history(candle(boundary.minusDays(2), 100.0))),
                        Map.of("TCS", history(candle(today.plusDays(1), 150.0))));

        Map<String, Double> result = new AnalysisService(marketDataService, cache)
                .getHeatmap("NIFTY 50", "1M");

        assertFalse(result.containsKey("TCS"));
    }

    private static HistoricalData history(OHLCVTPoint... points) {
        return HistoricalData.builder().dataPoints(List.of(points)).build();
    }

    private static OHLCVTPoint candle(LocalDate date, double close) {
        return OHLCVTPoint.builder()
                .time(LocalDateTime.of(date, LocalTime.of(15, 30)))
                .open(close).high(close).low(close).close(close).volume(1L)
                .build();
    }
}
