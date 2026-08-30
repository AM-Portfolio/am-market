package com.am.marketdata.common.model.fundamental;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Domain model representing valuation and profitability ratios alongside sector benchmarks.
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
}
