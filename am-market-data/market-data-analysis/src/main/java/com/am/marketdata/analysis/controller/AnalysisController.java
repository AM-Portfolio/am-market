package com.am.marketdata.analysis.controller;

import com.am.marketdata.analysis.service.AnalysisService;
import com.am.marketdata.analysis.service.MarketAnalyticsService;
import com.am.marketdata.api.model.HistoricalDataResponseV1;
import com.am.marketdata.common.observability.FlowLogger;
import com.am.marketdata.common.observability.FlowSpan;
import lombok.extern.slf4j.Slf4j;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.common.model.analysis.CalendarHeatmapResponse;
import com.am.marketdata.common.model.analysis.SeasonalityResponse;
import com.am.marketdata.common.model.analysis.TechnicalAnalysisResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/analysis")
@RequiredArgsConstructor
@Tag(name = "Market Analysis", description = "Unified API for all market analysis including Seasonality, Technicals, Heatmaps, Performance, and Charts")
@CrossOrigin(originPatterns = "*")
public class AnalysisController {

    private final FlowLogger flowLogger;
    private final AnalysisService analysisService;
    private final MarketAnalyticsService marketAnalyticsService;

    // --- Original Analysis Endpoints ---

    @GetMapping("/seasonality")
    public ResponseEntity<SeasonalityResponse> getSeasonality(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "DAY") String timeframe) {

        try (FlowSpan span = flowLogger.start("analysis.seasonality", "symbol", symbol, "timeframe", timeframe)) {
            try {
                TimeFrame tf = parseTimeFrame(timeframe);
                SeasonalityResponse response = analysisService.getSeasonalityAnalysis(symbol, tf);
                flowLogger.complete(span);
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("Error in seasonality analysis for {}", symbol, e);
                flowLogger.fail(span, e);
                throw e;
            }
        }
    }

    @GetMapping("/technical")
    public ResponseEntity<TechnicalAnalysisResponse> getTechnicalAnalysis(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "DAY") String timeframe) {

        try (FlowSpan span = flowLogger.start("analysis.technical", "symbol", symbol, "timeframe", timeframe)) {
            try {
                TimeFrame tf = parseTimeFrame(timeframe);
                TechnicalAnalysisResponse response = analysisService.getTechnicalAnalysis(symbol, tf);
                flowLogger.complete(span);
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("Error in technical analysis for {}", symbol, e);
                flowLogger.fail(span, e);
                throw e;
            }
        }
    }

    @GetMapping("/heatmap/calendar")
    public ResponseEntity<CalendarHeatmapResponse> getCalendarHeatmap(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "-1") int year) {

        try (FlowSpan span = flowLogger.start("analysis.heatmap.calendar", "symbol", symbol, "year", year)) {
            try {
                if (year == -1)
                    year = java.time.LocalDate.now().getYear();
                CalendarHeatmapResponse response = analysisService.getCalendarHeatmap(symbol, year);
                flowLogger.complete(span);
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("Error in calendar heatmap for {}", symbol, e);
                flowLogger.fail(span, e);
                throw e;
            }
        }
    }

    @GetMapping("/seasonality/batch")
    public ResponseEntity<java.util.Map<String, SeasonalityResponse>> getSeasonalityBatch(
            @RequestParam java.util.List<String> symbols,
            @RequestParam(defaultValue = "DAY") String timeframe) {

        TimeFrame tf = parseTimeFrame(timeframe);
        return ResponseEntity.ok(analysisService.getSeasonalityAnalysisBatch(symbols, tf));
    }

    @GetMapping("/technical/batch")
    public ResponseEntity<java.util.Map<String, TechnicalAnalysisResponse>> getTechnicalBatch(
            @RequestParam java.util.List<String> symbols,
            @RequestParam(defaultValue = "DAY") String timeframe) {

        TimeFrame tf = parseTimeFrame(timeframe);
        return ResponseEntity.ok(analysisService.getTechnicalAnalysisBatch(symbols, tf));
    }

    @GetMapping("/performance/monthly")
    public ResponseEntity<com.am.marketdata.common.model.analysis.HistoricalPerformanceResponse> getHistoricalPerformance(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "10") int years,
            @RequestParam(defaultValue = "false") boolean detailed) {

        return ResponseEntity.ok(analysisService.getHistoricalPerformance(symbol, years, detailed));
    }

    @GetMapping("/heatmap")
    public ResponseEntity<java.util.Map<String, Double>> getHeatmap(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "1D") String timeframe) {

        return ResponseEntity.ok(analysisService.getHeatmap(symbol, timeframe));
    }

    @GetMapping("/indices/historical-performance")
    public ResponseEntity<com.am.marketdata.common.model.analysis.IndicesHistoricalPerformanceResponse> getIndicesHistoricalPerformance(
            @RequestParam(defaultValue = "10") int years) {

        return ResponseEntity.ok(analysisService.getIndicesHistoricalPerformance(years));
    }

    // --- Migrated Market Analytics Endpoints ---

    /**
     * Get Top Gainers or Losers
     */
    @GetMapping(value = "/movers", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get Top Gainers/Losers", description = "Retrieves top performing or worst performing stocks from the specified market index")
    public ResponseEntity<?> getMovers(
            @RequestParam(defaultValue = "gainers") String type,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String indexSymbol,
            @RequestParam(required = false) String timeFrame,
            @RequestParam(defaultValue = "false") boolean expandIndices) {

        String index = indexSymbol != null ? indexSymbol : "NIFTY 50";
        try (FlowSpan span = flowLogger.start("analysis.movers", "type", type, "index", index, "timeFrame",
                timeFrame)) {
            try {
                TimeFrame tf = timeFrame != null ? TimeFrame.fromApiValue(timeFrame) : null;

                Object result;
                if ("all".equalsIgnoreCase(type)) {
                    result = marketAnalyticsService.getMoversUnified(limit, index, tf, expandIndices);
                } else {
                    result = marketAnalyticsService.getMovers(limit, type, index, tf, expandIndices);
                }
                flowLogger.complete(span);
                return ResponseEntity.ok(result);
            } catch (Exception e) {
                log.error("Error fetching movers index={} type={}", index, type, e);
                flowLogger.fail(span, e);
                throw e;
            }
        }
    }

    /**
     * Get Sector Performance
     */
    @GetMapping(value = "/sectors", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get Sector Performance", description = "Aggregates market performance by sector (Industry) from the specified index")
    public ResponseEntity<List<Map<String, Object>>> getSectorPerformance(
            @RequestParam(required = false) String indexSymbol,
            @RequestParam(required = false) String timeFrame,
            @RequestParam(defaultValue = "false") boolean expandIndices) {

        String index = indexSymbol != null ? indexSymbol : "NIFTY 50";
        TimeFrame tf = timeFrame != null ? TimeFrame.fromApiValue(timeFrame) : null;

        return ResponseEntity.ok(marketAnalyticsService.getSectorPerformance(index, tf, expandIndices));
    }

    /**
     * Get Index Performance (All Constituents)
     */
    @GetMapping(value = "/index-performance", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get Index Performance", description = "Retrieves performance of all constituent stocks in the specified index for a given timeframe")
    public ResponseEntity<List<Map<String, Object>>> getIndexPerformance(
            @RequestParam(required = false) String indexSymbol,
            @RequestParam(required = false) String timeFrame) {

        String index = indexSymbol != null ? indexSymbol : "NIFTY 50";
        TimeFrame tf = timeFrame != null ? TimeFrame.fromApiValue(timeFrame) : null;

        return ResponseEntity.ok(marketAnalyticsService.getIndexPerformance(index, tf));
    }

    /**
     * Get Historical Charts (Batch or Single)
     */
    @GetMapping(value = "/historical-charts", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get historical charts data", description = "Retrieves historical data for charts with various time frames (10m, 1H, 1D, 1W, 1M, 5Y, etc.)")
    public ResponseEntity<HistoricalDataResponseV1> getHistoricalCharts(
            @RequestParam String symbols,
            @RequestParam(defaultValue = "1D") String range,
            @RequestParam(defaultValue = "true") boolean isIndexSymbol) {

        try (FlowSpan span = flowLogger.start("analysis.historical.charts", "symbols", symbols, "range", range,
                "isIndex", isIndexSymbol)) {
            try {
                HistoricalDataResponseV1 response = marketAnalyticsService.getHistoricalCharts(symbols, range,
                        isIndexSymbol);
                flowLogger.complete(span, "resultCount", response.getData() != null ? response.getData().size() : 0);
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("Error fetching historical charts for {}", symbols, e);
                flowLogger.fail(span, e);
                throw e;
            }
        }
    }

    /**
     * Get Historical Charts (Single Symbol - Legacy Support)
     */
    @GetMapping(value = "/historical-charts/{symbol}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get historical charts data (Legacy path)", description = "Retrieves historical data for charts with various time frames")
    public ResponseEntity<HistoricalDataResponseV1> getHistoricalChartsLegacy(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1D") String range,
            @RequestParam(defaultValue = "true") boolean isIndexSymbol) {
        return getHistoricalCharts(symbol, range, isIndexSymbol);
    }

    private TimeFrame parseTimeFrame(String tf) {
        try {
            return TimeFrame.valueOf(tf.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TimeFrame.DAY;
        }
    }
}
