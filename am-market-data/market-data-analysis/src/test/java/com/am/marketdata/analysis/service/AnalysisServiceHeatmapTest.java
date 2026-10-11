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
