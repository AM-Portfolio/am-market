package com.am.marketdata.watchlist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload returning containment status of a stock symbol across all watchlists owned by a user.
 * Used by the "Add to Watchlist" popup modal in the UI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistCheckStatusDto {

    private String watchlistId;
    private String name;
    private Boolean isDefault;
    private Integer itemCount;
    private Boolean containsSymbol;
}
