package com.am.marketdata.service.global;

import com.am.common.investment.model.historical.HistoricalData;
import com.am.common.investment.model.historical.OHLCVTPoint;
import com.am.common.investment.persistence.document.global.GlobalIndexConfigDocument;
import com.am.common.investment.persistence.document.global.GlobalIndexConfigRepository;
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
import org.springframework.boot.ApplicationArguments;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalIndexGapFillServiceTest {

    @Mock
    private GlobalIndexConfigRepository configRepository;

    @Mock
    private GlobalIndexInfluxRepository influxRepository;

    @Mock
    private MarketDataProviderFactory providerFactory;

    @Mock
    private GlobalMarketScheduleService scheduleService;

    @Mock
    private MarketDataProvider provider;

    @InjectMocks
    private GlobalIndexGapFillService gapFillService;

    private static final String SYMBOL = "DJI";
    private static final String INSTRUMENT_KEY = "GLOBAL_INDEX|^DJI";

    private GlobalIndexConfigDocument config;

    @BeforeEach
    void setUp() {
        config = GlobalIndexConfigDocument.builder()
                .symbol(SYMBOL)
                .name("Dow Jones Industrial Average")
                .instrumentKey(INSTRUMENT_KEY)
                .build();
    }

    @Test
    void run_shouldTriggerAsynchronouslyWithoutThrowing() {
        ApplicationArguments args = mock(ApplicationArguments.class);
        gapFillService.run(args);
        // Method returns immediately without blocking
    }

    @Test
    void fillGaps_whenNoVerifiedInstrument_shouldSkip() {
        config.setInstrumentKey("");
        when(configRepository.findAll()).thenReturn(List.of(config));
        when(providerFactory.getProvider("upstox")).thenReturn(provider);

        gapFillService.fillGaps();

        verify(influxRepository, never()).findLastTimestamp(any());
        verify(influxRepository, never()).saveAll(any());
    }

    @Test
    void fillGaps_whenNoPriorData_shouldSkip() {
        when(configRepository.findAll()).thenReturn(List.of(config));
        when(providerFactory.getProvider("upstox")).thenReturn(provider);
        when(influxRepository.findLastTimestamp(INSTRUMENT_KEY)).thenReturn(Optional.empty());

        gapFillService.fillGaps();

        verify(provider, never()).getHistoricalData(any(), any(), any(), any(), anyBoolean(), any());
        verify(influxRepository, never()).saveAll(any());
    }

    @Test
    void fillGaps_whenGapIsFiveMinutesOrLess_shouldSkip() {
        when(configRepository.findAll()).thenReturn(List.of(config));
        when(providerFactory.getProvider("upstox")).thenReturn(provider);

        Instant recent = Instant.now().minusSeconds(180); // 3 mins ago
        when(influxRepository.findLastTimestamp(INSTRUMENT_KEY)).thenReturn(Optional.of(recent));

        gapFillService.fillGaps();

        verify(provider, never()).getHistoricalData(any(), any(), any(), any(), anyBoolean(), any());
        verify(influxRepository, never()).saveAll(any());
    }

    @Test
    void fillGaps_whenGapDetected_shouldFetchAndCalculateAgainstBaseline() {
        when(configRepository.findAll()).thenReturn(List.of(config));
        when(providerFactory.getProvider("upstox")).thenReturn(provider);

        Instant lastTime = Instant.now().minusSeconds(1800); // 30 mins ago
        when(influxRepository.findLastTimestamp(INSTRUMENT_KEY)).thenReturn(Optional.of(lastTime));

        // Mock historical data points returned
        LocalDateTime ptTime = LocalDateTime.ofInstant(lastTime.plusSeconds(600), ZoneOffset.UTC);
        OHLCVTPoint pt = OHLCVTPoint.builder()
                .time(ptTime)
                .open(50000.0)
                .high(50600.0)
                .low(49900.0)
                .close(50500.0)
                .volume(100L)
                .build();
        HistoricalData histData = new HistoricalData();
        histData.setDataPoints(List.of(pt));

        when(provider.getHistoricalData(eq(INSTRUMENT_KEY), any(), any(), eq(TimeFrame.MINUTE), eq(false), anyMap()))
                .thenReturn(histData);

        gapFillService.fillGaps();

        ArgumentCaptor<List<GlobalIndexMeasurement>> captor = ArgumentCaptor.forClass(List.class);
        verify(influxRepository).saveAll(captor.capture());

        List<GlobalIndexMeasurement> saved = captor.getValue();
        assertEquals(1, saved.size());
        assertEquals(50500.0, saved.get(0).getClose());
        assertEquals(50000.0, saved.get(0).getPreviousClose()); // First candle open as baseline
        assertEquals(1.0, saved.get(0).getChangePercent(), 0.001); // ((50500 - 50000) / 50000) * 100 = 1.0%
    }

    @Test
    void fillGaps_whenProviderReturnsEmpty_shouldNotFail() {
        when(configRepository.findAll()).thenReturn(List.of(config));
        when(providerFactory.getProvider("upstox")).thenReturn(provider);

        Instant lastTime = Instant.now().minusSeconds(1800);
        when(influxRepository.findLastTimestamp(INSTRUMENT_KEY)).thenReturn(Optional.of(lastTime));

        HistoricalData emptyData = new HistoricalData();
        emptyData.setDataPoints(Collections.emptyList());
        when(provider.getHistoricalData(eq(INSTRUMENT_KEY), any(), any(), eq(TimeFrame.MINUTE), eq(false), anyMap()))
                .thenReturn(emptyData);

        gapFillService.fillGaps();

        verify(influxRepository, never()).saveAll(any());
    }
}
