package com.am.marketdata.analysis.controller;

import com.am.marketdata.analysis.service.AnalysisService;
import com.am.marketdata.common.model.analysis.CalendarHeatmapResponse;
import com.am.marketdata.common.model.analysis.SeasonalityResponse;
import com.am.marketdata.common.model.analysis.TechnicalAnalysisResponse;
import com.am.marketdata.common.model.TimeFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(originPatterns = "*")
public class AnalysisController {

    private final AnalysisService analysisService;

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

    private TimeFrame parseTimeFrame(String tf) {
        try {
            return TimeFrame.valueOf(tf.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TimeFrame.DAY;
        }
    }
}
