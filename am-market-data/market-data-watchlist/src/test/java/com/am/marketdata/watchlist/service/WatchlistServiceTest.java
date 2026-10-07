package com.am.marketdata.watchlist.service;

import com.am.marketdata.watchlist.dto.WatchlistCheckStatusDto;
import com.am.marketdata.watchlist.dto.WatchlistDto;
import com.am.marketdata.watchlist.dto.WatchlistItemDto;
import com.am.marketdata.watchlist.entity.Watchlist;
import com.am.marketdata.watchlist.entity.WatchlistItem;
import com.am.marketdata.watchlist.repository.WatchlistItemRepository;
import com.am.marketdata.watchlist.repository.WatchlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests verifying multi-watchlist business logic, 5-watchlist limit,
 * 50-stock capacity limit, auto-seeding, and user-level isolation security.
 */
@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

    @Mock
    private WatchlistRepository watchlistRepository;

    @Mock
    private WatchlistItemRepository watchlistItemRepository;

    private WatchlistService watchlistService;

    private final String userId = "user-123";

    @BeforeEach
    void setUp() {
        watchlistService = new WatchlistService(
                watchlistRepository,
                watchlistItemRepository,
                null,
                "market:active-symbols",
                WatchlistService.DEFAULT_MAX_WATCHLISTS_PER_USER,
                WatchlistService.DEFAULT_MAX_STOCKS_PER_WATCHLIST,
                WatchlistService.DEFAULT_WATCHLIST_NAME
        );
    }

    @Test
    @DisplayName("Should auto-seed default 'My Watch List' when user has no watchlists")
    void testAutoSeedDefaultWatchlist() {
        when(watchlistRepository.findByUserIdAndIsDefaultTrue(userId)).thenReturn(Optional.empty());
        when(watchlistRepository.save(any(Watchlist.class))).thenAnswer(inv -> {
            Watchlist w = inv.getArgument(0);
            w.setId("default-wl-id");
            return w;
        });

        Watchlist defaultWl = watchlistService.getOrCreateDefaultWatchlist(userId);

        assertNotNull(defaultWl);
        assertEquals("My Watch List", defaultWl.getName());
        assertTrue(defaultWl.getIsDefault());
        verify(watchlistRepository, times(1)).save(any(Watchlist.class));
    }

    @Test
    @DisplayName("Should create custom watchlist when within 5-list limit")
    void testCreateWatchlistWithinLimit() {
        when(watchlistRepository.countByUserId(userId)).thenReturn(2L);
        when(watchlistRepository.existsByUserIdAndName(userId, "IT Giants")).thenReturn(false);
        when(watchlistRepository.save(any(Watchlist.class))).thenAnswer(inv -> {
            Watchlist w = inv.getArgument(0);
            w.setId("wl-it-giants");
            return w;
        });

        WatchlistDto dto = watchlistService.createWatchlist(userId, "IT Giants");

        assertNotNull(dto);
        assertEquals("IT Giants", dto.getName());
        assertFalse(dto.getIsDefault());
    }

    @Test
    @DisplayName("Should throw Exception when attempting to create 6th watchlist (Exceeds max 5 limit)")
    void testCreateWatchlistExceedsLimit() {
        when(watchlistRepository.countByUserId(userId)).thenReturn(5L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                watchlistService.createWatchlist(userId, "6th Watchlist")
        );

        assertTrue(ex.getMessage().contains("Maximum limit of 5 watchlists reached"));
        verify(watchlistRepository, never()).save(any(Watchlist.class));
    }

    @Test
    @DisplayName("Should add stock when watchlist is within 50-stock capacity limit")
    void testAddStockWithinCapacity() {
        Watchlist mockList = Watchlist.builder().id("wl-1").userId(userId).name("My Watch List").build();
        when(watchlistRepository.findByUserIdAndId(userId, "wl-1")).thenReturn(Optional.of(mockList));
        when(watchlistItemRepository.countByWatchlistId("wl-1")).thenReturn(10L);
        when(watchlistItemRepository.existsByWatchlistIdAndSymbolAndExchange("wl-1", "TCS", "NSE")).thenReturn(false);
        when(watchlistItemRepository.save(any(WatchlistItem.class))).thenAnswer(inv -> {
            WatchlistItem item = inv.getArgument(0);
            item.setId("item-1");
            return item;
        });

        WatchlistItemDto dto = watchlistService.addStockToWatchlist(userId, "wl-1", "TCS");

        assertNotNull(dto);
        assertEquals("TCS", dto.getSymbol());
        assertEquals("NSE", dto.getExchange());
    }

    @Test
    @DisplayName("Should allow same stock symbol on different exchanges in the same watchlist")
    void testAddSameSymbolDifferentExchanges() {
        Watchlist mockList = Watchlist.builder().id("wl-1").userId(userId).name("My Watch List").build();
        when(watchlistRepository.findByUserIdAndId(userId, "wl-1")).thenReturn(Optional.of(mockList));
        when(watchlistItemRepository.countByWatchlistId("wl-1")).thenReturn(1L);
        when(watchlistItemRepository.existsByWatchlistIdAndSymbolAndExchange("wl-1", "RELIANCE", "BSE")).thenReturn(false);
        when(watchlistItemRepository.save(any(WatchlistItem.class))).thenAnswer(inv -> {
            WatchlistItem item = inv.getArgument(0);
            item.setId("item-bse");
            return item;
        });

        WatchlistItemDto dto = watchlistService.addStockToWatchlist(userId, "wl-1", "RELIANCE", "BSE");

        assertNotNull(dto);
        assertEquals("RELIANCE", dto.getSymbol());
        assertEquals("BSE", dto.getExchange());
    }

    @Test
    @DisplayName("Should store a repeated NSE prefix as one canonical NSE ticker")
    void testAddDoublePrefixedSymbolStoresCanonicalTicker() {
        Watchlist mockList = Watchlist.builder().id("wl-1").userId(userId).name("My Watch List").build();
        when(watchlistRepository.findByUserIdAndId(userId, "wl-1")).thenReturn(Optional.of(mockList));
        when(watchlistItemRepository.countByWatchlistId("wl-1")).thenReturn(0L);
        when(watchlistItemRepository.existsByWatchlistIdAndSymbolAndExchange("wl-1", "IDEA", "NSE")).thenReturn(false);
        when(watchlistItemRepository.save(any(WatchlistItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WatchlistItemDto dto = watchlistService.addStockToWatchlist(userId, "wl-1", "NSE:NSE:IDEA");

        assertEquals("IDEA", dto.getSymbol());
        assertEquals("NSE", dto.getExchange());
        verify(watchlistItemRepository).save(argThat(item ->
                "IDEA".equals(item.getSymbol()) && "NSE".equals(item.getExchange())));
    }

    @Test
    @DisplayName("Should reject a request exchange that conflicts with the symbol prefix")
    void testAddRejectsConflictingExchange() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                watchlistService.addStockToWatchlist(userId, "wl-1", "NSE:IDEA", "BSE"));

        assertTrue(exception.getMessage().contains("conflicts"));
        verifyNoInteractions(watchlistRepository, watchlistItemRepository);
    }

    @Test
    @DisplayName("Should throw Exception when adding 51st stock to a watchlist (Exceeds max 50 capacity)")
    void testAddStockExceedsCapacity() {
        Watchlist mockList = Watchlist.builder().id("wl-1").userId(userId).name("My Watch List").build();
        when(watchlistRepository.findByUserIdAndId(userId, "wl-1")).thenReturn(Optional.of(mockList));
        when(watchlistItemRepository.countByWatchlistId("wl-1")).thenReturn(50L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                watchlistService.addStockToWatchlist(userId, "wl-1", "INFY")
        );

        assertTrue(ex.getMessage().contains("Watchlist has reached maximum capacity of 50 stocks"));
        verify(watchlistItemRepository, never()).save(any(WatchlistItem.class));
    }

    @Test
    @DisplayName("Should accurately check symbol containment across all user watchlists")
    void testCheckSymbolAcrossWatchlists() {
        Watchlist defaultWl = Watchlist.builder().id("wl-default").userId(userId).name("My Watch List").isDefault(true).build();
        Watchlist customWl = Watchlist.builder().id("wl-it").userId(userId).name("IT Giants").isDefault(false).build();

        when(watchlistRepository.findByUserIdAndIsDefaultTrue(userId)).thenReturn(Optional.of(defaultWl));
        when(watchlistRepository.findByUserIdOrderByDisplayOrderAsc(userId)).thenReturn(List.of(defaultWl, customWl));
        when(watchlistItemRepository.existsByWatchlistIdAndSymbolAndExchange("wl-default", "TCS", "NSE")).thenReturn(true);
        when(watchlistItemRepository.existsByWatchlistIdAndSymbolAndExchange("wl-it", "TCS", "NSE")).thenReturn(false);

        List<WatchlistCheckStatusDto> statuses = watchlistService.checkSymbolAcrossWatchlists(userId, "TCS");

        assertEquals(2, statuses.size());

        WatchlistCheckStatusDto status1 = statuses.get(0);
        assertEquals("My Watch List", status1.getName());
        assertTrue(status1.getContainsSymbol());

        WatchlistCheckStatusDto status2 = statuses.get(1);
        assertEquals("IT Giants", status2.getName());
        assertFalse(status2.getContainsSymbol());
    }
}
