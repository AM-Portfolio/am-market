package com.am.marketdata.service.ipo;

import com.am.marketdata.common.model.ipo.AsraxIpoCalendarEntryDto;
import com.am.marketdata.common.model.ipo.AsraxIpoCalendarMonthDto;
import com.am.marketdata.common.model.ipo.AsraxIpoDetailsDto;
import com.am.marketdata.common.model.ipo.AsraxIpoSummaryDto;
import com.am.marketdata.common.model.ipo.AsraxIpoTimelineDto;
import com.am.marketdata.common.model.ipo.IpoCalendarDataFreshness;
import com.am.marketdata.common.model.ipo.IpoCalendarDisplayStatus;
import com.am.marketdata.common.provider.IpoDataProvider;
import com.am.marketdata.service.model.IpoSyncMetaDocument;
import com.am.marketdata.service.model.UpstoxIpoDocument;
import com.am.marketdata.service.redis.UpstoxIpoCacheService;
import com.am.marketdata.service.repo.IpoSyncMetaRepository;
import com.am.marketdata.service.repo.UpstoxIpoMongoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpstoxIpoServiceCalendarTest {

    @Mock
    private IpoDataProvider ipoDataProvider;

    @Mock
    private UpstoxIpoMongoRepository mongoRepository;

    @Mock
    private UpstoxIpoCacheService cacheService;

    @Mock
    private IpoSyncMetaRepository ipoSyncMetaRepository;

    private Clock fixedClock;
    private UpstoxIpoService service;

    // Fixed instant: 2026-10-15T12:00:00Z (Asia/Kolkata date is 2026-10-15)
    private static final Instant OCT_15_2026 = Instant.parse("2026-10-15T12:00:00Z");

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(OCT_15_2026, ZoneId.of("Asia/Kolkata"));
        service = new UpstoxIpoService(
                ipoDataProvider,
                mongoRepository,
                cacheService,
                ipoSyncMetaRepository,
                fixedClock);
    }

    @Test
    void calendar_monthWithUpcomingAndListedRecords_returnsCorrectMapping() {
        // Mock sync meta as fresh
        IpoSyncMetaDocument meta = IpoSyncMetaDocument.builder()
                .id("UPSTOX_ALL")
                .source("UPSTOX")
                .lastFullSyncAt(OCT_15_2026.minusSeconds(3600)) // 1 hour ago
                .build();
        when(ipoSyncMetaRepository.findById("UPSTOX_ALL")).thenReturn(Optional.of(meta));
        when(cacheService.getCachedCalendarMonth(2026, 10)).thenReturn(Optional.empty());

        UpstoxIpoDocument listedDoc = UpstoxIpoDocument.builder()
                .id("listed-ipo-slug")
                .companyName("Listed Co IPO")
                .status("listed")
                .listingPrice(350.0)
                .listingExchange("BSE,NSE")
                .timeline(AsraxIpoTimelineDto.builder().listingDate("2026-10-10").build())
                .build();

        UpstoxIpoDocument upcomingDoc = UpstoxIpoDocument.builder()
                .id("upcoming-ipo-slug")
                .companyName("Upcoming Co IPO")
                .status("open")
                .listingPrice(120.0)
                .listingExchange("NSE")
                .timeline(AsraxIpoTimelineDto.builder().listingDate("2026-10-20").build())
                .build();

        when(mongoRepository.findByTimelineListingDateBetween("2026-10-01", "2026-10-31"))
                .thenReturn(List.of(upcomingDoc, listedDoc));

        AsraxIpoCalendarMonthDto response = service.getIpoCalendarMonth(2026, 10);

        assertNotNull(response);
        assertEquals(2026, response.getYear());
        assertEquals(10, response.getMonth());
        assertEquals(IpoCalendarDataFreshness.FRESH, response.getDataFreshness());
        assertNotNull(response.getLastSuccessfulSyncAt());
        assertEquals(2, response.getEntries().size());

        // Sorted by listingDate ascending: 2026-10-10 before 2026-10-20
        AsraxIpoCalendarEntryDto entry1 = response.getEntries().get(0);
        assertEquals("listed-ipo-slug", entry1.getId());
        assertEquals("Listed Co IPO", entry1.getCompanyName());
        assertEquals(IpoCalendarDisplayStatus.LISTED, entry1.getStatus());
        assertEquals("2026-10-10", entry1.getListingDate());
        assertEquals(350.0, entry1.getListingPrice());
        assertEquals("BSE,NSE", entry1.getListingExchange());

        AsraxIpoCalendarEntryDto entry2 = response.getEntries().get(1);
        assertEquals("upcoming-ipo-slug", entry2.getId());
        assertEquals("Upcoming Co IPO", entry2.getCompanyName());
        assertEquals(IpoCalendarDisplayStatus.UPCOMING, entry2.getStatus());
        assertEquals("2026-10-20", entry2.getListingDate());

        // Verify result is cached in Redis
        verify(cacheService).cacheCalendarMonth(eq(2026), eq(10), eq(response));
    }

    @Test
    void calendar_monthBoundaries_includesFirstAndLastDayExcludesOutside() {
        IpoSyncMetaDocument meta = IpoSyncMetaDocument.builder()
                .id("UPSTOX_ALL")
                .lastFullSyncAt(OCT_15_2026)
                .build();
        when(ipoSyncMetaRepository.findById("UPSTOX_ALL")).thenReturn(Optional.of(meta));
        when(cacheService.getCachedCalendarMonth(2026, 10)).thenReturn(Optional.empty());

        UpstoxIpoDocument firstDayDoc = UpstoxIpoDocument.builder()
                .id("first-day-ipo")
                .companyName("First Day Co")
                .status("listed")
                .timeline(AsraxIpoTimelineDto.builder().listingDate("2026-10-01").build())
                .build();

        UpstoxIpoDocument lastDayDoc = UpstoxIpoDocument.builder()
                .id("last-day-ipo")
                .companyName("Last Day Co")
                .status("open")
                .timeline(AsraxIpoTimelineDto.builder().listingDate("2026-10-31").build())
                .build();

        UpstoxIpoDocument previousMonthDoc = UpstoxIpoDocument.builder()
                .id("prev-month-ipo")
                .companyName("Prev Month Co")
                .status("listed")
                .timeline(AsraxIpoTimelineDto.builder().listingDate("2026-09-30").build())
                .build();

        when(mongoRepository.findByTimelineListingDateBetween("2026-10-01", "2026-10-31"))
                .thenReturn(List.of(firstDayDoc, lastDayDoc, previousMonthDoc));

        AsraxIpoCalendarMonthDto response = service.getIpoCalendarMonth(2026, 10);
        assertEquals(2, response.getEntries().size());
        assertEquals("first-day-ipo", response.getEntries().get(0).getId());
        assertEquals("last-day-ipo", response.getEntries().get(1).getId());
    }

    @Test
    void calendar_nullOrInvalidListingDate_omittedFromCalendar() {
        IpoSyncMetaDocument meta = IpoSyncMetaDocument.builder().id("UPSTOX_ALL").lastFullSyncAt(OCT_15_2026).build();
        when(ipoSyncMetaRepository.findById("UPSTOX_ALL")).thenReturn(Optional.of(meta));
        when(cacheService.getCachedCalendarMonth(2026, 10)).thenReturn(Optional.empty());

        UpstoxIpoDocument nullTimelineDoc = UpstoxIpoDocument.builder()
                .id("null-timeline-ipo")
                .companyName("Null Timeline")
                .status("open")
                .timeline(null)
                .build();

        UpstoxIpoDocument nullDateDoc = UpstoxIpoDocument.builder()
                .id("null-date-ipo")
                .companyName("Null Date")
                .status("open")
                .timeline(AsraxIpoTimelineDto.builder().listingDate(null).build())
                .build();

        UpstoxIpoDocument invalidDateDoc = UpstoxIpoDocument.builder()
                .id("invalid-date-ipo")
                .companyName("Invalid Date")
                .status("open")
                .timeline(AsraxIpoTimelineDto.builder().listingDate("not-a-date").build())
                .build();

        when(mongoRepository.findByTimelineListingDateBetween("2026-10-01", "2026-10-31"))
                .thenReturn(List.of(nullTimelineDoc, nullDateDoc, invalidDateDoc));

        AsraxIpoCalendarMonthDto response = service.getIpoCalendarMonth(2026, 10);
        assertTrue(response.getEntries().isEmpty());
    }

    @Test
    void calendar_nullListingPriceOrExchange_preservedInDto() {
        IpoSyncMetaDocument meta = IpoSyncMetaDocument.builder().id("UPSTOX_ALL").lastFullSyncAt(OCT_15_2026).build();
        when(ipoSyncMetaRepository.findById("UPSTOX_ALL")).thenReturn(Optional.of(meta));
        when(cacheService.getCachedCalendarMonth(2026, 10)).thenReturn(Optional.empty());

        UpstoxIpoDocument doc = UpstoxIpoDocument.builder()
                .id("tba-ipo")
                .companyName("TBA Co IPO")
                .status("open")
                .listingPrice(null)
                .listingExchange(null)
                .timeline(AsraxIpoTimelineDto.builder().listingDate("2026-10-25").build())
                .build();

        when(mongoRepository.findByTimelineListingDateBetween("2026-10-01", "2026-10-31"))
                .thenReturn(List.of(doc));

        AsraxIpoCalendarMonthDto response = service.getIpoCalendarMonth(2026, 10);
        assertEquals(1, response.getEntries().size());
        AsraxIpoCalendarEntryDto entry = response.getEntries().get(0);
        assertNull(entry.getListingPrice());
        assertNull(entry.getListingExchange());
    }

    @Test
    void calendar_emptyMonth_returnsEmptyEntriesWithRequestedYearMonth() {
        IpoSyncMetaDocument meta = IpoSyncMetaDocument.builder().id("UPSTOX_ALL").lastFullSyncAt(OCT_15_2026).build();
        when(ipoSyncMetaRepository.findById("UPSTOX_ALL")).thenReturn(Optional.of(meta));
        when(cacheService.getCachedCalendarMonth(2026, 10)).thenReturn(Optional.empty());
        when(mongoRepository.findByTimelineListingDateBetween("2026-10-01", "2026-10-31")).thenReturn(Collections.emptyList());

        AsraxIpoCalendarMonthDto response = service.getIpoCalendarMonth(2026, 10);
        assertNotNull(response);
        assertEquals(2026, response.getYear());
        assertEquals(10, response.getMonth());
        assertTrue(response.getEntries().isEmpty());
        assertEquals(IpoCalendarDataFreshness.FRESH, response.getDataFreshness());
    }

    @Test
    void calendar_invalidMonthInput_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> service.getIpoCalendarMonth(2026, 0));
        assertThrows(IllegalArgumentException.class, () -> service.getIpoCalendarMonth(2026, 13));
    }

    @Test
    void calendar_monthOutsideCurrentOrNext_throwsIllegalArgumentException() {
        // Today is 2026-10-15: only 2026-10 and 2026-11 are permitted
        assertThrows(IllegalArgumentException.class, () -> service.getIpoCalendarMonth(2026, 9));
        assertThrows(IllegalArgumentException.class, () -> service.getIpoCalendarMonth(2026, 12));
        assertThrows(IllegalArgumentException.class, () -> service.getIpoCalendarMonth(2025, 10));
    }

    @Test
    void calendar_rolloverToNextMonth_clockAdvanceAcceptsNewCurrentAndNext() {
        // Move clock to 2026-11-01
        Clock novClock = Clock.fixed(Instant.parse("2026-11-01T01:00:00Z"), ZoneId.of("Asia/Kolkata"));
        UpstoxIpoService novService = new UpstoxIpoService(
                ipoDataProvider, mongoRepository, cacheService, ipoSyncMetaRepository, novClock);

        // In Nov 2026: 2026-10 is now rejected (past month)
        assertThrows(IllegalArgumentException.class, () -> novService.getIpoCalendarMonth(2026, 10));

        // 2026-11 and 2026-12 are allowed
        IpoSyncMetaDocument meta = IpoSyncMetaDocument.builder().id("UPSTOX_ALL").lastFullSyncAt(Instant.parse("2026-11-01T00:00:00Z")).build();
        when(ipoSyncMetaRepository.findById("UPSTOX_ALL")).thenReturn(Optional.of(meta));
        when(mongoRepository.findByTimelineListingDateBetween(anyString(), anyString())).thenReturn(Collections.emptyList());

        AsraxIpoCalendarMonthDto novResponse = novService.getIpoCalendarMonth(2026, 11);
        assertEquals(11, novResponse.getMonth());

        AsraxIpoCalendarMonthDto decResponse = novService.getIpoCalendarMonth(2026, 12);
        assertEquals(12, decResponse.getMonth());
    }

    @Test
    void calendar_listingDatePastButStatusNotListed_omittedFromCalendar() {
        IpoSyncMetaDocument meta = IpoSyncMetaDocument.builder().id("UPSTOX_ALL").lastFullSyncAt(OCT_15_2026).build();
        when(ipoSyncMetaRepository.findById("UPSTOX_ALL")).thenReturn(Optional.of(meta));
        when(cacheService.getCachedCalendarMonth(2026, 10)).thenReturn(Optional.empty());

        // Listing date is 2026-10-10 (before today 2026-10-15), but status is still "open" or "upcoming"
        UpstoxIpoDocument staleStatusDoc = UpstoxIpoDocument.builder()
                .id("stale-status-ipo")
                .companyName("Stale Status Co")
                .status("open")
                .timeline(AsraxIpoTimelineDto.builder().listingDate("2026-10-10").build())
                .build();

        when(mongoRepository.findByTimelineListingDateBetween("2026-10-01", "2026-10-31"))
                .thenReturn(List.of(staleStatusDoc));

        AsraxIpoCalendarMonthDto response = service.getIpoCalendarMonth(2026, 10);
        assertTrue(response.getEntries().isEmpty(), "Past listing date with non-listed status must be omitted");
    }

    @Test
    void calendar_listedStatusWithFutureDate_remainsListed() {
        IpoSyncMetaDocument meta = IpoSyncMetaDocument.builder().id("UPSTOX_ALL").lastFullSyncAt(OCT_15_2026).build();
        when(ipoSyncMetaRepository.findById("UPSTOX_ALL")).thenReturn(Optional.of(meta));
        when(cacheService.getCachedCalendarMonth(2026, 10)).thenReturn(Optional.empty());

        // Status is listed, but listing date is 2026-10-25 (after today 2026-10-15)
        UpstoxIpoDocument futureListedDoc = UpstoxIpoDocument.builder()
                .id("future-listed-ipo")
                .companyName("Future Listed Co")
                .status("listed")
                .timeline(AsraxIpoTimelineDto.builder().listingDate("2026-10-25").build())
                .build();

        when(mongoRepository.findByTimelineListingDateBetween("2026-10-01", "2026-10-31"))
                .thenReturn(List.of(futureListedDoc));

        AsraxIpoCalendarMonthDto response = service.getIpoCalendarMonth(2026, 10);
        assertEquals(1, response.getEntries().size());
        assertEquals(IpoCalendarDisplayStatus.LISTED, response.getEntries().get(0).getStatus());
    }

    @Test
    void calendar_neverSynced_returnsUnavailableFreshnessAndEmptyEntries() {
        when(cacheService.getCachedCalendarMonth(2026, 10)).thenReturn(Optional.empty());
        when(ipoSyncMetaRepository.findById("UPSTOX_ALL")).thenReturn(Optional.empty());

        AsraxIpoCalendarMonthDto response = service.getIpoCalendarMonth(2026, 10);
        assertEquals(IpoCalendarDataFreshness.UNAVAILABLE, response.getDataFreshness());
        assertNull(response.getLastSuccessfulSyncAt());
        assertTrue(response.getEntries().isEmpty());
        // Verify Mongo was NOT queried for listings when unavailable
        verify(mongoRepository, never()).findByTimelineListingDateBetween(anyString(), anyString());
    }

    @Test
    void calendar_syncOlderThan24Hours_returnsStaleFreshness() {
        Instant thirtyHoursAgo = OCT_15_2026.minusSeconds(30 * 3600);
        IpoSyncMetaDocument meta = IpoSyncMetaDocument.builder()
                .id("UPSTOX_ALL")
                .lastFullSyncAt(thirtyHoursAgo)
                .build();
        when(ipoSyncMetaRepository.findById("UPSTOX_ALL")).thenReturn(Optional.of(meta));
        when(cacheService.getCachedCalendarMonth(2026, 10)).thenReturn(Optional.empty());
        when(mongoRepository.findByTimelineListingDateBetween("2026-10-01", "2026-10-31")).thenReturn(Collections.emptyList());

        AsraxIpoCalendarMonthDto response = service.getIpoCalendarMonth(2026, 10);
        assertEquals(IpoCalendarDataFreshness.STALE, response.getDataFreshness());
    }

    @Test
    void sync_paginatesAcrossPagesAndDeduplicatesById() {
        // Page 1 has 30 items, page 2 has 5 items -> stops pagination
        List<AsraxIpoSummaryDto> page1 = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            page1.add(AsraxIpoSummaryDto.builder().id("ipo-" + i).status("open").build());
        }
        List<AsraxIpoSummaryDto> page2 = List.of(
                AsraxIpoSummaryDto.builder().id("ipo-30").status("open").build(),
                AsraxIpoSummaryDto.builder().id("ipo-0").status("open").build() // Duplicate ID
        );

        when(ipoDataProvider.getIpos(eq("open"), any(), eq(1), eq(30))).thenReturn(page1);
        when(ipoDataProvider.getIpos(eq("open"), any(), eq(2), eq(30))).thenReturn(page2);
        when(ipoDataProvider.getIpos(eq("upcoming"), any(), eq(1), eq(30))).thenReturn(Collections.emptyList());
        when(ipoDataProvider.getIpos(eq("closed"), any(), eq(1), eq(30))).thenReturn(Collections.emptyList());
        when(ipoDataProvider.getIpos(eq("listed"), any(), eq(1), eq(30))).thenReturn(Collections.emptyList());

        when(mongoRepository.findById(anyString())).thenReturn(Optional.empty());
        when(ipoDataProvider.getIpoDetails(anyString())).thenAnswer(inv -> {
            String id = inv.getArgument(0);
            return Optional.of(AsraxIpoDetailsDto.builder()
                    .id(id)
                    .companyName("Company " + id)
                    .status("open")
                    .timeline(AsraxIpoTimelineDto.builder().listingDate("2026-10-25").build())
                    .build());
        });

        int synced = service.syncUpstoxIpos("all");
        assertEquals(31, synced, "Total unique IPOs should be 31 (30 from page 1 + 1 new from page 2)");

        // Verify sync meta updated with lastFullSyncAt
        ArgumentCaptor<IpoSyncMetaDocument> metaCaptor = ArgumentCaptor.forClass(IpoSyncMetaDocument.class);
        verify(ipoSyncMetaRepository).save(metaCaptor.capture());
        IpoSyncMetaDocument savedMeta = metaCaptor.getValue();
        assertEquals("UPSTOX_ALL", savedMeta.getId());
        assertEquals("UPSTOX", savedMeta.getSource());
        assertNotNull(savedMeta.getLastFullSyncAt());
        assertNull(savedMeta.getLastError());
    }

    @Test
    void sync_upstreamPageFailure_doesNotAdvanceLastFullSyncAt() {
        when(ipoDataProvider.getIpos(eq("open"), any(), eq(1), eq(30)))
                .thenThrow(new IllegalStateException("Upstox HTTP 500"));
        when(ipoDataProvider.getIpos(eq("upcoming"), any(), eq(1), eq(30))).thenReturn(Collections.emptyList());
        when(ipoDataProvider.getIpos(eq("closed"), any(), eq(1), eq(30))).thenReturn(Collections.emptyList());
        when(ipoDataProvider.getIpos(eq("listed"), any(), eq(1), eq(30))).thenReturn(Collections.emptyList());

        service.syncUpstoxIpos("all");

        ArgumentCaptor<IpoSyncMetaDocument> metaCaptor = ArgumentCaptor.forClass(IpoSyncMetaDocument.class);
        verify(ipoSyncMetaRepository).save(metaCaptor.capture());
        IpoSyncMetaDocument savedMeta = metaCaptor.getValue();
        assertNull(savedMeta.getLastFullSyncAt(), "Failed sync must NOT advance lastFullSyncAt");
        assertNotNull(savedMeta.getLastError(), "Error message must be recorded");
    }
}
