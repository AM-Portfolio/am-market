package com.am.marketdata.common.model.ipo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Unified ASRAX domain DTO representing full 100% granular detail for a specific IPO.
 * Extends basic listing parameters with daily bidding hours, lot sizes, price parameters, RHP/DRHP document links,
 * schedule timeline, and registrar contacts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsraxIpoDetailsDto {

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
     * Daily bidding opening time (Format: HH:MM:SS, e.g., "10:00:00").
     */
    private String dailyStartTime;

    /**
     * Daily bidding closing time (Format: HH:MM:SS, e.g., "17:00:00").
     */
    private String dailyEndTime;

    /**
     * Equity face value per share (INR).
     */
    private Double faceValue;

    /**
     * Bidding price tick size (INR).
     */
    private Double tickSize;

    /**
     * Shares per lot.
     */
    private Integer lotSize;

    /**
     * Minimum quantity of shares required for retail application.
     */
    private Integer minimumQuantity;

    /**
     * Cut-off price for retail individual bidding (INR).
     */
    private Double cutOffPrice;

    /**
     * Actual stock listing price post-allotment (INR).
     */
    private Double listingPrice;

    /**
     * Target exchanges where stock will be listed (e.g., "BSE", "BSE,NSE").
     */
    private String listingExchange;

    /**
     * Red Herring Prospectus (RHP) PDF document URL.
     */
    private String rhpUrl;

    /**
     * Draft Red Herring Prospectus (DRHP) PDF document URL.
     */
    private String drhpUrl;

    /**
     * Key event schedule timeline.
     */
    private AsraxIpoTimelineDto timeline;

    /**
     * Registrar contact info and web portal details.
     */
    private AsraxIpoRegistrarDto registrarInfo;

    /**
     * Overall subscription multiplier string (e.g., "0.24", "6.62").
     */
    private String totalSubscription;

    /**
     * List of eligible investor category codes (e.g., IND, HNI, EMP, QIB).
     */
    private List<AsraxInvestorCategoryDto> eligibleInvestors;
}
