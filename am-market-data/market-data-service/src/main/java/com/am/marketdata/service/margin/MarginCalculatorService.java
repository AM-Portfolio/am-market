package com.am.marketdata.service.margin;

import com.marketdata.common.model.margin.MarginCalculationRequest;
import com.marketdata.common.model.margin.MarginCalculationResponse;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Service for calculating margin requirements for various positions
 * Implements resilient patterns including retry, metrics, and async processing
 */
@Slf4j
@Service
public class MarginCalculatorService {

    private final MeterRegistry meterRegistry;
    private final ThreadPoolExecutor threadPoolExecutor;

    @Value("${market-data.margin.calculator.default-exposure-margin-percent:5}")
    private int defaultExposureMarginPercent;

    @Value("${market-data.margin.calculator.default-span-margin-percent:10}")
    private int defaultSpanMarginPercent;

    @Value("${market-data.margin.calculator.use-broker-api:true}")
    private boolean useBrokerApi;

    public MarginCalculatorService(
            MeterRegistry meterRegistry,
            ThreadPoolExecutor threadPoolExecutor) {
        this.meterRegistry = meterRegistry;
        this.threadPoolExecutor = threadPoolExecutor;
        log.info("Initializing Margin Calculator Service");
    }

    /**
     * Calculate margin requirement for a list of positions
     * 
     * @param request The margin calculation request containing positions
     * @return MarginCalculationResponse with calculated margins
     */
    public MarginCalculationResponse calculateMargin(MarginCalculationRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        log.info("Calculating margin for {} positions", request.getPositions().size());

        try {
            return calculateMarginLocally(request);
        } catch (Exception e) {
            log.error("Error calculating margin: {}", e.getMessage(), e);
            meterRegistry.counter("market-data.margin.calculation.error").increment();

            // Return error response
            return MarginCalculationResponse.builder()
                    .status("ERROR")
                    .error("Failed to calculate margin: " + e.getMessage())
                    .build();
        } finally {
            sample.stop(meterRegistry.timer("market-data.margin.calculation.time"));
        }
    }

    /**
     * Calculate margin asynchronously
     * 
     * @param request The margin calculation request
     * @return CompletableFuture with the margin calculation response
     */
    public CompletableFuture<MarginCalculationResponse> calculateMarginAsync(MarginCalculationRequest request) {
        return CompletableFuture.supplyAsync(() -> calculateMargin(request), threadPoolExecutor);
    }

    /**
     * Calculate margin using broker API for accurate margin requirements
     * 
     * @param request The margin calculation request
     * @return MarginCalculationResponse with calculated margins from broker
     */
    private MarginCalculationResponse calculateMarginUsingBrokerApi(MarginCalculationRequest request) {
        try {
            log.info("Calculating margin using broker API");
            meterRegistry.counter("market-data.margin.calculation.broker.api").increment();

            // TODO: Implement actual broker API call using zerodhaApiService
            // This is a placeholder for the actual implementation

            // For now, fall back to local calculation
            return calculateMarginLocally(request);
        } catch (Exception e) {
            log.warn("Failed to calculate margin using broker API: {}, falling back to local calculation",
                    e.getMessage());
            meterRegistry.counter("market-data.margin.calculation.broker.api.failure").increment();
            return calculateMarginLocally(request);
        }
    }

    /**
     * Calculate margin locally using predefined rules
     * This is a simplified calculation and should be used as fallback only
     * 
     * @param request The margin calculation request
     * @return MarginCalculationResponse with calculated margins
     */
    private MarginCalculationResponse calculateMarginLocally(MarginCalculationRequest request) {
        log.info("Calculating margin locally");
        meterRegistry.counter("market-data.margin.calculation.local").increment();

        BigDecimal totalSpanMargin = BigDecimal.ZERO;
        BigDecimal totalExposureMargin = BigDecimal.ZERO;
        BigDecimal totalAdditionalMargin = BigDecimal.ZERO;

        Map<String, MarginCalculationResponse.PositionMargin> positionMargins = new HashMap<>();

        for (MarginCalculationRequest.Position position : request.getPositions()) {
            // Calculate position value
            BigDecimal positionValue = position.getPrice()
                    .multiply(BigDecimal.valueOf(Math.abs(position.getQuantity())));

            // Calculate SPAN margin (varies by instrument type)
            BigDecimal spanMarginPercent = getSpanMarginPercent(position);
            BigDecimal spanMargin = positionValue
                    .multiply(spanMarginPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            // Calculate exposure margin
            BigDecimal exposureMarginPercent = getExposureMarginPercent(position);
            BigDecimal exposureMargin = positionValue
                    .multiply(exposureMarginPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            // Additional margin (if any)
            BigDecimal additionalMargin = calculateAdditionalMargin(position);

            // Total margin for this position
            BigDecimal totalPositionMargin = spanMargin.add(exposureMargin).add(additionalMargin);

            // Add to position margins map
            String key = position.getTradingSymbol() + "-" + position.getExchange();
            positionMargins.put(key, MarginCalculationResponse.PositionMargin.builder()
                    .tradingSymbol(position.getTradingSymbol())
                    .spanMargin(spanMargin)
                    .exposureMargin(exposureMargin)
                    .additionalMargin(additionalMargin)
                    .totalMargin(totalPositionMargin)
                    .type(position.getType())
                    .exchange(position.getExchange())
                    .build());

            // Add to totals
            totalSpanMargin = totalSpanMargin.add(spanMargin);
            totalExposureMargin = totalExposureMargin.add(exposureMargin);
            totalAdditionalMargin = totalAdditionalMargin.add(additionalMargin);
        }

        // Calculate total margin
        BigDecimal totalMarginRequired = totalSpanMargin.add(totalExposureMargin).add(totalAdditionalMargin);

        // Build response
        return MarginCalculationResponse.builder()
                .spanMargin(totalSpanMargin)
                .exposureMargin(totalExposureMargin)
                .additionalMargin(totalAdditionalMargin)
                .totalMarginRequired(totalMarginRequired)
                .positionMargins(positionMargins)
                .status("SUCCESS")
                .build();
    }

    /**
     * Get SPAN margin percentage based on position type
     * 
     * @param position The position
     * @return SPAN margin percentage
     */
    private BigDecimal getSpanMarginPercent(MarginCalculationRequest.Position position) {
        // Different margin requirements based on instrument type
        switch (position.getType().toLowerCase()) {
            case "equity":
                return BigDecimal.valueOf("MIS".equals(position.getProduct()) ? 8 : defaultSpanMarginPercent);
            case "future":
                return BigDecimal.valueOf(12);
            case "option":
                return BigDecimal.valueOf("CE".equals(position.getOptionType()) ? 15 : 20);
            default:
                return BigDecimal.valueOf(defaultSpanMarginPercent);
        }
    }

    /**
     * Get exposure margin percentage based on position type
     * 
     * @param position The position
     * @return Exposure margin percentage
     */
    private BigDecimal getExposureMarginPercent(MarginCalculationRequest.Position position) {
        // Different exposure margin requirements based on instrument type
        switch (position.getType().toLowerCase()) {
            case "equity":
                return BigDecimal.valueOf("MIS".equals(position.getProduct()) ? 3 : defaultExposureMarginPercent);
            case "future":
                return BigDecimal.valueOf(5);
            case "option":
                return BigDecimal.valueOf(3);
            default:
                return BigDecimal.valueOf(defaultExposureMarginPercent);
        }
    }

    /**
     * Calculate additional margin based on position type and other factors
     * 
     * @param position The position
     * @return Additional margin amount
     */
    private BigDecimal calculateAdditionalMargin(MarginCalculationRequest.Position position) {
        // For now, return zero additional margin
        // This can be enhanced with more complex rules in the future
        return BigDecimal.ZERO;
    }
}
