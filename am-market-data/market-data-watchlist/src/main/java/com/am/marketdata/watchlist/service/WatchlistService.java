package com.am.marketdata.watchlist.service;

import com.am.marketdata.watchlist.dto.*;
import com.am.marketdata.watchlist.entity.Watchlist;
import com.am.marketdata.watchlist.entity.WatchlistItem;
import com.am.marketdata.watchlist.repository.WatchlistItemRepository;
import com.am.marketdata.watchlist.repository.WatchlistRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service managing user-isolated multi-watchlist lifecycle, stock item additions,
 * capacity checks (max 5 watchlists, max 50 stocks/list), and default list auto-seeding.
 */
@Service
@Slf4j
public class WatchlistService {

    public static final String DEFAULT_ACTIVE_SET_KEY = "market:active-symbols";
    public static final String DEFAULT_WATCHLIST_NAME = "My Watch List";
    public static final int DEFAULT_MAX_WATCHLISTS_PER_USER = 5;
    public static final int DEFAULT_MAX_STOCKS_PER_WATCHLIST = 50;

    private final WatchlistRepository watchlistRepository;
    private final WatchlistItemRepository watchlistItemRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final String activeSetRedisKey;
    private final int maxWatchlistsPerUser;
    private final int maxStocksPerWatchlist;
    private final String defaultWatchlistName;

    public WatchlistService(
            WatchlistRepository watchlistRepository,
            WatchlistItemRepository watchlistItemRepository,
            @Nullable StringRedisTemplate stringRedisTemplate,
            @Value("${market.active-symbols.redis-key:" + DEFAULT_ACTIVE_SET_KEY + "}") String activeSetRedisKey,
            @Value("${market.watchlist.max-per-user:" + DEFAULT_MAX_WATCHLISTS_PER_USER + "}") int maxWatchlistsPerUser,
            @Value("${market.watchlist.max-stocks-per-list:" + DEFAULT_MAX_STOCKS_PER_WATCHLIST + "}") int maxStocksPerWatchlist,
            @Value("${market.watchlist.default-name:" + DEFAULT_WATCHLIST_NAME + "}") String defaultWatchlistName) {
        this.watchlistRepository = watchlistRepository;
        this.watchlistItemRepository = watchlistItemRepository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.activeSetRedisKey = activeSetRedisKey;
        this.maxWatchlistsPerUser = maxWatchlistsPerUser;
        this.maxStocksPerWatchlist = maxStocksPerWatchlist;
        this.defaultWatchlistName = defaultWatchlistName;
    }

    /**
     * Gets or seeds the default "My Watch List" for a user.
     * Ensures every user has at least one default watchlist container.
     */
    public Watchlist getOrCreateDefaultWatchlist(String userId) {
        return watchlistRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElseGet(() -> {
                    log.info("Auto-seeding default watchlist '{}' for user {}", defaultWatchlistName, userId);
                    Watchlist defaultList = Watchlist.builder()
                            .userId(userId)
                            .name(defaultWatchlistName)
                            .isDefault(true)
                            .displayOrder(0)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return watchlistRepository.save(defaultList);
                });
    }

    /**
     * Retrieves all custom watchlists owned by a user with stock counts.
     * Automatically seeds default watchlist if user has none.
     */
    public List<WatchlistDto> getUserWatchlists(String userId) {
        log.info("Fetching watchlists for user {}", userId);
        getOrCreateDefaultWatchlist(userId); // Ensure default list exists

        List<Watchlist> watchlists = watchlistRepository.findByUserIdOrderByDisplayOrderAsc(userId);
        return watchlists.stream()
                .map(wl -> {
                    int count = (int) watchlistItemRepository.countByWatchlistId(wl.getId());
                    return toWatchlistDto(wl, count);
                })
                .collect(Collectors.toList());
    }

    /**
     * Creates a new custom named watchlist for a user.
     * Enforces strict limit of maximum 5 watchlists per user.
     */
    public WatchlistDto createWatchlist(String userId, String name) {
        String trimmedName = name != null ? name.trim() : "";
        log.info("Creating custom watchlist '{}' for user {}", trimmedName, userId);

        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Watchlist name cannot be empty");
        }

        // Limit Check: Maximum watchlists per user
        long currentCount = watchlistRepository.countByUserId(userId);
        if (currentCount >= maxWatchlistsPerUser) {
            log.warn("User {} exceeded maximum limit of {} watchlists", userId, maxWatchlistsPerUser);
            throw new IllegalArgumentException("Maximum limit of " + maxWatchlistsPerUser + " watchlists reached");
        }

        // Check for duplicate name for this user
        if (watchlistRepository.existsByUserIdAndName(userId, trimmedName)) {
            throw new IllegalArgumentException("A watchlist named '" + trimmedName + "' already exists");
        }

        Watchlist watchlist = Watchlist.builder()
                .userId(userId)
                .name(trimmedName)
                .isDefault(false)
                .displayOrder((int) currentCount)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Watchlist saved = watchlistRepository.save(watchlist);
        return toWatchlistDto(saved, 0);
    }

    /**
     * Renames an existing custom watchlist owned by the user.
     */
    public WatchlistDto renameWatchlist(String userId, String watchlistId, String newName) {
        String trimmedName = newName != null ? newName.trim() : "";
        log.info("Renaming watchlist {} for user {} to '{}'", watchlistId, userId, trimmedName);

        Watchlist watchlist = watchlistRepository.findByUserIdAndId(userId, watchlistId)
                .orElseThrow(() -> new IllegalArgumentException("Watchlist not found or access denied"));

        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Watchlist name cannot be empty");
        }

        if (!watchlist.getName().equalsIgnoreCase(trimmedName) && watchlistRepository.existsByUserIdAndName(userId, trimmedName)) {
            throw new IllegalArgumentException("A watchlist named '" + trimmedName + "' already exists");
        }

        watchlist.setName(trimmedName);
        watchlist.setUpdatedAt(LocalDateTime.now());
        Watchlist updated = watchlistRepository.save(watchlist);

        int count = (int) watchlistItemRepository.countByWatchlistId(updated.getId());
        return toWatchlistDto(updated, count);
    }

    /**
     * Deletes a custom watchlist and all contained stock items.
     * Prevents deletion of the default "My Watch List".
     */
    public void deleteWatchlist(String userId, String watchlistId) {
        log.info("Deleting watchlist {} for user {}", watchlistId, userId);

        Watchlist watchlist = watchlistRepository.findByUserIdAndId(userId, watchlistId)
                .orElseThrow(() -> new IllegalArgumentException("Watchlist not found or access denied"));

        if (Boolean.TRUE.equals(watchlist.getIsDefault())) {
            throw new IllegalArgumentException("Default watchlist cannot be deleted");
        }

        watchlistItemRepository.deleteByWatchlistId(watchlistId);
        watchlistRepository.deleteByUserIdAndId(userId, watchlistId);
    }

    /**
     * Gets all stock items inside a specific watchlist owned by the user.
     */
    public List<WatchlistItemDto> getWatchlistItems(String userId, String watchlistId) {
        log.info("Getting items in watchlist {} for user {}", watchlistId, userId);

        // Ownership verification
        watchlistRepository.findByUserIdAndId(userId, watchlistId)
                .orElseThrow(() -> new IllegalArgumentException("Watchlist not found or access denied"));

        return watchlistItemRepository.findByWatchlistIdOrderByDisplayOrderAsc(watchlistId)
                .stream()
                .map(this::toItemDto)
                .collect(Collectors.toList());
    }

    /**
     * Adds a stock symbol to a specific watchlist owned by the user.
     * Enforces strict capacity limit of maximum stocks per watchlist.
     */
    public WatchlistItemDto addStockToWatchlist(String userId, String watchlistId, String symbol) {
        String cleanSymbol = symbol != null ? symbol.trim().toUpperCase(Locale.ROOT) : "";
        log.info("Adding symbol {} to watchlist {} for user {}", cleanSymbol, watchlistId, userId);

        // Ownership verification
        Watchlist watchlist = watchlistRepository.findByUserIdAndId(userId, watchlistId)
                .orElseThrow(() -> new IllegalArgumentException("Watchlist not found or access denied"));

        // Capacity Check: Max stocks per watchlist
        long itemCount = watchlistItemRepository.countByWatchlistId(watchlistId);
        if (itemCount >= maxStocksPerWatchlist) {
            log.warn("Watchlist {} exceeded maximum capacity of {} stocks", watchlistId, maxStocksPerWatchlist);
            throw new IllegalArgumentException("Watchlist has reached maximum capacity of " + maxStocksPerWatchlist + " stocks");
        }

        if (watchlistItemRepository.existsByWatchlistIdAndSymbol(watchlistId, cleanSymbol)) {
            throw new IllegalArgumentException("Symbol '" + cleanSymbol + "' is already in this watchlist");
        }

        WatchlistItem item = WatchlistItem.builder()
                .watchlistId(watchlist.getId())
                .userId(userId)
                .symbol(cleanSymbol)
                .displayOrder((int) itemCount)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        WatchlistItem saved = watchlistItemRepository.save(item);
        publishActiveSymbol(cleanSymbol);
        return toItemDto(saved);
    }

    /**
     * Removes a stock symbol from a specific watchlist owned by the user.
     */
    public void removeStockFromWatchlist(String userId, String watchlistId, String symbol) {
        String cleanSymbol = symbol != null ? symbol.trim().toUpperCase(Locale.ROOT) : "";
        log.info("Removing symbol {} from watchlist {} for user {}", cleanSymbol, watchlistId, userId);

        watchlistRepository.findByUserIdAndId(userId, watchlistId)
                .orElseThrow(() -> new IllegalArgumentException("Watchlist not found or access denied"));

        watchlistItemRepository.deleteByWatchlistIdAndSymbol(watchlistId, cleanSymbol);
    }

    /**
     * Returns containment status of a stock symbol across all watchlists owned by the user.
     * Crucial for powering the UI "Add to Watchlist" popup modal.
     */
    public List<WatchlistCheckStatusDto> checkSymbolAcrossWatchlists(String userId, String symbol) {
        String cleanSymbol = symbol != null ? symbol.trim().toUpperCase(Locale.ROOT) : "";
        log.info("Checking symbol {} containment across watchlists for user {}", cleanSymbol, userId);

        List<WatchlistDto> watchlists = getUserWatchlists(userId);
        List<WatchlistCheckStatusDto> statusList = new ArrayList<>();

        for (WatchlistDto wl : watchlists) {
            boolean contains = watchlistItemRepository.existsByWatchlistIdAndSymbol(wl.getId(), cleanSymbol);
            statusList.add(WatchlistCheckStatusDto.builder()
                    .watchlistId(wl.getId())
                    .name(wl.getName())
                    .isDefault(wl.getIsDefault())
                    .itemCount(wl.getItemCount())
                    .containsSymbol(contains)
                    .build());
        }

        return statusList;
    }

    // --- Legacy Single-Watchlist Compatibility Helpers ---

    public List<WatchlistItemDto> getWatchlist(String userId) {
        Watchlist defaultList = getOrCreateDefaultWatchlist(userId);
        return getWatchlistItems(userId, defaultList.getId());
    }

    public WatchlistItemDto addToWatchlist(String userId, String symbol) {
        Watchlist defaultList = getOrCreateDefaultWatchlist(userId);
        return addStockToWatchlist(userId, defaultList.getId(), symbol);
    }

    public void removeFromWatchlist(String userId, String symbol) {
        Watchlist defaultList = getOrCreateDefaultWatchlist(userId);
        removeStockFromWatchlist(userId, defaultList.getId(), symbol);
    }

    public boolean isInWatchlist(String userId, String symbol) {
        Watchlist defaultList = getOrCreateDefaultWatchlist(userId);
        return watchlistItemRepository.existsByWatchlistIdAndSymbol(defaultList.getId(), symbol.toUpperCase());
    }

    // --- Private Helper Methods ---

    private WatchlistDto toWatchlistDto(Watchlist watchlist, int itemCount) {
        return WatchlistDto.builder()
                .id(watchlist.getId())
                .userId(watchlist.getUserId())
                .name(watchlist.getName())
                .isDefault(watchlist.getIsDefault())
                .displayOrder(watchlist.getDisplayOrder())
                .itemCount(itemCount)
                .createdAt(watchlist.getCreatedAt())
                .updatedAt(watchlist.getUpdatedAt())
                .build();
    }

    private WatchlistItemDto toItemDto(WatchlistItem item) {
        return WatchlistItemDto.builder()
                .id(item.getId())
                .symbol(item.getSymbol())
                .displayOrder(item.getDisplayOrder())
                .createdAt(item.getCreatedAt())
                .build();
    }

    private void publishActiveSymbol(String symbol) {
        if (stringRedisTemplate == null || symbol == null || symbol.isBlank()) {
            return;
        }
        try {
            stringRedisTemplate.opsForSet().add(activeSetRedisKey, symbol.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            log.warn("Watchlist active-symbol publish failed (fail-open): {}", e.getMessage());
        }
    }
}
