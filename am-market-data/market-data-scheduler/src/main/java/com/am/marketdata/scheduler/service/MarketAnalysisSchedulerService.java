package com.am.marketdata.scheduler.service;

import com.am.marketdata.analysis.service.AnalysisService;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.service.SymbolOrchestratorService;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketAnalysisSchedulerService {

    private final SymbolOrchestratorService symbolOrchestratorService;
    private final AnalysisService analysisService;

    // Executor for parallel batch processing if needed
    // Using a simple loop is safer for database limits, but parallel can be faster.
    // Given "thousands of stocks", sequential batches is probably fine for a
    // nightly job.

    public void executeDailyAnalysis() {
        log.info("Starting Daily Market Analysis Job...");
        long startTime = System.currentTimeMillis();

        try {
            // 1. Fetch all distinct symbols
            List<String> allSymbols = new ArrayList<>(symbolOrchestratorService.findDistinctSymbols());
            log.info("Found {} symbols for analysis.", allSymbols.size());

            // 2. Process in Batches
            List<List<String>> batches = Lists.partition(allSymbols, 10);

            for (List<String> batch : batches) {
                processBatch(batch);
            }

            // 3. Process Aggregate Indices Performance
            processIndicesPerformance();

        } catch (Exception e) {
            log.error("Error during Daily Market Analysis Job", e);
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Daily Market Analysis Job completed in {} ms", duration);
    }

    private void processBatch(List<String> batch) {
        log.info("Processing batch of {} symbols: {}", batch.size(), batch.get(0) + "...");

        try {
            // A. Batch Operations (Supported by Service)
            // Seasonality (Daily)
            try {
                analysisService.getSeasonalityAnalysisBatch(batch, TimeFrame.DAY);
            } catch (Exception e) {
                log.error("Error processing Seasonality Batch", e);
            }

            // Technical Analysis (Daily)
            try {
                analysisService.getTechnicalAnalysisBatch(batch, TimeFrame.DAY);
            } catch (Exception e) {
                log.error("Error processing Technical Analysis Batch", e);
            }

            // B. Individual Operations (Iterate)
            for (String symbol : batch) {
                processIndividualSymbol(symbol);
            }

        } catch (Exception e) {
            log.error("Critical error processing batch starting with {}", batch.get(0), e);
        }
    }

    private void processIndividualSymbol(String symbol) {
        // 1. Heatmap (Various Timeframes)
        List<String> timeframes = Arrays.asList("1D", "1W", "1M", "3M", "6M", "1Y", "3Y", "5Y");
        for (String tf : timeframes) {
            try {
                // Force Refresh (bypass cache = true)
                analysisService.getHeatmap(symbol, tf, true);
            } catch (Exception e) {
                log.warn("Failed to calculate heatmap for {} {}", symbol, tf, e);
            }
        }

        // 2. Historical Performance (10 Years)
        try {
            // Detailed = true for rich UI support, bypassCache = true
            analysisService.getHistoricalPerformance(symbol, 10, true, true);
        } catch (Exception e) {
            log.warn("Failed to calculate historical performance for {}", symbol, e);
        }
    }

    private void processIndicesPerformance() {
        log.info("Processing aggregated Indices Historical Performance...");
        try {
            // bypassCache = true
            analysisService.getIndicesHistoricalPerformance(10, true);
        } catch (Exception e) {
            log.error("Failed to calculate Indices Historical Performance", e);
        }
    }
}
