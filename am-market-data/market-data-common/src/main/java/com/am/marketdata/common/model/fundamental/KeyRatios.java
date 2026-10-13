package com.am.marketdata.common.model.fundamental;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * Domain model representing valuation, profitability, banking, and liquidity ratios alongside sector benchmarks.
 * Features a dynamic extension map for future-proof provider and sector adaptivity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyRatios implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Price to Earnings ratio (P/E) for the company.
     */
    private Double pe;

    /**
     * Benchmark P/E ratio for the company's sector.
     */
    private Double sectorPe;

    /**
     * Price to Book value ratio (P/B) for the company.
     */
    private Double pb;

    /**
     * Benchmark P/B ratio for the company's sector.
     */
    private Double sectorPb;

    /**
     * Return on Assets percentage (ROA) for the company.
     */
    private Double roa;

    /**
     * Benchmark ROA for the company's sector.
     */
    private Double sectorRoa;

    /**
     * Return on Equity percentage (ROE) for the company.
     */
    private Double roe;

    /**
     * Benchmark ROE for the company's sector.
     */
    private Double sectorRoe;

    /**
     * Return on Capital Employed percentage (ROCE) for the company.
     */
    private Double roce;

    /**
     * Benchmark ROCE for the company's sector.
     */
    private Double sectorRoce;

    /**
     * Enterprise Value to EBITDA (EV/EBITDA) for the company.
     */
    private Double evEbitda;

    /**
     * Benchmark EV/EBITDA for the company's sector.
     */
    private Double sectorEvEbitda;

    /**
     * Quick Ratio (Acid Test) for corporate liquidity.
     */
    private Double quickRatio;

    /**
     * Benchmark Quick Ratio for the company's sector.
     */
    private Double sectorQuickRatio;

    /**
     * Net Interest Margin percentage (NIM) for banking institutions.
     */
    private Double nim;

    /**
     * Benchmark NIM for banking sector.
     */
    private Double sectorNim;

    /**
     * Net Non-Performing Assets percentage (Net NPA) for banking institutions.
     */
    private Double netNpa;

    /**
     * Benchmark Net NPA for banking sector.
     */
    private Double sectorNetNpa;

    /**
     * Current Account Savings Account ratio percentage (CASA) for banking institutions.
     */
    private Double casa;

    /**
     * Benchmark CASA for banking sector.
     */
    private Double sectorCasa;

    /**
     * Dynamic sector-adaptive ratios map to preserve any provider-specific metric without schema locks.
     */
    private Map<String, Double> dynamicRatios;

    /**
     * Dynamic sector benchmarks map.
     */
    private Map<String, Double> sectorDynamicRatios;
}
