package com.am.marketdata.watchlist.repository;

import com.am.marketdata.watchlist.entity.WatchlistItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Mongo repository for {@link WatchlistItem} entries.
 * Enforces scoping by 'watchlistId' and 'userId' for multi-watchlist privacy.
 */
@Repository
public interface WatchlistItemRepository extends MongoRepository<WatchlistItem, String> {

    /**
     * Gets all stock items inside a specific watchlist ordered by position.
     */
    List<WatchlistItem> findByWatchlistIdOrderByDisplayOrderAsc(String watchlistId);

    /**
     * Checks if a symbol exists inside a specific watchlist.
     */
    boolean existsByWatchlistIdAndSymbol(String watchlistId, String symbol);

    /**
     * Finds a specific item entry by watchlist ID and symbol.
     */
    Optional<WatchlistItem> findByWatchlistIdAndSymbol(String watchlistId, String symbol);

    /**
     * Counts items in a watchlist to enforce the maximum 50-stock limit.
     */
    long countByWatchlistId(String watchlistId);

    /**
     * Deletes a stock symbol from a specific watchlist.
     */
    void deleteByWatchlistIdAndSymbol(String watchlistId, String symbol);

    /**
     * Deletes all items associated with a watchlist when that watchlist is deleted.
     */
    void deleteByWatchlistId(String watchlistId);

    /**
     * Backward compatibility helper to check items by userId.
     */
    List<WatchlistItem> findByUserIdOrderByDisplayOrderAsc(String userId);
}
