package com.am.marketdata.watchlist.controller;

import com.am.marketdata.watchlist.dto.*;
import com.am.marketdata.watchlist.service.WatchlistService;
import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller exposing Multi-Watchlist management endpoints.
 * Handles custom watchlist creation, stock management, limits, and containment checks.
 */
@Slf4j
@RestController
@RequestMapping("/v1/watchlists")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class WatchlistController {

    private final FlowLogger flowLogger;
    private final WatchlistService watchlistService;

    /**
     * Retrieves all custom watchlists owned by the user (max 5).
     */
    @GetMapping
    public ResponseEntity<List<WatchlistDto>> getUserWatchlists(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "demo-user") String userId) {
        try (FlowSpan span = flowLogger.start("watchlists.get_all", "userId", userId)) {
            try {
                List<WatchlistDto> watchlists = watchlistService.getUserWatchlists(userId);
                flowLogger.complete(span, "count", watchlists.size());
                return ResponseEntity.ok(watchlists);
            } catch (Exception e) {
                log.error("Error fetching watchlists for user {}", userId, e);
                flowLogger.fail(span, e);
                throw e;
            }
        }
    }

    /**
     * Creates a new custom named watchlist for the user (enforces max 5 watchlist limit).
     */
    @PostMapping
    public ResponseEntity<WatchlistDto> createWatchlist(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "demo-user") String userId,
            @Valid @RequestBody CreateWatchlistRequest request) {
        try (FlowSpan span = flowLogger.start("watchlists.create", "userId", userId, "name", request.getName())) {
            try {
                WatchlistDto created = watchlistService.createWatchlist(userId, request.getName());
                flowLogger.complete(span);
                return ResponseEntity.status(HttpStatus.CREATED).body(created);
            } catch (Exception e) {
                log.error("Error creating watchlist for user {} name={}", userId, request.getName(), e);
                flowLogger.fail(span, e);
                throw e;
            }
        }
    }

    /**
     * Renames an existing custom watchlist owned by the user.
     */
    @PutMapping("/{watchlistId}")
    public ResponseEntity<WatchlistDto> renameWatchlist(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "demo-user") String userId,
            @PathVariable String watchlistId,
            @Valid @RequestBody CreateWatchlistRequest request) {
        try (FlowSpan span = flowLogger.start("watchlists.rename", "userId", userId, "watchlistId", watchlistId)) {
            try {
                WatchlistDto updated = watchlistService.renameWatchlist(userId, watchlistId, request.getName());
                flowLogger.complete(span);
                return ResponseEntity.ok(updated);
            } catch (Exception e) {
                log.error("Error renaming watchlist {} for user {}", watchlistId, userId, e);
                flowLogger.fail(span, e);
                throw e;
            }
        }
    }

    /**
     * Deletes a custom watchlist owned by the user (default watchlist cannot be deleted).
     */
    @DeleteMapping("/{watchlistId}")
    public ResponseEntity<Void> deleteWatchlist(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "demo-user") String userId,
            @PathVariable String watchlistId) {
        try (FlowSpan span = flowLogger.start("watchlists.delete", "userId", userId, "watchlistId", watchlistId)) {
            try {
                watchlistService.deleteWatchlist(userId, watchlistId);
                flowLogger.complete(span);
                return ResponseEntity.noContent().build();
            } catch (Exception e) {
                log.error("Error deleting watchlist {} for user {}", watchlistId, userId, e);
                flowLogger.fail(span, e);
                throw e;
            }
        }
    }

    /**
     * Retrieves all stock items inside a specific watchlist owned by the user.
     */
    @GetMapping("/{watchlistId}/items")
    public ResponseEntity<List<WatchlistItemDto>> getWatchlistItems(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "demo-user") String userId,
            @PathVariable String watchlistId) {
        try (FlowSpan span = flowLogger.start("watchlists.get_items", "userId", userId, "watchlistId", watchlistId)) {
            try {
                List<WatchlistItemDto> items = watchlistService.getWatchlistItems(userId, watchlistId);
                flowLogger.complete(span, "itemCount", items.size());
                return ResponseEntity.ok(items);
            } catch (Exception e) {
                log.error("Error fetching items in watchlist {} for user {}", watchlistId, userId, e);
                flowLogger.fail(span, e);
                throw e;
            }
        }
    }

    /**
     * Adds a stock symbol to a specific watchlist (enforces max 50 stock capacity limit).
     */
    @PostMapping("/{watchlistId}/items")
    public ResponseEntity<WatchlistItemDto> addStockToWatchlist(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "demo-user") String userId,
            @PathVariable String watchlistId,
            @Valid @RequestBody AddToWatchlistRequest request) {
        try (FlowSpan span = flowLogger.start("watchlists.add_item", "userId", userId, "watchlistId", watchlistId, "symbol", request.getSymbol())) {
            try {
                WatchlistItemDto item = watchlistService.addStockToWatchlist(userId, watchlistId, request.getSymbol());
                flowLogger.complete(span);
                return ResponseEntity.status(HttpStatus.CREATED).body(item);
            } catch (Exception e) {
                log.error("Error adding symbol {} to watchlist {} for user {}", request.getSymbol(), watchlistId, userId, e);
                flowLogger.fail(span, e);
                throw e;
            }
        }
    }

    /**
     * Removes a stock symbol from a specific watchlist owned by the user.
     */
    @DeleteMapping("/{watchlistId}/items/{symbol}")
    public ResponseEntity<Void> removeStockFromWatchlist(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "demo-user") String userId,
            @PathVariable String watchlistId,
            @PathVariable String symbol) {
        try (FlowSpan span = flowLogger.start("watchlists.remove_item", "userId", userId, "watchlistId", watchlistId, "symbol", symbol)) {
            try {
                watchlistService.removeStockFromWatchlist(userId, watchlistId, symbol);
                flowLogger.complete(span);
                return ResponseEntity.noContent().build();
            } catch (Exception e) {
                log.error("Error removing symbol {} from watchlist {} for user {}", symbol, watchlistId, userId, e);
                flowLogger.fail(span, e);
                throw e;
            }
        }
    }

    /**
     * Checks stock symbol containment status across all user watchlists.
     * Used by the "Add to Watchlist" popup modal in the UI.
     */
    @GetMapping("/check/{symbol}")
    public ResponseEntity<List<WatchlistCheckStatusDto>> checkSymbolAcrossWatchlists(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "demo-user") String userId,
            @PathVariable String symbol) {
        try (FlowSpan span = flowLogger.start("watchlists.check_symbol", "userId", userId, "symbol", symbol)) {
            try {
                List<WatchlistCheckStatusDto> statuses = watchlistService.checkSymbolAcrossWatchlists(userId, symbol);
                flowLogger.complete(span);
                return ResponseEntity.ok(statuses);
            } catch (Exception e) {
                log.error("Error checking symbol {} across watchlists for user {}", symbol, userId, e);
                flowLogger.fail(span, e);
                throw e;
            }
        }
    }
}
