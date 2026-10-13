package com.am.marketdata.service.calendar;

import com.am.marketdata.common.calendar.MarketCalendarSource;
import com.am.marketdata.common.calendar.MarketHolidayDay;
import com.am.marketdata.common.calendar.MarketHolidayType;
import com.am.marketdata.service.model.MarketCalendarDayDocument;
import com.am.marketdata.service.repo.MarketCalendarRepository;
import com.am.marketdata.service.repo.MarketCalendarSyncMetaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketCalendarServiceTest {

    @Mock
    private MarketCalendarSource marketCalendarSource;
    @Mock
    private MarketCalendarRepository calendarRepository;
    @Mock
    private MarketCalendarSyncMetaRepository syncMetaRepository;

    private MarketCalendarSyncService syncService;
    private MarketCalendarService calendarService;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(
                Instant.parse("2026-01-26T05:00:00Z"),
                ZoneId.of("Asia/Kolkata"));
        syncService = new MarketCalendarSyncService(
                marketCalendarSource, calendarRepository, syncMetaRepository);
        calendarService = new MarketCalendarService(syncService, calendarRepository, clock);
        calendarService.getClass();
        // inject @Value fields via reflection-free defaults by using package setter pattern:
        // MarketCalendarService uses field injection for hours; set via reflection
        setField(calendarService, "marketHoursStart", "09:15");
        setField(calendarService, "marketHoursEnd", "15:30");
        setField(calendarService, "timezone", "Asia/Kolkata");
    }

    @Test
    void statusClosedOnRepublicDayWhenCalendarHasClosedDay() {
        LocalDate republicDay = LocalDate.of(2026, 1, 26);
        when(calendarRepository.countByExchangeAndDateBetween(eq("NSE"), any(), any())).thenReturn(1L);
        when(calendarRepository.findByExchangeAndDate("NSE", republicDay))
                .thenReturn(java.util.Optional.of(MarketCalendarDayDocument.builder()
                        .id("NSE:2026-01-26")
                        .date(republicDay)
                        .exchange("NSE")
                        .closed(true)
                        .holidayType("TRADING_HOLIDAY")
                        .build()));
        when(calendarRepository.findByExchangeAndDateBetween(eq("NSE"), any(), any()))
                .thenReturn(List.of());

        MarketCalendarService.MarketStatusView status = calendarService.getStatus("NSE");
        assertFalse(status.open());
        assertEquals("HOLIDAY", status.reason());
        verify(marketCalendarSource, never()).fetchHolidays(anyString());
    }

    @Test
    void lazySyncInvokesSourceOnceWhenDbEmpty() {
        AtomicInteger persisted = new AtomicInteger(0);
        when(calendarRepository.countByExchangeAndDateBetween(eq("NSE"), any(), any()))
                .thenAnswer(inv -> persisted.get() > 0 ? 1L : 0L);
        when(marketCalendarSource.sourceId()).thenReturn("UPSTOX");
        when(marketCalendarSource.fetchHolidays("NSE")).thenReturn(List.of(
                new MarketHolidayDay(
                        LocalDate.of(2026, 1, 26),
                        "NSE",
                        "Republic Day",
                        MarketHolidayType.TRADING_HOLIDAY,
                        true,
                        null,
                        null)));
        when(calendarRepository.saveAll(any())).thenAnswer(inv -> {
            persisted.incrementAndGet();
            return inv.getArgument(0);
        });
        when(calendarRepository.findByExchangeAndDateBetween(eq("NSE"), any(), any()))
                .thenReturn(List.of(MarketCalendarDayDocument.builder()
                        .id("NSE:2026-01-26")
                        .date(LocalDate.of(2026, 1, 26))
                        .exchange("NSE")
                        .closed(true)
                        .build()));

        calendarService.getHolidays("NSE", 2026);
        calendarService.getHolidays("NSE", 2026);

        verify(marketCalendarSource, times(1)).fetchHolidays("NSE");
        verify(calendarRepository, times(1)).saveAll(any());
    }

    @Test
    void syncMapsAndPersistsHolidayDays() {
        when(marketCalendarSource.sourceId()).thenReturn("UPSTOX");
        when(marketCalendarSource.fetchHolidays("NSE")).thenReturn(List.of(
                new MarketHolidayDay(
                        LocalDate.of(2026, 2, 1),
                        "NSE",
                        "Budget",
                        MarketHolidayType.SPECIAL_TIMING,
                        false,
                        LocalTime.of(9, 15),
                        LocalTime.of(14, 30))));

        int saved = syncService.sync("NSE", MarketCalendarSyncTrigger.ADMIN);
        assertEquals(1, saved);
        verify(calendarRepository).saveAll(any());
        verify(syncMetaRepository).save(any());
    }

    private static void setField(Object target, String name, Object value) {
        try {
            var field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
