package com.am.marketdata.api.controller;

import com.am.marketdata.api.dto.HistoricalDataRequest;
import com.am.marketdata.api.service.MarketAnalyticsService;
import com.am.marketdata.api.service.MarketDataFetchService;
import com.am.marketdata.common.log.AppLogger;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.api.model.HistoricalDataResponseV1;
import com.am.common.investment.model.historical.HistoricalData;
import com.am.common.investment.model.historical.OHLCVTPoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/market-analytics")
@RequiredArgsConstructor
@Tag(name = "Market Analytics", description = "APIs for market analysis including Top Movers, Sector Performance, Market Cap Analysis, and Historical Charts")
public class MarketAnalyticsController {

    private final AppLogger log = AppLogger.getLogger(MarketAnalyticsController.class);
    private final MarketAnalyticsService marketAnalyticsService;
    private final MarketDataFetchService marketDataFetchService;

    /**
     * Get Top Gainers or Losers
     */
    @GetMapping(value = "/movers", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get Top Gainers/Losers", description = "Retrieves top performing or worst performing stocks from the specified market index")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Data retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getMovers(
            @RequestParam(defaultValue = "gainers") String type,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String indexSymbol,
            @RequestParam(required = false) String timeFrame,
            @RequestParam(defaultValue = "false") boolean expandIndices) {
        try {
            String index = indexSymbol != null ? indexSymbol : "NIFTY 50";
            com.am.marketdata.common.model.TimeFrame tf = timeFrame != null
                    ? com.am.marketdata.common.model.TimeFrame.fromApiValue(timeFrame)
                    : null;
            log.info("getMovers",
                    "Fetching top " + limit + " " + type + " from " + index + " with timeFrame: " + timeFrame
                            + ", expandIndices: " + expandIndices);

            if ("all".equalsIgnoreCase(type)) {
                Map<String, List<Map<String, Object>>> movers = marketAnalyticsService.getMoversUnified(limit,
                        indexSymbol, tf, expandIndices);
                return ResponseEntity.ok(movers);
            } else {
                List<Map<String, Object>> movers = marketAnalyticsService.getMovers(limit, type, indexSymbol, tf,
                        expandIndices);
                return ResponseEntity.ok(movers);
            }
        } catch (Exception e) {
            log.error("getMovers", "Error fetching movers", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get Sector Performance
     */
    @GetMapping(value = "/sectors", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get Sector Performance", description = "Aggregates market performance by sector (Industry) from the specified index")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Data retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Map<String, Object>>> getSectorPerformance(
            @RequestParam(required = false) String indexSymbol,
            @RequestParam(required = false) String timeFrame,
            @RequestParam(defaultValue = "false") boolean expandIndices) {
        try {
            String index = indexSymbol != null ? indexSymbol : "NIFTY 50";
            com.am.marketdata.common.model.TimeFrame tf = timeFrame != null
                    ? com.am.marketdata.common.model.TimeFrame.fromApiValue(timeFrame)
                    : null;
            log.info("getSectorPerformance",
                    "Fetching sector performance from " + index + " with timeFrame: " + timeFrame + ", expandIndices: "
                            + expandIndices);
            List<Map<String, Object>> sectors = marketAnalyticsService.getSectorPerformance(indexSymbol, tf,
                    expandIndices);
            return ResponseEntity.ok(sectors);
        } catch (Exception e) {
            log.error("getSectorPerformance", "Error fetching sector performance", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get Index Performance (All Constituents)
     */
    @GetMapping(value = "/index-performance", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get Index Performance", description = "Retrieves performance of all constituent stocks in the specified index for a given timeframe")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Data retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Map<String, Object>>> getIndexPerformance(
            @RequestParam(required = false) String indexSymbol,
            @RequestParam(required = false) String timeFrame) {
        try {
            String index = indexSymbol != null ? indexSymbol : "NIFTY 50";
            com.am.marketdata.common.model.TimeFrame tf = timeFrame != null
                    ? com.am.marketdata.common.model.TimeFrame.fromApiValue(timeFrame)
                    : null;

            log.info("getIndexPerformance",
                    "Fetching index performance for " + index + " with timeFrame: " + timeFrame);

            List<Map<String, Object>> result = marketAnalyticsService.getIndexPerformance(index, tf);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("getIndexPerformance", "Error fetching index performance", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get Historical Charts (Batch or Single)
     */
    @GetMapping(value = "/historical-charts", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get historical charts data", description = "Retrieves historical data for charts with various time frames (10m, 1H, 1D, 1W, 1M, 5Y, etc.)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chart data retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<HistoricalDataResponseV1> getHistoricalCharts(
            @RequestParam String symbols,
            @RequestParam(defaultValue = "1D") String range,
            @RequestParam(defaultValue = "true") boolean isIndexSymbol) {
        try {
            log.info("getHistoricalCharts", "Fetching historical charts for symbols: " + symbols + ", range: "
                    + range + ", isIndexSymbol: " + isIndexSymbol);

            String interval = "1D";
            java.time.LocalDateTime to = java.time.LocalDateTime.now();
            java.time.LocalDateTime from = to.minusDays(1);

            // Determine Interval and From Time based on Range
            switch (range.toUpperCase()) {
                case "10M":
                    interval = "1m";
                    from = to.minusMinutes(10);
                    break;
                case "15M":
                    interval = "1m";
                    from = to.minusMinutes(15);
                    break;
                case "30M":
                    interval = "1m";
                    from = to.minusMinutes(30);
                    break;
                case "1H":
                    interval = "1m";
                    from = to.minusHours(1);
                    break;
                case "4H":
                    interval = "5m";
                    from = to.minusHours(4);
                    break;
                case "1D": // Change default to 1D to match daily chart expectation
                    interval = "5m";
                    from = to.minusDays(1);
                    break;
                case "1W":
                    interval = "1H";
                    from = to.minusWeeks(1);
                    break;
                case "1M":
                    interval = "1D";
                    from = to.minusMonths(1);
                    break;
                case "5Y":
                    interval = "1W";
                    from = to.minusYears(5);
                    break;
                default: // Default 1Y
                    interval = "1D";
                    from = to.minusYears(1);
            }

            // Construct Request
            HistoricalDataRequest request = HistoricalDataRequest.builder()
                    .symbols(symbols)
                    .from(from.toLocalDate().toString())
                    .to(to.toLocalDate().toString())
                    .interval(TimeFrame.fromApiValue(interval)) // Convert string to TimeFrame
                    .filterType("price")
                    .indexSymbol(isIndexSymbol)
                    .build();

            // Fetch Data
            HistoricalDataResponseV1 response = marketDataFetchService.processHistoricalDataRequest(request);

            if (response.getError() != null) {
                return ResponseEntity.status(500).body(response);
            }

            // Filter data points by time range for each symbol
            if (response.getData() != null) {
                long minTime = from.atZone(java.time.ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli();

                response.getData().forEach((s, symbolData) -> {
                    if (symbolData != null && symbolData.getDataPoints() != null) {
                        List<OHLCVTPoint> filteredPoints = symbolData.getDataPoints().stream()
                                .filter(p -> {
                                    try {
                                        long timestamp = p.getTime().atZone(java.time.ZoneId.systemDefault())
                                                .toInstant()
                                                .toEpochMilli();
                                        return timestamp >= minTime;
                                    } catch (Exception e) {
                                        return true;
                                    }
                                })
                                .collect(Collectors.toList());

                        symbolData.setDataPoints(filteredPoints);
                    }
                });
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("getHistoricalCharts", "Error fetching historical charts: " + e.getMessage());
            HistoricalDataResponseV1 errorResponse = HistoricalDataResponseV1.builder()
                    .error("Failed to fetch chart data")
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get Historical Charts (Single Symbol - Legacy Support)
     */
    @GetMapping(value = "/historical-charts/{symbol}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get historical charts data (Legacy path)", description = "Retrieves historical data for charts with various time frames")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chart data retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<HistoricalDataResponseV1> getHistoricalChartsLegacy(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1D") String range,
            @RequestParam(defaultValue = "true") boolean isIndexSymbol) {
        return getHistoricalCharts(symbol, range, isIndexSymbol);
    }
}
