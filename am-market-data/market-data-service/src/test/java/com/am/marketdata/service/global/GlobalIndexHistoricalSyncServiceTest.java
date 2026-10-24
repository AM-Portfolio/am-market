package com.am.marketdata.service.global;

import com.am.common.investment.model.historical.HistoricalData;
import com.am.common.investment.model.historical.OHLCVTPoint;
import com.am.common.investment.persistence.document.global.GlobalIndexConfigDocument;
import com.am.common.investment.persistence.document.global.GlobalIndexConfigRepository;
import com.am.common.investment.persistence.document.global.GlobalIndexSyncStatus;
import com.am.common.investment.persistence.influx.measurement.GlobalIndexMeasurement;
import com.am.common.investment.persistence.repository.measurement.impl.GlobalIndexInfluxRepository;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.provider.common.MarketDataProviderFactory;
import com.marketdata.common.MarketDataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class GlobalIndexHistoricalSyncServiceTest {

    private static final String SYMBOL = "DJI";
    private static final String KEY = "GLOBAL_INDEX|^DJI";

    @Mock private GlobalIndexConfigRepository configRepository;
    @Mock private GlobalIndexInfluxRepository influxRepository;
    @Mock private MarketDataProviderFactory providerFactory;
    @Mock private GlobalMarketScheduleService scheduleService;
    @Mock private MarketDataProvider provider;

    @InjectMocks private GlobalIndexHistoricalSyncService syncService;

    private GlobalIndexConfigDocument config;
    private GlobalMarketScheduleService.MarketEntry market;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(syncService, "historyBackfillYears", 10);
        config = GlobalIndexConfigDocument.builder()
                .symbol(SYMBOL)
                .name("Dow Jones Industrial Average")
                .instrumentKey(KEY)
                .syncStatus(GlobalIndexSyncStatus.PENDING)
                .build();
        market = new GlobalMarketScheduleService.MarketEntry(
                SYMBOL, config.getName(), KEY, "NYSE", "America/New_York",
                "09:30", "16:00", "US", "-14h30m", 20);
        when(providerFactory.getProvider("upstox")).thenReturn(provider);
        when(configRepository.findBySymbol(SYMBOL)).thenReturn(Optional.of(config));
        lenient().when(scheduleService.getMarkets()).thenReturn(List.of(market));
        lenient().when(scheduleService.latestCompletedSessionDate(eq(market), any(Instant.class)))
                .thenReturn(Optional.of(LocalDate.of(2026, 10, 23)));
    }

    @Test
    void pendingSyncWritesSortedDeduplicatedValidCandlesThenMarksComplete() {
        OHLCVTPoint later = candle("2026-10-22T00:00:00", 110, 115, 108, 112);
        OHLCVTPoint earlier = candle("2026-10-21T00:00:00", 100, 111, 99, 110);
        OHLCVTPoint invalid = candle("2026-10-20T00:00:00", 100, 99, 98, 101);
        when(provider.getHistoricalData(eq(KEY), any(Date.class), any(Date.class), eq(TimeFrame.DAY),
                eq(false), anyMap()))
                .thenReturn(HistoricalData.builder().dataPoints(List.of(later, earlier, earlier, invalid)).build());

        syncService.syncHistoricalData(SYMBOL, false);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GlobalIndexMeasurement>> captor = ArgumentCaptor.forClass(List.class);
        verify(influxRepository).saveAll(captor.capture());
        List<GlobalIndexMeasurement> saved = captor.getValue();
        assertEquals(2, saved.size());
        assertEquals(Instant.parse("2026-10-21T00:00:00Z"), saved.get(0).getTime());
        assertNull(saved.get(0).getPreviousClose());
        assertEquals(100.0, saved.get(1).getPreviousClose());
        assertEquals(GlobalIndexSyncStatus.COMPLETE, config.getSyncStatus());
        verify(configRepository).save(config);
    }

    @Test
    void blankKeyIsSkippedWithoutCallingProviderOrWritingInflux() {
        config.setInstrumentKey(" ");

        syncService.syncHistoricalData(SYMBOL, false);

        verify(provider, never()).getHistoricalData(any(), any(), any(), any(), any(), any());
        verify(influxRepository, never()).saveAll(any());
        assertEquals(GlobalIndexSyncStatus.PENDING, config.getSyncStatus());
    }

    @Test
    void emptyProviderResponseDoesNotMarkCompleteOrWrite() {
        when(provider.getHistoricalData(eq(KEY), any(Date.class), any(Date.class), eq(TimeFrame.DAY),
                eq(false), anyMap()))
                .thenReturn(HistoricalData.builder().dataPoints(List.of()).build());

        syncService.syncHistoricalData(SYMBOL, false);

        verify(influxRepository, never()).saveAll(any());
        assertEquals(GlobalIndexSyncStatus.PENDING, config.getSyncStatus());
    }

    @Test
    void inProgressSyncSkipsDuplicateNonForcedRequest() {
        config.setSyncStatus(GlobalIndexSyncStatus.IN_PROGRESS);

        syncService.syncHistoricalData(SYMBOL, false);

        verify(provider, never()).getHistoricalData(any(), any(), any(), any(), any(), any());
        verify(influxRepository, never()).saveAll(any());
    }

    @Test
    void influxWriteFailureLeavesRetryableFailedStatus() {
        when(provider.getHistoricalData(eq(KEY), any(Date.class), any(Date.class), eq(TimeFrame.DAY),
                eq(false), anyMap()))
                .thenReturn(HistoricalData.builder().dataPoints(List.of(
                        candle("2026-10-21T00:00:00", 100, 111, 99, 110))).build());
        org.mockito.Mockito.doThrow(new RuntimeException("Influx unavailable"))
                .when(influxRepository).saveAll(any());

        syncService.syncHistoricalData(SYMBOL, false);

        assertEquals(GlobalIndexSyncStatus.FAILED, config.getSyncStatus());
        verify(configRepository).save(config);
    }

    @Test
    void configuredKeyMismatchFailsClosed() {
        config.setInstrumentKey("GLOBAL_INDEX|WRONG");

        syncService.syncHistoricalData(SYMBOL, true);

        verify(provider, never()).getHistoricalData(any(), any(), any(), any(), any(), any());
        verify(influxRepository, never()).saveAll(any());
        assertEquals(GlobalIndexSyncStatus.PENDING, config.getSyncStatus());
    }

    @Test
    void completedSyncSkipsProviderUnlessForceIsSet() {
        config.setSyncStatus(GlobalIndexSyncStatus.COMPLETE);

        syncService.syncHistoricalData(SYMBOL, false);

        verify(provider, never()).getHistoricalData(any(), any(), any(), any(), any(), any());
        verify(influxRepository, never()).saveAll(any());
    }

    @Test
    void forceRefreshesCompletedConfigAndPersistsHistory() {
        config.setSyncStatus(GlobalIndexSyncStatus.COMPLETE);
        when(provider.getHistoricalData(eq(KEY), any(Date.class), any(Date.class), eq(TimeFrame.DAY),
                eq(false), anyMap()))
                .thenReturn(HistoricalData.builder().dataPoints(List.of(
                        candle("2026-10-21T00:00:00", 100, 111, 99, 110))).build());

        syncService.syncHistoricalData(SYMBOL, true);

        verify(provider).getHistoricalData(eq(KEY), any(Date.class), any(Date.class), eq(TimeFrame.DAY),
                eq(false), anyMap());
        verify(influxRepository).saveAll(any());
        assertEquals(GlobalIndexSyncStatus.COMPLETE, config.getSyncStatus());
    }

    @Test
    void missingCompletedMarketSessionDoesNotCallProviderOrMarkComplete() {
        when(scheduleService.latestCompletedSessionDate(eq(market), any(Instant.class)))
                .thenReturn(Optional.empty());

        syncService.syncHistoricalData(SYMBOL, false);

        verify(provider, never()).getHistoricalData(any(), any(), any(), any(), any(), any());
        verify(influxRepository, never()).saveAll(any());
        assertEquals(GlobalIndexSyncStatus.PENDING, config.getSyncStatus());
    }

    @Test
    void providerFailureMarksConfigFailedAndKeepsInfluxUntouched() {
        when(provider.getHistoricalData(eq(KEY), any(Date.class), any(Date.class), eq(TimeFrame.DAY),
                eq(false), anyMap()))
                .thenThrow(new RuntimeException("provider unavailable"));

        syncService.syncHistoricalData(SYMBOL, false);

        verify(influxRepository, never()).saveAll(any());
        assertEquals(GlobalIndexSyncStatus.FAILED, config.getSyncStatus());
        verify(configRepository, times(2)).save(config); // IN_PROGRESS then FAILED.
    }

    private OHLCVTPoint candle(String time, double open, double high, double low, double close) {
        return OHLCVTPoint.builder()
                .time(LocalDateTime.parse(time))
                .open(open)
                .high(high)
                .low(low)
                .close(close)
                .build();
    }
}
