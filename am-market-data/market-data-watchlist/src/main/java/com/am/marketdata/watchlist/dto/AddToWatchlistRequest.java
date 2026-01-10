package com.am.marketdata.watchlist.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class AddToWatchlistRequest {
    @NotBlank(message = "Symbol is required")
    private String symbol;
}
