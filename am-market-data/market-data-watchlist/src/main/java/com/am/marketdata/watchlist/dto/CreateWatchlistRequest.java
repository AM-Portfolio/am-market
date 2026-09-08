package com.am.marketdata.watchlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating a new custom named watchlist.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWatchlistRequest {

    @NotBlank(message = "Watchlist name is required")
    @Size(min = 1, max = 50, message = "Watchlist name must be between 1 and 50 characters")
    private String name;
}
