package com.am.marketdata.common.model.fundamental;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * Competitor peer data enriched with both fundamental ratios, banking metrics, and real-time market pricing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitorPeer implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Unique trading instrument key (e.g., "NSE_EQ|INE154A01025").
     */
    private String instrumentKey;

    /**
     * International Securities Identification Number (ISIN).
     */
    private String isin;

    /**
     * Trading symbol (e.g., "ITC", "HUL").
     */
    private String symbol;

    /**
     * Company full legal / brand name.
     */
    private String companyName;

    /**
     * Sector classification.
     */
    private String sector;

    /**
     * Short company profile / business summary description.
     */
    private String description;

    /**
     * Total sector market capitalization in INR (crore).
     */
    private Double sectorMarketCapInr;

    /**
     * Total sector market capitalization in USD (billion).
     */
    private Double sectorMarketCapUsd;

    // Real-time market data (populated via existing market data service)
    private Double currentPrice;
    private Double dayChange;
    private Double dayChangePercent;

    // Key Valuation and Profitability Ratios for peer comparison
    private Double pe;
    private Double pb;
    private Double roe;
    private Double roce;
    private Double roa;
    private Double evEbitda;
    private Double quickRatio;
    private Double nim;
    private Double netNpa;
    private Double casa;

    /**
     * Dynamic ratios map for custom or provider-specific peer comparisons.
     */
    private Map<String, Double> dynamicRatios;
}
