package com.am.marketdata.common.model.ipo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Unified ASRAX domain DTO representing an IPO summary item in listing views.
 * Provider-agnostic model returned by {@link com.am.marketdata.common.provider.IpoDataProvider}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsraxIpoSummaryDto {

    /**
     * Unique provider-agnostic slug identifier (e.g., "shree-tnb-polymers-limited-ipo").
     */
    private String id;

    /**
     * Stock symbol assigned to the company (e.g., "SHREETNB").
     */
    private String symbol;

    /**
     * Display name of the IPO / Company.
     */
    private String companyName;

    /**
     * Lifecycle status: "open", "upcoming", "closed", or "listed".
     */
    private String status;

    /**
     * International Securities Identification Number (ISIN).
     */
    private String isin;

    /**
     * Issue segment: "regular" (Mainboard) or "sme".
     */
    private String issueType;

    /**
     * Total issue size in INR Crores.
     */
    private Double issueSizeCr;

    /**
     * Industry sector classification (e.g., "Plastic Products", "e-Commerce").
     */
    private String industry;

    /**
     * Minimum floor price of the price band (INR).
     */
    private Double minimumPrice;

    /**
     * Maximum cap price of the price band (INR).
     */
    private Double maximumPrice;

    /**
     * Bidding start date (Format: YYYY-MM-DD).
     */
    private String biddingStartDate;

    /**
     * Bidding end date (Format: YYYY-MM-DD).
     */
    private String biddingEndDate;

    /**
     * Overall subscription multiplier string (e.g., "0.24", "6.62").
     */
    private String totalSubscription;

    /**
     * List of eligible investor category codes (e.g., IND, HNI, EMP, QIB).
     */
    private List<AsraxInvestorCategoryDto> eligibleInvestors;
}
