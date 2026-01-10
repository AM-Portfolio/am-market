package com.am.marketdata.watchlist.controller;

import com.am.marketdata.watchlist.dto.AddToWatchlistRequest;
import com.am.marketdata.watchlist.dto.ReorderWatchlistRequest;
import com.am.marketdata.watchlist.dto.WatchlistItemDto;
import com.am.marketdata.watchlist.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/watchlist")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class WatchlistController {

    private final WatchlistService watchlistService;

    @GetMapping
    public ResponseEntity<List<WatchlistItemDto>> getWatchlist(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "demo-user") String userId) {
        return ResponseEntity.ok(watchlistService.getWatchlist(userId));
    }

    @PostMapping
    public ResponseEntity<WatchlistItemDto> addToWatchlist(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "demo-user") String userId,
            @Valid @RequestBody AddToWatchlistRequest request) {
        return ResponseEntity.ok(watchlistService.addToWatchlist(userId, request.getSymbol()));
    }

    @DeleteMapping("/{symbol}")
    public ResponseEntity<Void> removeFromWatchlist(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "demo-user") String userId,
            @PathVariable String symbol) {
        watchlistService.removeFromWatchlist(userId, symbol);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderWatchlist(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "demo-user") String userId,
            @RequestBody ReorderWatchlistRequest request) {
        watchlistService.reorderWatchlist(userId, request.getSymbols());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check/{symbol}")
    public ResponseEntity<Map<String, Boolean>> checkInWatchlist(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "demo-user") String userId,
            @PathVariable String symbol) {
        boolean inWatchlist = watchlistService.isInWatchlist(userId, symbol);
        return ResponseEntity.ok(Map.of("inWatchlist", inWatchlist));
    }
}
