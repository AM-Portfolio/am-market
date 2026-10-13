package com.am.marketdata.common.model.ipo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified ASRAX domain DTO representing the key event schedule timeline of an IPO.
 * Tracks dates for pre-application, official bidding window, allotment, refunds, exchange listing, and mandate expiry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsraxIpoTimelineDto {

    /**
     * Date when pre-applications open (Format: YYYY-MM-DD).
     */
    private String preApplyStartDate;

    /**
     * Date when official bidding opens (Format: YYYY-MM-DD).
     */
    private String applicationStartDate;

    /**
     * Date when official bidding closes (Format: YYYY-MM-DD).
     */
    private String applicationEndDate;

    /**
     * Date when allotment process initiates (Format: YYYY-MM-DD).
     */
    private String allotmentStartDate;

    /**
     * Date when final allotment status is declared (Format: YYYY-MM-DD).
     */
    private String allotmentDate;

    /**
     * Date when un-allotted funds refund is initiated (Format: YYYY-MM-DD).
     */
    private String refundInitiationDate;

    /**
     * Date when stock is officially listed on stock exchange (Format: YYYY-MM-DD).
     */
    private String listingDate;

    /**
     * Date when UPI mandate block expires (Format: YYYY-MM-DD).
     */
    private String mandateEndDate;
}
