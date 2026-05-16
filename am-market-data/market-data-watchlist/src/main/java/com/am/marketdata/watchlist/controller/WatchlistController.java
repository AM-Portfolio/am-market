package com.am.marketdata.watchlist.controller;

import com.am.marketdata.watchlist.dto.AddToWatchlistRequest;
import com.am.marketdata.watchlist.dto.ReorderWatchlistRequest;
import com.am.marketdata.watchlist.dto.WatchlistItemDto;
import com.am.marketdata.watchlist.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/watchlist")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class WatchlistController {

    private final FlowLogger flowLogger;
    private final WatchlistService watchlistService;

    @GetMapping
    public ResponseEntity<List<WatchlistItemDto>> getWatchlist(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "demo-user") String userId) {
        try (FlowSpan span = flowLogger.start("watchlist.get", "userId", userId)) {
            try {
                List<WatchlistItemDto> items = watchlistService.getWatchlist(userId);
                flowLogger.complete(span, "itemCount", items.size());
                return ResponseEntity.ok(items);
            } catch (Exception e) {
                log.error("Error fetching watchlist for {}", userId, e);
                flowLogger.fail(span, e);
                throw e;
            }
        }
    }

    @PostMapping
    public ResponseEntity<WatchlistItemDto> addToWatchlist(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "demo-user") String userId,
            @Valid @RequestBody AddToWatchlistRequest request) {
        try (FlowSpan span = flowLogger.start("watchlist.add", "userId", userId, "symbol", request.getSymbol())) {
            try {
                WatchlistItemDto result = watchlistService.addToWatchlist(userId, request.getSymbol());
                flowLogger.complete(span);
                return ResponseEntity.ok(result);
            } catch (Exception e) {
                log.error("Error adding to watchlist for {} symbol={}", userId, request.getSymbol(), e);
                flowLogger.fail(span, e);
                throw e;
            }
        }
    }

    @DeleteMapping("/{symbol}")
    public ResponseEntity<Void> removeFromWatchlist(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "demo-user") String userId,
            @PathVariable String symbol) {
        try (FlowSpan span = flowLogger.start("watchlist.remove", "userId", userId, "symbol", symbol)) {
            try {
                watchlistService.removeFromWatchlist(userId, symbol);
                flowLogger.complete(span);
                return ResponseEntity.noContent().build();
            } catch (Exception e) {
                log.error("Error removing from watchlist for {} symbol={}", userId, symbol, e);
                flowLogger.fail(span, e);
                throw e;
            }
        }
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderWatchlist(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "demo-user") String userId,
            @RequestBody ReorderWatchlistRequest request) {
        try (FlowSpan span = flowLogger.start("watchlist.reorder", "userId", userId, "symbolsCount",
                request.getSymbols() != null ? request.getSymbols().size() : 0)) {
            try {
                watchlistService.reorderWatchlist(userId, request.getSymbols());
                flowLogger.complete(span);
                return ResponseEntity.ok().build();
            } catch (Exception e) {
                log.error("Error reordering watchlist for {}", userId, e);
                flowLogger.fail(span, e);
                throw e;
            }
        }
    }

    @GetMapping("/check/{symbol}")
    public ResponseEntity<Map<String, Boolean>> checkInWatchlist(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "demo-user") String userId,
            @PathVariable String symbol) {
        boolean inWatchlist = watchlistService.isInWatchlist(userId, symbol);
        return ResponseEntity.ok(Map.of("inWatchlist", inWatchlist));
    }
}

