package com.am.marketdata.watchlist.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class AddToWatchlistRequest {
    @NotBlank(message = "Symbol is required")
    private String symbol;

    /**
     * Target exchange for the stock (e.g., "NSE", "BSE", "NSE_FO").
     * When omitted, the service uses an exchange prefix in {@code symbol}, or NSE.
     */
    private String exchange;
}
