package com.am.marketdata.watchlist.repository;

import com.am.marketdata.watchlist.entity.WatchlistItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistRepository extends MongoRepository<WatchlistItem, String> {

    List<WatchlistItem> findByUserIdOrderByDisplayOrderAsc(String userId);

    Optional<WatchlistItem> findByUserIdAndSymbol(String userId, String symbol);

    void deleteByUserIdAndSymbol(String userId, String symbol);

    boolean existsByUserIdAndSymbol(String userId, String symbol);

    long countByUserId(String userId);
}
