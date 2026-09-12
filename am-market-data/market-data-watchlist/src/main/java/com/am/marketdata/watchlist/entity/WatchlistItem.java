package com.am.marketdata.watchlist.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * MongoDB Document representing a stock item added to a specific Watchlist.
 */
@Document(collection = "watchlist_items")
@CompoundIndexes({
        // Enforces unique stock symbol per exchange inside a single watchlist
        @CompoundIndex(name = "idx_watchlist_item_unique", def = "{'watchlistId': 1, 'symbol': 1, 'exchange': 1}", unique = true),
        // Fast lookups per user, symbol, and exchange across all user watchlists
        @CompoundIndex(name = "idx_user_symbol", def = "{'userId': 1, 'symbol': 1, 'exchange': 1}")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistItem {

    @Id
    private String id;

    /**
     * ID of the parent Watchlist container.
     */
    @Field("watchlist_id")
    private String watchlistId;

    /**
     * User ID owning this item (for user-level isolation).
     */
    @Field("user_id")
    private String userId;

    /**
     * Ticker or trading symbol (e.g. "TCS", "HDFCBANK").
     */
    private String symbol;

    /**
     * Exchange for this stock item (e.g. "NSE", "BSE", "NSE_FO").
     * Defaults to "NSE" for backward compatibility with existing items.
     */
    @Field("exchange")
    @Builder.Default
    private String exchange = "NSE";

    /**
     * Display order sequence inside the specific watchlist.
     */
    @Field("display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;
}
