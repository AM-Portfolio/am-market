package com.am.marketdata.watchlist.service;

import com.am.marketdata.watchlist.dto.WatchlistItemDto;
import com.am.marketdata.watchlist.entity.WatchlistItem;
import com.am.marketdata.watchlist.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;

    public List<WatchlistItemDto> getWatchlist(String userId) {
        log.info("Getting watchlist for user: {}", userId);
        return watchlistRepository.findByUserIdOrderByDisplayOrderAsc(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public WatchlistItemDto addToWatchlist(String userId, String symbol) {
        log.info("Adding symbol {} to watchlist for user {}", symbol, userId);

        // Check if already exists
        if (watchlistRepository.existsByUserIdAndSymbol(userId, symbol)) {
            throw new IllegalArgumentException("Symbol already in watchlist");
        }

        // Get next display order
        long count = watchlistRepository.countByUserId(userId);

        WatchlistItem item = WatchlistItem.builder()
                .userId(userId)
                .symbol(symbol.toUpperCase())
                .displayOrder((int) count)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        WatchlistItem saved = watchlistRepository.save(item);
        log.info("Added symbol {} to watchlist for user {}", symbol, userId);
        return toDto(saved);
    }

    public void removeFromWatchlist(String userId, String symbol) {
        log.info("Removing symbol {} from watchlist for user {}", symbol, userId);
        watchlistRepository.deleteByUserIdAndSymbol(userId, symbol.toUpperCase());
    }

    public void reorderWatchlist(String userId, List<String> symbols) {
        log.info("Reordering watchlist for user {}", userId);

        for (int i = 0; i < symbols.size(); i++) {
            final int order = i; // Make effectively final for lambda
            String symbol = symbols.get(i);
            watchlistRepository.findByUserIdAndSymbol(userId, symbol.toUpperCase())
                    .ifPresent(item -> {
                        item.setDisplayOrder(order);
                        item.setUpdatedAt(LocalDateTime.now());
                        watchlistRepository.save(item);
                    });
        }
    }

    public boolean isInWatchlist(String userId, String symbol) {
        return watchlistRepository.existsByUserIdAndSymbol(userId, symbol.toUpperCase());
    }

    private WatchlistItemDto toDto(WatchlistItem item) {
        return WatchlistItemDto.builder()
                .id(item.getId())
                .symbol(item.getSymbol())
                .displayOrder(item.getDisplayOrder())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
