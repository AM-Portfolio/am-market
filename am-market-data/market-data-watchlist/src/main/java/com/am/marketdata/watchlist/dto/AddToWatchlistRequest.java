package com.am.marketdata.watchlist.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class AddToWatchlistRequest {
    @NotBlank(message = "Symbol is required")
    private String symbol;

    /**
     * Target exchange for the stock (e.g., "NSE", "BSE", "NSE_FO").
     * Defaults to "NSE" when not explicitly supplied.
     */
    private String exchange = "NSE";
}
