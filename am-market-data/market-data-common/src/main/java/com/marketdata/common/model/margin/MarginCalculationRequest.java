package com.marketdata.common.model.margin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request model for margin calculation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarginCalculationRequest {
    
    /**
     * List of positions to calculate margin for
     */
    private List<Position> positions;
    
    /**
     * Optional order type (MIS, NRML, CNC)
     */
    private String orderType;
    
    /**
     * Optional exchange (NSE, BSE)
     */
    private String exchange;
    
    /**
     * Position model representing a single position
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Position {
        /**
         * Trading symbol (e.g., INFY, RELIANCE)
         */
        private String tradingSymbol;
        
        /**
         * Quantity of the position (positive for long, negative for short)
         */
        private int quantity;
        
        /**
         * Position type (equity, future, option, etc.)
         */
        private String type;
        
        /**
         * Product type (MIS, NRML, CNC)
         */
        private String product;
        
        /**
         * Exchange (NSE, BSE)
         */
        private String exchange;
        
        /**
         * Price at which the position is entered/to be entered
         */
        private BigDecimal price;
        
        /**
         * Option specific fields
         */
        private String optionType; // CE or PE
        private BigDecimal strikePrice;
        private String expiry; // Format: YYYY-MM-DD
    }
}
