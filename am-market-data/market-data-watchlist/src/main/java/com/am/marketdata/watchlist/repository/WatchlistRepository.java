package com.am.marketdata.watchlist.repository;

import com.am.marketdata.watchlist.entity.Watchlist;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access repository for {@link Watchlist} documents.
 * Every query strictly enforces user-level isolation by requiring 'userId'.
 */
@Repository
public interface WatchlistRepository extends MongoRepository<Watchlist, String> {

    /**
     * Retrieves all watchlists owned by a user, ordered by display position.
     */
    List<Watchlist> findByUserIdOrderByDisplayOrderAsc(String userId);

    /**
     * Finds a specific watchlist by ID while strictly verifying ownership by userId.
     */
    Optional<Watchlist> findByUserIdAndId(String userId, String id);

    /**
     * Finds the default auto-seeded watchlist for a user.
     */
    Optional<Watchlist> findByUserIdAndIsDefaultTrue(String userId);

    /**
     * Checks if a user already has a watchlist with the given name (case-insensitive check handled in service).
     */
    boolean existsByUserIdAndName(String userId, String name);

    /**
     * Counts total watchlists created by a user to enforce the maximum 5-watchlist limit.
     */
    long countByUserId(String userId);

    /**
     * Deletes a specific watchlist owned by a user.
     */
    void deleteByUserIdAndId(String userId, String id);
}
