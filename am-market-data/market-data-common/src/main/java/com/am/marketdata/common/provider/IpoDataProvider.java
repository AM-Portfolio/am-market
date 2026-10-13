package com.am.marketdata.common.provider;

import com.am.marketdata.common.model.ipo.AsraxIpoDetailsDto;
import com.am.marketdata.common.model.ipo.AsraxIpoSummaryDto;

import java.util.List;
import java.util.Optional;

/**
 * Provider-agnostic contract interface for retrieving IPO market data.
 * Implementations (e.g., Upstox, Zerodha, NSE Direct) handle third-party specific REST/Scraper calls
 * and map raw responses into unified ASRAX domain objects.
 */
public interface IpoDataProvider {

    /**
     * Unique identifier for the provider implementation (e.g., "UPSTOX", "ZERODHA").
     *
     * @return Provider name code.
     */
    String getProviderName();

    /**
     * Fetches a paginated summary list of IPOs filtered by status and issue type.
     *
     * @param status lifecycle status filter ("open", "upcoming", "closed", "listed"). Default "open".
     * @param issueType issue segment filter ("regular", "sme").
     * @param pageNumber 1-based page index.
     * @param records number of items per page.
     * @return List of unified ASRAX IPO summary objects.
     */
    List<AsraxIpoSummaryDto> getIpos(String status, String issueType, Integer pageNumber, Integer records);

    /**
     * Fetches full 100% granular detail for a specific IPO using its slug identifier.
     *
     * @param ipoId target IPO slug identifier.
     * @return Optional containing unified ASRAX IPO details, or empty if not found.
     */
    Optional<AsraxIpoDetailsDto> getIpoDetails(String ipoId);
}
