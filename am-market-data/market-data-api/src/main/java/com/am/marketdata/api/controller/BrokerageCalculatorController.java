package com.am.marketdata.api.controller;

import com.am.marketdata.api.service.BrokerageCalculatorApiService;
import com.marketdata.common.model.margin.BrokerageCalculationRequest;
import com.marketdata.common.model.margin.BrokerageCalculationResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.extern.slf4j.Slf4j;
import com.am.marketdata.common.log.AppLogger;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.CompletableFuture;

/**
 * REST controller for brokerage and tax calculation
 */
@RestController
@RequestMapping("/v1/brokerage")
@Tag(name = "Brokerage Calculator", description = "API for calculating brokerage, taxes, and other charges for stock trades")
public class BrokerageCalculatorController {
    private final AppLogger log = AppLogger.getLogger();
    private final BrokerageCalculatorApiService brokerageCalculatorApiService;

    public BrokerageCalculatorController(BrokerageCalculatorApiService brokerageCalculatorApiService) {
        this.brokerageCalculatorApiService = brokerageCalculatorApiService;
        log.info("BrokerageCalculatorController", "Initializing Brokerage Calculator Controller");
    }

    /**
     * Calculate brokerage and taxes for a trade (synchronous)
     *
     * @param request The brokerage calculation request
     * @return BrokerageCalculationResponse with calculated charges
     */
    @PostMapping(value = "/calculate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Calculate brokerage and taxes", description = "Calculate brokerage, STT, GST, exchange charges, SEBI charges, stamp duty, and DP charges for a trade")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Brokerage calculation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BrokerageCalculationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<BrokerageCalculationResponse> calculateBrokerage(
            @RequestBody BrokerageCalculationRequest request) {

        String methodName = "calculateBrokerage";
        log.info(methodName, String.format("Received brokerage calculation request for %s trade of %d shares of %s",
                request.getTradeType(), request.getQuantity(), request.getTradingSymbol()));

        try {
            BrokerageCalculationResponse response = brokerageCalculatorApiService.calculateBrokerage(request);

            if ("ERROR".equals(response.getStatus())) {
                log.error(methodName, "Error calculating brokerage: " + response.getError());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, response.getError());
            }

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error(methodName, "Invalid brokerage calculation request: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error(methodName, "Error processing brokerage calculation request: " + e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to calculate brokerage: " + e.getMessage());
        }
    }

    /**
     * Calculate brokerage and taxes for a trade (asynchronous)
     *
     * @param request The brokerage calculation request
     * @return CompletableFuture with BrokerageCalculationResponse
     */
    @PostMapping(value = "/calculate-async", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Calculate brokerage and taxes asynchronously", description = "Asynchronously calculate brokerage, STT, GST, exchange charges, SEBI charges, stamp duty, and DP charges for a trade")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Brokerage calculation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BrokerageCalculationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<BrokerageCalculationResponse>> calculateBrokerageAsync(
            @RequestBody BrokerageCalculationRequest request) {

        String methodName = "calculateBrokerageAsync";
        log.info(methodName,
                String.format("Received async brokerage calculation request for %s trade of %d shares of %s",
                        request.getTradeType(), request.getQuantity(), request.getTradingSymbol()));

        try {
            return brokerageCalculatorApiService.calculateBrokerageAsync(request)
                    .thenApply(response -> {
                        if ("ERROR".equals(response.getStatus())) {
                            log.error(methodName, "Error calculating brokerage asynchronously: " + response.getError());
                            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, response.getError());
                        }

                        return ResponseEntity.ok(response);
                    });
        } catch (IllegalArgumentException e) {
            log.error(methodName, "Invalid async brokerage calculation request: " + e.getMessage());
            CompletableFuture<ResponseEntity<BrokerageCalculationResponse>> future = new CompletableFuture<>();
            future.completeExceptionally(new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()));
            return future;
        } catch (Exception e) {
            log.error(methodName, "Error processing async brokerage calculation request: " + e.getMessage(), e);
            CompletableFuture<ResponseEntity<BrokerageCalculationResponse>> future = new CompletableFuture<>();
            future.completeExceptionally(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to calculate brokerage: " + e.getMessage()));
            return future;
        }
    }

    /**
     * Get breakeven price for a stock
     *
     * @param symbol     Trading symbol
     * @param price      Buy price
     * @param quantity   Quantity
     * @param exchange   Exchange (NSE/BSE)
     * @param tradeType  Trade type (DELIVERY/INTRADAY)
     * @param brokerType Broker type (DISCOUNT/FULL_SERVICE)
     * @return Breakeven price
     */
    @GetMapping(value = "/breakeven", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Calculate breakeven price", description = "Calculate the breakeven price for a stock considering all charges")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Breakeven calculation successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<BrokerageCalculationResponse> calculateBreakeven(
            @RequestParam("symbol") String symbol,
            @RequestParam("price") double price,
            @RequestParam("quantity") int quantity,
            @RequestParam("exchange") String exchange,
            @RequestParam("tradeType") String tradeType,
            @RequestParam("brokerType") String brokerType) {

        String methodName = "calculateBreakeven";
        log.info(methodName, String.format("Received breakeven calculation request for %d shares of %s at price %f",
                quantity, symbol, price));

        try {
            // Create request object
            BrokerageCalculationRequest request = BrokerageCalculationRequest.builder()
                    .tradingSymbol(symbol)
                    .buyPrice(java.math.BigDecimal.valueOf(price))
                    .quantity(quantity)
                    .exchange(exchange)
                    .tradeType(BrokerageCalculationRequest.TradeType.valueOf(tradeType))
                    .brokerType(BrokerageCalculationRequest.BrokerType.valueOf(brokerType))
                    .build();

            BrokerageCalculationResponse response = brokerageCalculatorApiService.calculateBrokerage(request);

            if ("ERROR".equals(response.getStatus())) {
                log.error(methodName, "Error calculating breakeven: " + response.getError());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, response.getError());
            }

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error(methodName, "Invalid breakeven calculation request: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error(methodName, "Error processing breakeven calculation request: " + e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to calculate breakeven: " + e.getMessage());
        }
    }
}
