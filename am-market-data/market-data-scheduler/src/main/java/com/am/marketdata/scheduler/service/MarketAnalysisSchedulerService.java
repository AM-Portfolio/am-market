package com.am.marketdata.scheduler.service;

import com.am.marketdata.analysis.service.AnalysisService;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.service.SymbolOrchestratorService;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketAnalysisSchedulerService {

    private final SymbolOrchestratorService symbolOrchestratorService;
    private final AnalysisService analysisService;

    @Value("${market.analysis.heatmap.warming.enabled}")
    private boolean indexHeatmapWarmingEnabled;

    @Value("${market.analysis.heatmap.warming.minimum-constituents}")
    private int minimumIndexConstituents;

    @Value("${market.analysis.heatmap.timeframes}")
    private String configuredHeatmapTimeframes;

    // Executor for parallel batch processing if needed
    // Using a simple loop is safer for database limits, but parallel can be faster.
    // Given "thousands of stocks", sequential batches is probably fine for a
    // nightly job.

    public void executeDailyAnalysis() {
        log.info("Starting Daily Market Analysis Job...");
        long startTime = System.currentTimeMillis();

        try {
            // Run the requested large-index warm first so it is not delayed by
            // the existing per-symbol analysis workload below.
            warmConfiguredIndexHeatmaps();

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
        for (String tf : getHeatmapTimeframes()) {
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

        // Explicitly refresh the Market Heatmap (Indices) board — not covered by
        // per-stock symbol iteration above.
        for (String tf : getHeatmapTimeframes()) {
            try {
                analysisService.getHeatmap("INDICES", tf, true);
                log.info("Refreshed INDICES heatmap for {}", tf);
            } catch (Exception e) {
                log.warn("Failed to refresh INDICES heatmap for {}", tf, e);
            }
        }

    }

    /**
     * Precompute heatmaps for configured NSE constituent indexes above the
     * configurable roster threshold.
     */
    void warmConfiguredIndexHeatmaps() {
        if (!indexHeatmapWarmingEnabled) {
            log.info("Configured NSE index heatmap warming is disabled");
            return;
        }

        Map<String, Integer> configuredIndexes = symbolOrchestratorService.getConfiguredNseIndexConstituentCounts();
        if (configuredIndexes.isEmpty()) {
            log.warn("Skipping configured NSE index heatmap warming because constituent counts could not be loaded");
            return;
        }

        List<String> timeframes = getHeatmapTimeframes();
        log.info("Starting configured NSE index heatmap warming indexes={} minimumConstituents={} timeframes={}",
                configuredIndexes.size(), minimumIndexConstituents, timeframes);
        for (Map.Entry<String, Integer> entry : configuredIndexes.entrySet()) {
            String index = entry.getKey();
            Integer configuredRosterSize = entry.getValue();
            if (configuredRosterSize == null) {
                log.warn("Skipping configured index heatmap warm because constituent count is null index={}", index);
                continue;
            }
            int rosterSize = configuredRosterSize;
            int effectiveMinimum = Math.max(0, minimumIndexConstituents);
            if (rosterSize <= 0 || rosterSize <= effectiveMinimum) {
                log.debug("Skipping configured index heatmap warm index={} rosterSize={} requiredGreaterThan={}",
                        index, rosterSize, effectiveMinimum);
                continue;
            }

            for (String timeframe : timeframes) {
                long startedAt = System.currentTimeMillis();
                try {
                    var values = analysisService.getHeatmap(index, timeframe, true);
                    log.info("Configured index heatmap warm completed index={} timeframe={} rosterSize={} resolved={} durationMs={}",
                            index, timeframe, rosterSize, values == null ? 0 : values.size(),
                            System.currentTimeMillis() - startedAt);
                } catch (Exception e) {
                    log.warn("Configured index heatmap warm failed index={} timeframe={} rosterSize={} durationMs={}",
                            index, timeframe, rosterSize, System.currentTimeMillis() - startedAt, e);
                }
            }
        }
    }

    private List<String> getHeatmapTimeframes() {
        if (configuredHeatmapTimeframes == null || configuredHeatmapTimeframes.isBlank()) {
            return List.of();
        }
        return Arrays.stream(configuredHeatmapTimeframes.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }
}
