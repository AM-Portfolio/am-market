package com.am.marketdata.api.controller;

import com.marketdata.common.model.margin.MarginCalculationRequest;
import com.marketdata.common.model.margin.MarginCalculationResponse;
import com.am.marketdata.api.service.MarginCalculatorApiService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import com.am.marketdata.common.log.AppLogger;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * REST controller for margin calculation operations
 * Provides endpoints for calculating margin requirements for various trading
 * positions
 */
@RestController
@RequestMapping("/v1/margin")
@Tag(name = "Margin Calculator", description = "APIs for calculating margin requirements for trading positions across different exchanges and instruments")
public class MarginCalculatorController {

        private final AppLogger log = AppLogger.getLogger();
        private final MarginCalculatorApiService marginCalculatorApiService;

        public MarginCalculatorController(MarginCalculatorApiService marginCalculatorApiService) {
                this.marginCalculatorApiService = marginCalculatorApiService;
        }

        /**
         * Calculate margin requirement for a list of positions
         * 
         * @param request The margin calculation request containing positions
         * @return MarginCalculationResponse with calculated margins
         */
        @PostMapping(value = "/calculate", produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(summary = "Calculate margin requirements", description = "Calculate SPAN margin, exposure margin, and total margin requirements for a list of positions across different segments")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Margin calculation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MarginCalculationResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<MarginCalculationResponse> calculateMargin(
                        @RequestBody MarginCalculationRequest request) {

                log.info("calculateMargin", "Received margin calculation request for {} positions",
                                request.getPositions().size());

                try {
                        MarginCalculationResponse response = marginCalculatorApiService.calculateMargin(request);
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        log.error("calculateMargin", "Error calculating margin: " + e.getMessage(), e);

                        MarginCalculationResponse errorResponse = MarginCalculationResponse.builder()
                                        .status("ERROR")
                                        .error("Failed to calculate margin: " + e.getMessage())
                                        .build();

                        return ResponseEntity.badRequest().body(errorResponse);
                }
        }

        /**
         * Calculate margin requirement asynchronously
         * 
         * @param request The margin calculation request containing positions
         * @return CompletableFuture with MarginCalculationResponse
         */
        @PostMapping(value = "/calculate-async", produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(summary = "Calculate margin requirements asynchronously", description = "Asynchronously calculate SPAN margin, exposure margin, and total margin requirements for a list of positions across different segments")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Margin calculation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MarginCalculationResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public CompletableFuture<ResponseEntity<MarginCalculationResponse>> calculateMarginAsync(
                        @RequestBody MarginCalculationRequest request) {

                log.info("calculateMarginAsync", "Received async margin calculation request for {} positions",
                                request.getPositions().size());

                return marginCalculatorApiService.calculateMarginAsync(request)
                                .thenApply(ResponseEntity::ok)
                                .exceptionally(ex -> {
                                        log.error("calculateMarginAsync",
                                                        "Error calculating margin asynchronously: " + ex.getMessage(),
                                                        ex);

                                        MarginCalculationResponse errorResponse = MarginCalculationResponse.builder()
                                                        .status("ERROR")
                                                        .error("Failed to calculate margin: " + ex.getMessage())
                                                        .build();

                                        return ResponseEntity.badRequest().body(errorResponse);
                                });
        }
}
