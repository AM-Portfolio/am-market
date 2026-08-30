package com.am.marketdata.common.model.fundamental;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Domain model representing high-level company profile and sector information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Business description and operational profile of the company.
     */
    private String description;

    /**
     * Sector classification (e.g. "FMCG", "Information Technology").
     */
    private String sector;

    /**
     * Total market capitalization of the sector in Indian Rupees (₹ Cr).
     */
    private Double sectorMarketCapInr;

    /**
     * Total market capitalization of the sector in USD ($ M/B).
     */
    private Double sectorMarketCapUsd;
}
