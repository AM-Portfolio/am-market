package com.am.marketdata.scheduler.service;

import com.am.marketdata.analysis.service.AnalysisService;
import com.am.marketdata.service.SymbolOrchestratorService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MarketAnalysisSchedulerLargeIndexHeatmapTest {

    @Test
    void warmsConfiguredIndexesForEveryConfiguredHeatmapTimeframe() {
        SymbolOrchestratorService symbols = mock(SymbolOrchestratorService.class);
        AnalysisService analysis = mock(AnalysisService.class);
        when(symbols.getConfiguredNseIndexConstituentCounts()).thenReturn(Map.of(
                "NIFTY 500", 501,
                "NIFTY MIDCAP 150", 150,
                "NIFTY 50", 50));
        MarketAnalysisSchedulerService scheduler = new MarketAnalysisSchedulerService(symbols, analysis);
        configure(scheduler, true, 100, "1D,5D,1W,1M,3M,6M,1Y,3Y,5Y");

        scheduler.warmConfiguredIndexHeatmaps();

        verify(symbols).getConfiguredNseIndexConstituentCounts();
        for (String index : List.of("NIFTY 500", "NIFTY MIDCAP 150")) {
            for (String timeframe : List.of("1D", "5D", "1W", "1M", "3M", "6M", "1Y", "3Y", "5Y")) {
                verify(analysis).getHeatmap(index, timeframe, true);
            }
        }
        verify(analysis, never()).getHeatmap(eq("NIFTY 50"), anyString(), anyBoolean());
    }

    @Test
    void doesNotWarmAnIndexThatDoesNotExceedTheRosterThreshold() {
        SymbolOrchestratorService symbols = mock(SymbolOrchestratorService.class);
        AnalysisService analysis = mock(AnalysisService.class);
        when(symbols.getConfiguredNseIndexConstituentCounts()).thenReturn(Map.of("NIFTY 50", 100));
        MarketAnalysisSchedulerService scheduler = new MarketAnalysisSchedulerService(symbols, analysis);
        configure(scheduler, true, 100, "1D,5D,1W,1M,3M,6M,1Y,3Y,5Y");

        scheduler.warmConfiguredIndexHeatmaps();

        verifyNoInteractions(analysis);
    }

    @Test
    void continuesWarmingAfterOneTimeframeFails() {
        SymbolOrchestratorService symbols = mock(SymbolOrchestratorService.class);
        AnalysisService analysis = mock(AnalysisService.class);
        when(symbols.getConfiguredNseIndexConstituentCounts()).thenReturn(Map.of("NIFTY 500", 501));
        when(analysis.getHeatmap("NIFTY 500", "1W", true))
                .thenThrow(new IllegalStateException("test failure"));
        MarketAnalysisSchedulerService scheduler = new MarketAnalysisSchedulerService(symbols, analysis);
        configure(scheduler, true, 100, "1W,5Y");

        scheduler.warmConfiguredIndexHeatmaps();

        verify(analysis).getHeatmap("NIFTY 500", "5Y", true);
    }

    @Test
    void disabledWarmingDoesNotReadMembershipOrTouchAnalysis() {
        SymbolOrchestratorService symbols = mock(SymbolOrchestratorService.class);
        AnalysisService analysis = mock(AnalysisService.class);
        MarketAnalysisSchedulerService scheduler = new MarketAnalysisSchedulerService(symbols, analysis);
        configure(scheduler, false, 0, "1D,5Y");

        scheduler.warmConfiguredIndexHeatmaps();

        verifyNoInteractions(symbols, analysis);
    }

    private static void configure(MarketAnalysisSchedulerService scheduler, boolean enabled, int minConstituents,
                                  String timeframes) {
        ReflectionTestUtils.setField(scheduler, "indexHeatmapWarmingEnabled", enabled);
        ReflectionTestUtils.setField(scheduler, "minimumIndexConstituents", minConstituents);
        ReflectionTestUtils.setField(scheduler, "configuredHeatmapTimeframes", timeframes);
    }
}
