package com.am.marketdata.analysis.controller;

import com.am.marketdata.analysis.service.AnalysisService;
import com.am.marketdata.analysis.service.MarketAnalyticsService;
import com.am.marketdata.api.model.HistoricalDataResponseV1;
import com.am.marketdata.common.log.AppLogger;
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

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
@Tag(name = "Market Analysis", description = "Unified API for all market analysis including Seasonality, Technicals, Heatmaps, Performance, and Charts")
@CrossOrigin(originPatterns = "*")
public class AnalysisController {

    private final AppLogger log = AppLogger.getLogger(AnalysisController.class);
    private final AnalysisService analysisService;
    private final MarketAnalyticsService marketAnalyticsService;

    // --- Original Analysis Endpoints ---

    @GetMapping("/seasonality")
    public ResponseEntity<SeasonalityResponse> getSeasonality(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "DAY") String timeframe) {

        TimeFrame tf = parseTimeFrame(timeframe);
        return ResponseEntity.ok(analysisService.getSeasonalityAnalysis(symbol, tf));
    }

    @GetMapping("/technical")
    public ResponseEntity<TechnicalAnalysisResponse> getTechnicalAnalysis(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "DAY") String timeframe) {

        TimeFrame tf = parseTimeFrame(timeframe);
        return ResponseEntity.ok(analysisService.getTechnicalAnalysis(symbol, tf));
    }

    @GetMapping("/heatmap/calendar")
    public ResponseEntity<CalendarHeatmapResponse> getCalendarHeatmap(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "-1") int year) {

        if (year == -1)
            year = java.time.LocalDate.now().getYear();
        return ResponseEntity.ok(analysisService.getCalendarHeatmap(symbol, year));
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
        TimeFrame tf = timeFrame != null ? TimeFrame.fromApiValue(timeFrame) : null;

        if ("all".equalsIgnoreCase(type)) {
            return ResponseEntity.ok(marketAnalyticsService.getMoversUnified(limit, index, tf, expandIndices));
        } else {
            return ResponseEntity.ok(marketAnalyticsService.getMovers(limit, type, index, tf, expandIndices));
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

        return ResponseEntity.ok(marketAnalyticsService.getHistoricalCharts(symbols, range, isIndexSymbol));
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
