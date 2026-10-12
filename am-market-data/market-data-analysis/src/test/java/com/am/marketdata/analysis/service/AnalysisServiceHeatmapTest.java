package com.am.marketdata.analysis.service;

import com.am.common.investment.model.historical.HistoricalData;
import com.am.common.investment.model.historical.OHLCVTPoint;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.redis.cache.AnalysisRedisCache;
import com.am.marketdata.service.MarketDataService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AnalysisServiceHeatmapTest {

    @Test
    void interactiveHeatmapReadsCacheAndDatabaseOnly() {
        MarketDataService marketDataService = mock(MarketDataService.class);
        AnalysisRedisCache analysisRedisCache = mock(AnalysisRedisCache.class);
        AnalysisService service = new AnalysisService(marketDataService, analysisRedisCache);
        LocalDate today = LocalDate.now();
        HistoricalData history = HistoricalData.builder()
                .dataPoints(List.of(point(today.minusDays(1), 100.0), point(today, 105.0)))
                .build();

        when(analysisRedisCache.getIndexHeatmap("NIFTY TEST", "1D")).thenReturn(null);
        when(marketDataService.getIndexConstituents("NIFTY TEST")).thenReturn(List.of("ABC"));
        when(marketDataService.getHistoricalDataBatch(
                anyList(), any(), any(), any(), anyBoolean(),
                any(), any(), anyBoolean(), anyBoolean(), eq(false)))
                .thenReturn(Map.of("ABC", history));

        Map<String, Double> result = service.getHeatmap("NIFTY TEST", "1D", false);

        assertTrue(result.containsKey("ABC"));
        verify(marketDataService).getHistoricalDataBatch(
                eq(List.of("ABC")), any(), any(), eq(TimeFrame.DAY), eq(true),
                isNull(), isNull(), eq(false), eq(false), eq(false));
    }

    @Test
    void missingOrZeroEndPriceIsOmittedAndNeverCachedAsMinusHundred() {
        MarketDataService marketDataService = mock(MarketDataService.class);
        AnalysisRedisCache analysisRedisCache = mock(AnalysisRedisCache.class);
        AnalysisService service = new AnalysisService(marketDataService, analysisRedisCache);
        LocalDate today = LocalDate.now();
        HistoricalData history = HistoricalData.builder()
                .dataPoints(List.of(point(today.minusDays(1), 100.0), point(today, 0.0)))
                .build();

        when(analysisRedisCache.getIndexHeatmap("NIFTY TEST", "1D")).thenReturn(null);
        when(marketDataService.getIndexConstituents("NIFTY TEST")).thenReturn(List.of("ABC"));
        when(marketDataService.getHistoricalDataBatch(anyList(), any(), any(), any(), anyBoolean(),
                any(), any(), anyBoolean(), anyBoolean(), anyBoolean()))
                .thenReturn(Map.of("ABC", history));

        Map<String, Double> result = service.getHeatmap("NIFTY TEST", "1D", false);

        assertFalse(result.containsKey("ABC"));
        verify(analysisRedisCache, never()).saveIndexHeatmap(eq("NIFTY TEST"), eq("1D"), anyMap());
    }

    @Test
    void cachesHeatmapWhenOneMemberHasNoHistoryButCoverageIsAboveThreshold() {
        MarketDataService marketDataService = mock(MarketDataService.class);
        AnalysisRedisCache analysisRedisCache = mock(AnalysisRedisCache.class);
        AnalysisService service = new AnalysisService(marketDataService, analysisRedisCache);
        LocalDate today = LocalDate.now();
        List<String> roster = List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J");
        Map<String, HistoricalData> history = new LinkedHashMap<>();
        for (String symbol : roster.subList(0, 9)) {
            history.put(symbol, HistoricalData.builder()
                    .dataPoints(List.of(point(today.minusDays(1), 100.0), point(today, 101.0)))
                    .build());
        }
        when(analysisRedisCache.getIndexHeatmap("NIFTY IT", "1D")).thenReturn(null);
        when(marketDataService.getIndexConstituents("NIFTY IT")).thenReturn(roster);
        when(marketDataService.getHistoricalDataBatch(anyList(), any(), any(), any(), anyBoolean(),
                any(), any(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(history);

        Map<String, Double> result = service.getHeatmap("NIFTY IT", "1D", true);

        assertEquals(9, result.size());
        verify(analysisRedisCache).saveIndexHeatmap(eq("NIFTY IT"), eq("1D"), anyMap());
        verify(analysisRedisCache, never()).deleteIndexHeatmap("NIFTY IT", "1D");
    }

    @Test
    void doesNotCacheHeatmapFromAnUndersizedRosterOrDeleteAValidatedCache() {
        MarketDataService marketDataService = mock(MarketDataService.class);
        AnalysisRedisCache analysisRedisCache = mock(AnalysisRedisCache.class);
        AnalysisService service = new AnalysisService(marketDataService, analysisRedisCache);
        LocalDate today = LocalDate.now();
        List<String> roster = List.of("A", "B", "C", "D", "E", "F", "G");
        Map<String, HistoricalData> history = new LinkedHashMap<>();
        for (String symbol : roster) {
            history.put(symbol, HistoricalData.builder()
                    .dataPoints(List.of(point(today.minusDays(1), 100.0), point(today, 101.0)))
                    .build());
        }
        when(marketDataService.getIndexConstituents("NIFTY IT")).thenReturn(roster);
        when(marketDataService.getHistoricalDataBatch(anyList(), any(), any(), any(), anyBoolean(),
                any(), any(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(history);

        Map<String, Double> result = service.getHeatmap("NIFTY IT", "1D", true);

        assertEquals(7, result.size());
        verify(analysisRedisCache, never()).saveIndexHeatmap(eq("NIFTY IT"), eq("1D"), anyMap());
        verify(analysisRedisCache, never()).deleteIndexHeatmap("NIFTY IT", "1D");
    }

    private static OHLCVTPoint point(LocalDate date, double close) {
        return OHLCVTPoint.builder()
                .time(LocalDateTime.of(date, LocalTime.of(15, 30)))
                .open(close)
                .high(close)
                .low(close)
                .close(close)
                .build();
    }
}
