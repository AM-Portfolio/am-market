package com.am.marketdata.service;

import com.am.common.investment.model.historical.HistoricalData;
import com.am.common.investment.model.historical.OHLCVTPoint;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.service.calendar.MarketCalendarService;
import com.am.marketdata.service.model.PreviousCloseDocument;
import com.am.marketdata.service.repo.PreviousCloseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartStockPreviousCloseSnapshotTest {

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private MarketDataCacheService marketDataCacheService;

    @Mock
    private PreviousCloseRepository previousCloseRepository;

    @Mock
    private MarketCalendarService marketCalendarService;

    private SmartStockService smartStockService;

    @BeforeEach
    void setUp() {
        smartStockService = new SmartStockService(marketDataService, marketDataCacheService);
        ReflectionTestUtils.setField(smartStockService, "previousCloseRepository", previousCloseRepository);
        ReflectionTestUtils.setField(smartStockService, "marketCalendarService", marketCalendarService);
    }

    @Test
    void ignoresSnapshotFromEarlierTradingSessionAndUsesHistoricalClose() {
        LocalDate sessionDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        when(marketCalendarService.getTimings("NSE", sessionDate)).thenReturn(openSession(sessionDate));
        when(marketDataService.getOHLC(eq(List.of("TCS")), eq(TimeFrame.DAY), eq(false), isNull(), eq(false)))
                .thenReturn(new HashMap<>(Map.of("TCS", quote(2500.0, 0.0))));
        when(previousCloseRepository.findBySymbolIn(List.of("TCS"))).thenReturn(List.of(
                PreviousCloseDocument.builder()
                        .symbol("TCS")
                        .previousClose(2490.0)
                        .tradeDate(sessionDate.minusDays(1).toString())
                        .build()));

        HistoricalData history = HistoricalData.builder()
                .dataPoints(List.of(
                        candle(sessionDate.minusDays(2), 2460.0),
                        candle(sessionDate.minusDays(1), 2500.0)))
                .build();
        when(marketDataService.getHistoricalDataBatch(
                eq(List.of("TCS")), any(Date.class), any(Date.class), eq(TimeFrame.DAY),
                eq(false), isNull(), isNull(), eq(false), eq(false), eq(false)))
                .thenReturn(Map.of("TCS", history));

        Map<String, OHLCQuote> result = smartStockService.getSmartQuotes(List.of("TCS"), TimeFrame.DAY);

        assertEquals(2500.0, result.get("TCS").getPreviousClose(), 0.001);
        verify(marketDataService).getHistoricalDataBatch(
                eq(List.of("TCS")), any(Date.class), any(Date.class), eq(TimeFrame.DAY),
                eq(false), isNull(), isNull(), eq(false), eq(false), eq(false));
    }

    @Test
    void acceptsSnapshotForCurrentTradingSession() {
        LocalDate sessionDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        when(marketCalendarService.getTimings("NSE", sessionDate)).thenReturn(openSession(sessionDate));
        when(marketDataService.getOHLC(eq(List.of("TCS")), eq(TimeFrame.DAY), eq(false), isNull(), eq(false)))
                .thenReturn(new HashMap<>(Map.of("TCS", quote(2500.0, 0.0))));
        when(previousCloseRepository.findBySymbolIn(List.of("TCS"))).thenReturn(List.of(
                PreviousCloseDocument.builder()
                        .symbol("TCS")
                        .previousClose(2460.0)
                        .tradeDate(sessionDate.toString())
                        .build()));

        Map<String, OHLCQuote> result = smartStockService.getSmartQuotes(List.of("TCS"), TimeFrame.DAY);

        assertEquals(2460.0, result.get("TCS").getPreviousClose(), 0.001);
        verify(marketDataService, never()).getHistoricalDataBatch(
                anyList(), any(Date.class), any(Date.class), any(), anyBoolean(), any(), any(),
                anyBoolean(), anyBoolean(), anyBoolean());
    }

    @Test
    void verifiesSnapshotThatMatchesLastPriceAgainstHistory() {
        LocalDate sessionDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        when(marketCalendarService.getTimings("NSE", sessionDate)).thenReturn(openSession(sessionDate));
        when(marketDataService.getOHLC(eq(List.of("TCS")), eq(TimeFrame.DAY), eq(false), isNull(), eq(false)))
                .thenReturn(new HashMap<>(Map.of("TCS", quote(2500.0, 2500.0))));
        when(previousCloseRepository.findBySymbolIn(List.of("TCS"))).thenReturn(List.of(
                PreviousCloseDocument.builder()
                        .symbol("TCS")
                        .previousClose(2500.0)
                        .tradeDate(sessionDate.toString())
                        .build()));

        HistoricalData history = HistoricalData.builder()
                .dataPoints(List.of(
                        candle(sessionDate.minusDays(2), 2460.0),
                        candle(sessionDate.minusDays(1), 2500.0)))
                .build();
        when(marketDataService.getHistoricalDataBatch(
                eq(List.of("TCS")), any(Date.class), any(Date.class), eq(TimeFrame.DAY),
                eq(false), isNull(), isNull(), eq(false), eq(false), eq(false)))
                .thenReturn(Map.of("TCS", history));

        Map<String, OHLCQuote> result = smartStockService.getSmartQuotes(List.of("TCS"), TimeFrame.DAY);

        assertEquals(2500.0, result.get("TCS").getPreviousClose(), 0.001);
        verify(marketDataService).getHistoricalDataBatch(
                eq(List.of("TCS")), any(Date.class), any(Date.class), eq(TimeFrame.DAY),
                eq(false), isNull(), isNull(), eq(false), eq(false), eq(false));
    }

    private static OHLCQuote quote(double lastPrice, double previousClose) {
        return OHLCQuote.builder().lastPrice(lastPrice).previousClose(previousClose).build();
    }

    private static OHLCVTPoint candle(LocalDate date, double close) {
        return OHLCVTPoint.builder()
                .time(LocalDateTime.of(date, LocalTime.of(15, 30)))
                .open(close)
                .high(close)
                .low(close)
                .close(close)
                .volume(1000L)
                .build();
    }

    private static MarketCalendarService.SessionTiming openSession(LocalDate date) {
        return new MarketCalendarService.SessionTiming(
                "NSE", date, true, LocalTime.of(9, 15), LocalTime.of(15, 30), "TEST",
                new MarketCalendarService.CalendarMeta(false, "TEST", java.time.Instant.now()));
    }
}
