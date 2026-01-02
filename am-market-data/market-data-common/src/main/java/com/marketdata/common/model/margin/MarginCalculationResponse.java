package com.marketdata.common.model.margin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Response model for margin calculation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarginCalculationResponse {
    
    /**
     * Total margin required for all positions
     */
    private BigDecimal totalMarginRequired;
    
    /**
     * SPAN margin component
     */
    private BigDecimal spanMargin;
    
    /**
     * Exposure margin component
     */
    private BigDecimal exposureMargin;
    
    /**
     * Additional margins (if any)
     */
    private BigDecimal additionalMargin;
    
    /**
     * Breakdown of margin by position
     */
    private Map<String, PositionMargin> positionMargins;
    
    /**
     * Status of the calculation
     */
    private String status;
    
    /**
     * Error message (if any)
     */
    private String error;
    
    /**
     * Margin details for a specific position
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionMargin {
        /**
         * Trading symbol
         */
        private String tradingSymbol;
        
        /**
         * Total margin required for this position
         */
        private BigDecimal totalMargin;
        
        /**
         * SPAN margin component
         */
        private BigDecimal spanMargin;
        
        /**
         * Exposure margin component
         */
        private BigDecimal exposureMargin;
        
        /**
         * Additional margins (if any)
         */
        private BigDecimal additionalMargin;
        
        /**
         * Position type
         */
        private String type;
        
        /**
         * Exchange
         */
        private String exchange;
    }
}
