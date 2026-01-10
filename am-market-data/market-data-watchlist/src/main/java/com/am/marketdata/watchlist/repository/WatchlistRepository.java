package com.am.marketdata.watchlist.repository;

import com.am.marketdata.watchlist.entity.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistRepository extends JpaRepository<WatchlistItem, Long> {

    List<WatchlistItem> findByUserIdOrderByDisplayOrderAsc(String userId);

    Optional<WatchlistItem> findByUserIdAndSymbol(String userId, String symbol);

    @Modifying
    @Query("DELETE FROM WatchlistItem w WHERE w.userId = ?1 AND w.symbol = ?2")
    void deleteByUserIdAndSymbol(String userId, String symbol);

    boolean existsByUserIdAndSymbol(String userId, String symbol);

    @Query("SELECT COUNT(w) FROM WatchlistItem w WHERE w.userId = ?1")
    long countByUserId(String userId);
}
