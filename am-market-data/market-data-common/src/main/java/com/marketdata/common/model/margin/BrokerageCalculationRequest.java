package com.marketdata.common.model.margin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request model for brokerage and tax calculation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrokerageCalculationRequest {
    
    /**
     * Trading symbol (e.g., INFY, RELIANCE)
     */
    private String tradingSymbol;
    
    /**
     * Quantity of shares
     */
    private int quantity;
    /**
     * Buy price
     */
    private BigDecimal buyPrice;
    
    /**
     * Sell price (optional, for completed trades)
     */
    private BigDecimal sellPrice;
    
    /**
     * Exchange (NSE, BSE)
     */
    private String exchange;
    
    /**
     * Trade type (DELIVERY, INTRADAY)
     */
    private TradeType tradeType;
    
    /**
     * Broker type (DISCOUNT, FULL_SERVICE)
     */
    private BrokerType brokerType;
    
    /**
     * Broker name (e.g., zerodha, upstox, mstock)
     * Used to look up broker-specific fees
     */
    private String brokerName;
    
    /**
     * Broker-specific flat fee (if applicable)
     * If provided, this overrides the configured fee for the broker
     */
    private BigDecimal brokerFlatFee;
    
    /**
     * Broker-specific percentage fee (if applicable)
     * If provided, this overrides the configured fee for the broker
     */
    private BigDecimal brokerPercentageFee;
    
    /**
     * State code for stamp duty calculation
     */
    private String stateCode;
    
    /**
     * Trade types
     */
    public enum TradeType {
        DELIVERY,
        INTRADAY
    }
    
    /**
     * Broker types
     */
    public enum BrokerType {
        DISCOUNT,
        FULL_SERVICE
    }
}
