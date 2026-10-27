package com.am.marketdata.common.model.ipo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Single calendar entry representing an IPO with a scheduled or actual listing event on a specific date.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsraxIpoCalendarEntryDto {

    /**
     * Stable Upstox IPO slug identifier (e.g., "sfc-environmental-technologies-limited-ipo").
     */
    private String id;

    /**
     * Display name of the issuing company.
     */
    private String companyName;

    /**
     * Calendar display status (UPCOMING or LISTED).
     */
    private IpoCalendarDisplayStatus status;

    /**
     * ISO-8601 listing date formatted as YYYY-MM-DD.
     */
    private String listingDate;

    /**
     * Listing price in INR, nullable before listing or if omitted by provider.
     */
    private Double listingPrice;

    /**
     * Listing exchange name(s), comma-separated (e.g., "BSE,NSE"), nullable if omitted.
     */
    private String listingExchange;
}
