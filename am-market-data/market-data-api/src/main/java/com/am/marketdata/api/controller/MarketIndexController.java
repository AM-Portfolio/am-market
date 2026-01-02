package com.am.marketdata.api.controller;

import com.am.common.investment.model.stockindice.StockIndicesMarketData;
import com.am.marketdata.api.service.StockIndicesService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.am.marketdata.common.log.AppLogger;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for NSE (National Stock Exchange) indices data
 * Provides endpoints for retrieving market data for various NSE indices
 */
@RestController
@RequestMapping("/v1/indices")
@RequiredArgsConstructor
@Tag(name = "Indices", description = "APIs for retrieving market data for various indices")
public class MarketIndexController {
        private final AppLogger log = AppLogger.getLogger();
        private final com.am.marketdata.scraper.config.NSEIndicesConfig nseIndicesConfig;
        private final StockIndicesService stockIndicesService;

        /**
         * Get available indices
         * 
         * @return List of available indices (Broad and Sector)
         */
        @GetMapping(value = "/available", produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(summary = "Get available indices", description = "Retrieves the list of available indices (Broad and Sector)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Indices retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.List.class))),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<java.util.Map<String, java.util.List<String>>> getAvailableIndices() {
                log.info("getAvailableIndices", "Fetching available indices");
                java.util.Map<String, java.util.List<String>> indices = new java.util.HashMap<>();
                indices.put("broad", nseIndicesConfig.getBroadMarketIndices());
                indices.put("sector", nseIndicesConfig.getSectorIndices());
                return ResponseEntity.ok(indices);
        }

        /**
         * Get latest market data for multiple indices
         * 
         * @param indexSymbols List of Index symbols
         * @param forceRefresh Whether to force refresh from source instead of using
         *                     cache
         * @return List of market data for the requested indices
         */
        @PostMapping(value = "/batch", produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(summary = "Get latest market data for multiple indices", description = "Retrieves the latest market data for multiple indices in a single request")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Indices data retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StockIndicesMarketData.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<java.util.List<StockIndicesMarketData>> getLatestIndicesData(
                        @Parameter(description = "List of Index symbols (e.g., NIFTY, BANKNIFTY, FINNIFTY)", required = true) @RequestBody java.util.List<String> indexSymbols,
                        @Parameter(description = "Force refresh from source instead of using cache") @RequestParam(value = "forceRefresh", required = false, defaultValue = "false") boolean forceRefresh) {

                String methodName = "getLatestIndicesData";
                log.info(methodName, String.format("Fetching latest data for indices: %s, forceRefresh: %b",
                                indexSymbols, forceRefresh));
                java.util.List<StockIndicesMarketData> data = stockIndicesService.getLatestIndicesData(indexSymbols,
                                forceRefresh);

                return ResponseEntity.ok(data);
        }
}
