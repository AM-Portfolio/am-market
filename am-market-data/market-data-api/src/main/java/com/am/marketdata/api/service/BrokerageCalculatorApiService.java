package com.am.marketdata.api.service;

import com.marketdata.common.model.margin.BrokerageCalculationRequest;
import com.marketdata.common.model.margin.BrokerageCalculationResponse;
import com.marketdata.service.margin.BrokerageCalculatorService;
import com.am.marketdata.common.log.AppLogger;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

/**
 * API service for brokerage and tax calculation
 * Acts as a bridge between controller and service layer
 */
@Service
public class BrokerageCalculatorApiService {

    private final AppLogger log = AppLogger.getLogger();
    private final BrokerageCalculatorService brokerageCalculatorService;
    private final MeterRegistry meterRegistry;

    public BrokerageCalculatorApiService(BrokerageCalculatorService brokerageCalculatorService,
            MeterRegistry meterRegistry) {
        this.brokerageCalculatorService = brokerageCalculatorService;
        this.meterRegistry = meterRegistry;
        log.info("BrokerageCalculatorApiService", "Initializing Brokerage Calculator API Service");
    }

    /**
     * Calculate brokerage and taxes for a trade
     *
     * @param request The brokerage calculation request
     * @return BrokerageCalculationResponse with calculated charges
     */
    public BrokerageCalculationResponse calculateBrokerage(BrokerageCalculationRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String methodName = "calculateBrokerage";
        log.info(methodName, String.format("API: Calculating brokerage for %s trade of %d shares of %s",
                request.getTradeType(), request.getQuantity(), request.getTradingSymbol()));

        try {
            // Validate request
            validateRequest(request);

            // Call service layer
            BrokerageCalculationResponse response = brokerageCalculatorService.calculateBrokerage(request);

            // Increment success counter
            meterRegistry.counter("market-data.api.brokerage.calculation.success").increment();
            return response;
        } catch (Exception e) {
            log.error("calculateBrokerage", "API: Error calculating brokerage: " + e.getMessage(), e);
            meterRegistry.counter("market-data.api.brokerage.calculation.error").increment();

            // Return error response
            return BrokerageCalculationResponse.builder()
                    .status("ERROR")
                    .error("Failed to calculate brokerage: " + e.getMessage())
                    .build();
        } finally {
            sample.stop(meterRegistry.timer("market-data.api.brokerage.calculation.time"));
        }
    }

    /**
     * Calculate brokerage and taxes asynchronously
     *
     * @param request The brokerage calculation request
     * @return CompletableFuture with BrokerageCalculationResponse
     */
    public CompletableFuture<BrokerageCalculationResponse> calculateBrokerageAsync(
            BrokerageCalculationRequest request) {
        String methodName = "calculateBrokerageAsync";
        log.info(methodName, String.format("API: Calculating brokerage asynchronously for %s trade of %d shares of %s",
                request.getTradeType(), request.getQuantity(), request.getTradingSymbol()));

        try {
            // Validate request
            validateRequest(request);

            // Call service layer asynchronously
            return brokerageCalculatorService.calculateBrokerageAsync(request)
                    .thenApply(response -> {
                        meterRegistry.counter("market-data.api.brokerage.calculation.success").increment();
                        return response;
                    })
                    .exceptionally(ex -> {
                        log.error("calculateBrokerageAsync",
                                "API: Error calculating brokerage asynchronously: " + ex.getMessage(), ex);
                        meterRegistry.counter("market-data.api.brokerage.calculation.error").increment();

                        return BrokerageCalculationResponse.builder()
                                .status("ERROR")
                                .error("Failed to calculate brokerage: " + ex.getMessage())
                                .build();
                    });
        } catch (Exception e) {
            log.error("calculateBrokerageAsync", "API: Error initiating async brokerage calculation: " + e.getMessage(),
                    e);
            meterRegistry.counter("market-data.api.brokerage.calculation.error").increment();

            CompletableFuture<BrokerageCalculationResponse> future = new CompletableFuture<>();
            future.complete(BrokerageCalculationResponse.builder()
                    .status("ERROR")
                    .error("Failed to initiate brokerage calculation: " + e.getMessage())
                    .build());
            return future;
        }
    }

    /**
     * Validate brokerage calculation request
     *
     * @param request The request to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateRequest(BrokerageCalculationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        if (request.getTradingSymbol() == null || request.getTradingSymbol().isEmpty()) {
            throw new IllegalArgumentException("Trading symbol is required");
        }

        if (request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if (request.getBuyPrice() == null || request.getBuyPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Buy price must be greater than zero");
        }

        if (request.getSellPrice() != null && request.getSellPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Sell price must be greater than zero");
        }

        if (request.getExchange() == null || request.getExchange().isEmpty()) {
            throw new IllegalArgumentException("Exchange is required");
        }

        if (request.getTradeType() == null) {
            throw new IllegalArgumentException("Trade type is required");
        }

        if (request.getBrokerType() == null) {
            throw new IllegalArgumentException("Broker type is required");
        }

        // Log if broker name is provided for better debugging
        if (request.getBrokerName() != null && !request.getBrokerName().isEmpty()) {
            log.info("validateRequest", "Using broker-specific fees for broker: " + request.getBrokerName());
        }
    }
}
