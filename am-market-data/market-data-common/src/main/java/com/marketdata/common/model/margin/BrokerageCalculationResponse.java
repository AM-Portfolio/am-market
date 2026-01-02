package com.marketdata.common.model.margin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response model for brokerage and tax calculation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrokerageCalculationResponse {
    
    /**
     * Total transaction value (buy side)
     */
    private BigDecimal buyTransactionValue;
    
    /**
     * Total transaction value (sell side)
     */
    private BigDecimal sellTransactionValue;
    
    /**
     * Brokerage fee for buy transaction
     */
    private BigDecimal buyBrokerage;
    
    /**
     * Brokerage fee for sell transaction
     */
    private BigDecimal sellBrokerage;
    
    /**
     * Securities Transaction Tax (STT) for buy transaction
     */
    private BigDecimal buySTT;
    
    /**
     * Securities Transaction Tax (STT) for sell transaction
     */
    private BigDecimal sellSTT;
    
    /**
     * Exchange transaction charges for buy transaction
     */
    private BigDecimal buyExchangeCharges;
    
    /**
     * Exchange transaction charges for sell transaction
     */
    private BigDecimal sellExchangeCharges;
    
    /**
     * GST on buy transaction charges
     */
    private BigDecimal buyGST;
    
    /**
     * GST on sell transaction charges
     */
    private BigDecimal sellGST;
    
    /**
     * SEBI turnover charges for buy transaction
     */
    private BigDecimal buySEBICharges;
    
    /**
     * SEBI turnover charges for sell transaction
     */
    private BigDecimal sellSEBICharges;
    
    /**
     * Stamp duty for buy transaction
     */
    private BigDecimal buyStampDuty;
    
    /**
     * Stamp duty for sell transaction
     */
    private BigDecimal sellStampDuty;
    
    /**
     * DP charges (applicable on sell transactions only)
     */
    private BigDecimal dpCharges;
    
    /**
     * Total charges for buy transaction
     */
    private BigDecimal totalBuyCharges;
    
    /**
     * Total charges for sell transaction
     */
    private BigDecimal totalSellCharges;
    
    /**
     * Total charges for the complete trade (buy + sell)
     */
    private BigDecimal totalCharges;
    
    /**
     * Net profit/loss after all charges (if sell price is provided)
     */
    private BigDecimal netProfitLoss;
    
    /**
     * Percentage of charges relative to transaction value
     */
    private BigDecimal chargesPercentage;
    
    /**
     * Breakeven price (sell price needed to cover all charges)
     */
    private BigDecimal breakEvenPrice;
    
    /**
     * Status of the calculation
     */
    private String status;
    
    /**
     * Error message (if any)
     */
    private String error;
}
