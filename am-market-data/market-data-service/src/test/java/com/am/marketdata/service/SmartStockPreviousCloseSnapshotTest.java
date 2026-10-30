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
    void acceptsCurrentSessionSnapshotWhenStockIsUnchanged() {
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

        Map<String, OHLCQuote> result = smartStockService.getSmartQuotes(List.of("TCS"), TimeFrame.DAY);

        assertEquals(2500.0, result.get("TCS").getPreviousClose(), 0.001);
        verify(marketDataService, never()).getHistoricalDataBatch(
                anyList(), any(Date.class), any(Date.class), any(), anyBoolean(), any(), any(),
                anyBoolean(), anyBoolean(), anyBoolean());
    }

    @Test
    void cachesMultipleVerifiedSnapshotsWithOneBatchWrite() {
        LocalDate sessionDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        Map<String, OHLCQuote> quotes = new HashMap<>();
        quotes.put("TCS", quote(2500.0, 0.0));
        quotes.put("INFY", quote(1500.0, 0.0));
        when(marketCalendarService.getTimings("NSE", sessionDate)).thenReturn(openSession(sessionDate));
        when(marketDataService.getOHLC(eq(List.of("TCS", "INFY")), eq(TimeFrame.DAY), eq(false), isNull(), eq(false)))
                .thenReturn(quotes);
        when(previousCloseRepository.findBySymbolIn(List.of("TCS", "INFY"))).thenReturn(List.of(
                PreviousCloseDocument.builder().symbol("TCS").previousClose(2490.0)
                        .tradeDate(sessionDate.toString()).build(),
                PreviousCloseDocument.builder().symbol("INFY").previousClose(1490.0)
                        .tradeDate(sessionDate.toString()).build()));

        Map<String, OHLCQuote> result = smartStockService.getSmartQuotes(List.of("TCS", "INFY"), TimeFrame.DAY);

        assertEquals(2490.0, result.get("TCS").getPreviousClose(), 0.001);
        assertEquals(1490.0, result.get("INFY").getPreviousClose(), 0.001);
        verify(marketDataCacheService, times(1)).cacheLatestPrices(argThat(batch ->
                batch.size() == 2 && batch.containsKey("TCS") && batch.containsKey("INFY")));
        verify(marketDataService, never()).getHistoricalDataBatch(
                anyList(), any(Date.class), any(Date.class), any(), anyBoolean(), any(), any(),
                anyBoolean(), anyBoolean(), anyBoolean());
    }

    @Test
    void cacheWriteFailureDoesNotDiscardVerifiedDailyQuote() {
        LocalDate sessionDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        when(marketCalendarService.getTimings("NSE", sessionDate)).thenReturn(openSession(sessionDate));
        when(marketDataService.getOHLC(eq(List.of("TCS")), eq(TimeFrame.DAY), eq(false), isNull(), eq(false)))
                .thenReturn(new HashMap<>(Map.of("TCS", quote(2500.0, 0.0))));
        when(previousCloseRepository.findBySymbolIn(List.of("TCS"))).thenReturn(List.of(
                PreviousCloseDocument.builder().symbol("TCS").previousClose(2490.0)
                        .tradeDate(sessionDate.toString()).build()));
        doThrow(new IllegalStateException("cache unavailable"))
                .when(marketDataCacheService).cacheLatestPrices(anyMap());

        Map<String, OHLCQuote> result = smartStockService.getSmartQuotes(List.of("TCS"), TimeFrame.DAY);

        assertEquals(2490.0, result.get("TCS").getPreviousClose(), 0.001);
    }

    @Test
    void choosesLatestAvailableCandleOnOrBeforeBoundaryAndNeverFutureCandle() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        LocalDate boundary = today.minusMonths(1);
        when(marketDataCacheService.getLatestPrices(anySet())).thenReturn(Map.of(
                "TCS", quote(120.0, 99.0),
                "INFY", quote(120.0, 99.0)));
        when(marketDataService.getOHLC(eq(List.of("TCS", "INFY")), eq(TimeFrame.DAY), eq(false), isNull(), eq(false)))
                .thenReturn(Map.of());
        when(marketDataService.getHistoricalDataBatch(
                eq(List.of("TCS", "INFY")), any(Date.class), any(Date.class), eq(TimeFrame.DAY),
                eq(false), isNull(), isNull(), eq(false), eq(false), eq(false)))
                .thenReturn(Map.of(
                        "TCS", HistoricalData.builder().dataPoints(List.of(
                                candle(boundary.minusDays(3), 100.0), candle(boundary.plusDays(2), 111.0))).build(),
                        "INFY", HistoricalData.builder().dataPoints(List.of(
                                candle(boundary.plusDays(2), 110.0))).build()));

        Map<String, OHLCQuote> result = smartStockService.getSmartQuotes(List.of("TCS", "INFY"), TimeFrame.MONTH);

        assertEquals(100.0, result.get("TCS").getPreviousClose(), 0.001);
        assertEquals(0.0, result.get("INFY").getPreviousClose(), 0.001);
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
