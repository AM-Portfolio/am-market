package com.am.common.investment.service.historical.impl;

import com.am.common.investment.model.equity.EquityPrice;
import com.am.common.investment.model.equity.MarketIndexIndices;
import com.am.common.investment.model.equity.MarketData;
import com.am.common.investment.model.historical.HistoricalData;
import com.am.common.investment.model.historical.OHLCVTPoint;
import com.am.common.investment.service.EquityService;
import com.am.common.investment.service.MarketIndexIndicesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HistoricalDataServiceImplTest {

    @Mock
    private EquityService equityService;

    @Mock
    private MarketIndexIndicesService marketIndexIndicesService;

    @Mock
    private com.am.common.investment.persistence.document.global.GlobalIndexConfigRepository globalIndexConfigRepository;

    private HistoricalDataServiceImpl historicalDataService;

    @BeforeEach
    void setUp() {
        historicalDataService = new HistoricalDataServiceImpl(
                equityService, 
                marketIndexIndicesService,
                null,
                globalIndexConfigRepository
        );
    }

    @Test
    void shouldReturnHistoricalDataForIndexSymbol() {
        // Given
        String symbol = "NIFTY 50";
        Instant now = Instant.parse("2026-06-12T10:00:00Z");
        Instant from = now.minus(5, ChronoUnit.DAYS);
        String interval = "1d";

        MarketIndexIndices indexPoint1 = new MarketIndexIndices();
        indexPoint1.setIndexSymbol(symbol);
        indexPoint1.setTimestamp(LocalDateTime.ofInstant(from, ZoneOffset.UTC));
        MarketData md1 = new MarketData();
        md1.setOpen(22000.0);
        md1.setHigh(22100.0);
        md1.setLow(21900.0);
        md1.setLast(22050.0);
        indexPoint1.setMarketData(md1);

        MarketIndexIndices indexPoint2 = new MarketIndexIndices();
        indexPoint2.setIndexSymbol(symbol);
        indexPoint2.setTimestamp(LocalDateTime.ofInstant(now, ZoneOffset.UTC));
        MarketData md2 = new MarketData();
        md2.setOpen(22100.0);
        md2.setHigh(22200.0);
        md2.setLow(22000.0);
        md2.setLast(22150.0);
        indexPoint2.setMarketData(md2);

        List<MarketIndexIndices> mockIndices = Arrays.asList(indexPoint1, indexPoint2);

        when(marketIndexIndicesService.getByIndexSymbolAndTimeBetween(eq(symbol), eq(from), eq(now)))
                .thenReturn(mockIndices);

        // When
        Optional<HistoricalData> result = historicalDataService.getHistoricalData(symbol, from, now, interval, true);

        // Then
        assertThat(result).isPresent();
        HistoricalData historicalData = result.get();
        assertThat(historicalData.getTradingSymbol()).isEqualTo(symbol);
        assertThat(historicalData.getDataPoints()).hasSize(2);
        
        assertThat(historicalData.getDataPoints().get(0).getOpen()).isEqualTo(22000.0);
        assertThat(historicalData.getDataPoints().get(0).getClose()).isEqualTo(22050.0);
        assertThat(historicalData.getDataPoints().get(1).getOpen()).isEqualTo(22100.0);
        assertThat(historicalData.getDataPoints().get(1).getClose()).isEqualTo(22150.0);
    }

    @Test
    void shouldReturnEmptyForEmptyIndexResult() {
        // Given
        String symbol = "NIFTY 50";
        Instant now = Instant.parse("2026-06-12T10:00:00Z");
        Instant from = now.minus(5, ChronoUnit.DAYS);

        when(marketIndexIndicesService.getByIndexSymbolAndTimeBetween(eq(symbol), eq(from), eq(now)))
                .thenReturn(Collections.emptyList());

        // When
        Optional<HistoricalData> result = historicalDataService.getHistoricalData(symbol, from, now, "1d", true);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldResampleIntradayIndexCandlesRespectingIndianTradingHours() {
        String symbol = "NIFTY 50";
        // 2026-11-14: 09:15 AM IST is 03:45:00 UTC
        Instant from = Instant.parse("2026-11-14T03:00:00Z");
        Instant to = Instant.parse("2026-11-14T11:00:00Z");

        // Pre-market tick at 09:05 AM IST (03:35 UTC) -> should be discarded
        MarketIndexIndices preMarket = new MarketIndexIndices();
        preMarket.setIndexSymbol(symbol);
        preMarket.setTimestamp(LocalDateTime.ofInstant(Instant.parse("2026-11-14T03:35:00Z"), ZoneOffset.UTC));
        MarketData mdPre = new MarketData();
        mdPre.setLast(25000.0);
        preMarket.setMarketData(mdPre);

        // Candle 1: 09:15 AM IST (03:45:00 UTC) tick 1
        MarketIndexIndices tick1 = new MarketIndexIndices();
        tick1.setIndexSymbol(symbol);
        tick1.setTimestamp(LocalDateTime.ofInstant(Instant.parse("2026-11-14T03:45:10Z"), ZoneOffset.UTC));
        MarketData md1 = new MarketData();
        md1.setLast(25100.0);
        tick1.setMarketData(md1);

        // Candle 1: 09:17 AM IST (03:47:00 UTC) tick 2
        MarketIndexIndices tick2 = new MarketIndexIndices();
        tick2.setIndexSymbol(symbol);
        tick2.setTimestamp(LocalDateTime.ofInstant(Instant.parse("2026-11-14T03:47:00Z"), ZoneOffset.UTC));
        MarketData md2 = new MarketData();
        md2.setLast(25150.0);
        tick2.setMarketData(md2);

        // Candle 2: 09:20 AM IST (03:50:00 UTC) tick 3
        MarketIndexIndices tick3 = new MarketIndexIndices();
        tick3.setIndexSymbol(symbol);
        tick3.setTimestamp(LocalDateTime.ofInstant(Instant.parse("2026-11-14T03:50:00Z"), ZoneOffset.UTC));
        MarketData md3 = new MarketData();
        md3.setLast(25180.0);
        tick3.setMarketData(md3);

        // Closing auction tick: 15:32:00 IST (10:02:00 UTC) -> clamped to 15:25 candle
        MarketIndexIndices closeTick = new MarketIndexIndices();
        closeTick.setIndexSymbol(symbol);
        closeTick.setTimestamp(LocalDateTime.ofInstant(Instant.parse("2026-11-14T10:02:00Z"), ZoneOffset.UTC));
        MarketData mdClose = new MarketData();
        mdClose.setLast(25200.0);
        closeTick.setMarketData(mdClose);

        List<MarketIndexIndices> mockIndices = List.of(preMarket, tick1, tick2, tick3, closeTick);

        when(marketIndexIndicesService.getByIndexSymbolAndTimeBetween(eq(symbol), eq(from), eq(to)))
                .thenReturn(mockIndices);

        Optional<HistoricalData> result = historicalDataService.getHistoricalData(symbol, from, to, "5m", true);

        assertThat(result).isPresent();
        List<OHLCVTPoint> points = result.get().getDataPoints();
        // 3 candles: 09:15-09:20, 09:20-09:25, and 15:25-15:30 (pre-market was discarded)
        assertThat(points).hasSize(3);
        // First candle open=25100, close=25150
        assertThat(points.get(0).getOpen()).isEqualTo(25100.0);
        assertThat(points.get(0).getClose()).isEqualTo(25150.0);
        // Second candle open=25180, close=25180
        assertThat(points.get(1).getOpen()).isEqualTo(25180.0);
        assertThat(points.get(1).getClose()).isEqualTo(25180.0);
        // Closing candle has the settlement tick
        assertThat(points.get(2).getClose()).isEqualTo(25200.0);
    }

    @Test
    void shouldSupportAllTimeframesMultipliedToFiveMinutes() {
        String symbol = "NIFTY 50";
        // 2026-11-18: 09:15 IST is 03:45:00 UTC, 15:30 IST is 10:00:00 UTC
        Instant from = Instant.parse("2026-11-18T03:45:00Z");
        Instant to = Instant.parse("2026-11-18T10:00:00Z");

        // Generate 375 1-minute index samples across the entire trading session (09:15 to 15:30 IST)
        List<MarketIndexIndices> fullSessionIndices = new java.util.ArrayList<>();
        for (int i = 0; i < 375; i++) {
            Instant minuteTime = from.plus(i, ChronoUnit.MINUTES);
            MarketIndexIndices item = new MarketIndexIndices();
            item.setIndexSymbol(symbol);
            item.setTimestamp(LocalDateTime.ofInstant(minuteTime, ZoneOffset.UTC));

            double basePrice = 24000.0 + (i * 0.5); // Slow upward trend
            MarketData md = new MarketData();
            md.setOpen(basePrice);
            md.setHigh(basePrice + 5.0);
            md.setLow(basePrice - 5.0);
            md.setLast(basePrice + 2.0);
            item.setMarketData(md);
            fullSessionIndices.add(item);
        }

        when(marketIndexIndicesService.getByIndexSymbolAndTimeBetween(eq(symbol), eq(from), eq(to)))
                .thenReturn(fullSessionIndices);

        // Test 5m: 375 / 5 = 75 candles
        Optional<HistoricalData> res5m = historicalDataService.getHistoricalData(symbol, from, to, "5m", true);
        assertThat(res5m).isPresent();
        assertThat(res5m.get().getDataPoints()).hasSize(75);

        // Test 10m: 375 / 10 = 37.5 -> 38 candles (09:15, 09:25, ..., 15:25)
        Optional<HistoricalData> res10m = historicalDataService.getHistoricalData(symbol, from, to, "10m", true);
        assertThat(res10m).isPresent();
        assertThat(res10m.get().getDataPoints()).hasSize(38);

        // Test 15m: 375 / 15 = 25 candles (09:15, 09:30, ..., 15:15)
        Optional<HistoricalData> res15m = historicalDataService.getHistoricalData(symbol, from, to, "15m", true);
        assertThat(res15m).isPresent();
        assertThat(res15m.get().getDataPoints()).hasSize(25);

        // Test 30m: 375 / 30 = 12.5 -> 13 candles (09:15, 09:45, ..., 15:15)
        Optional<HistoricalData> res30m = historicalDataService.getHistoricalData(symbol, from, to, "30m", true);
        assertThat(res30m).isPresent();
        assertThat(res30m.get().getDataPoints()).hasSize(13);

        // Test 1H: 375 / 60 = 6.25 -> 7 candles (6 60-minute candles + 1 closing 15-minute candle)
        Optional<HistoricalData> res1h = historicalDataService.getHistoricalData(symbol, from, to, "1H", true);
        assertThat(res1h).isPresent();
        assertThat(res1h.get().getDataPoints()).hasSize(7);

        // Verify OHLC integrity on 15m candle 0 (covers minutes 0 through 14)
        OHLCVTPoint first15mCandle = res15m.get().getDataPoints().get(0);
        // First minute open was 24000.0
        assertThat(first15mCandle.getOpen()).isEqualTo(24000.0);
        // Max high was minute 14 high: 24000.0 + (14 * 0.5) + 5.0 = 24012.0
        assertThat(first15mCandle.getHigh()).isEqualTo(24012.0);
        // Min low was minute 0 low: 24000.0 - 5.0 = 23995.0
        assertThat(first15mCandle.getLow()).isEqualTo(23995.0);
        // Close was minute 14 close/last: 24000.0 + (14 * 0.5) + 2.0 = 24009.0
        assertThat(first15mCandle.getClose()).isEqualTo(24009.0);
    }

    @Test
    void shouldBuildBatchHistoricalDataFromOneEquityBatchRead() {
        Instant from = Instant.parse("2026-06-01T00:00:00Z");
        Instant to = Instant.parse("2026-06-03T00:00:00Z");
        List<EquityPrice> prices = List.of(
                equityPrice("TCS", "2026-06-02T10:00:00Z", 102.0),
                equityPrice("TCS", "2026-06-01T10:00:00Z", 100.0),
                equityPrice("INFY", "2026-06-01T10:00:00Z", 50.0));
        when(equityService.getPriceHistoryBySymbols(eq(List.of("TCS", "INFY")), eq(from), eq(to), eq("1d")))
                .thenReturn(prices);

        var result = historicalDataService.getHistoricalDataBatch(List.of("TCS", "INFY"), from, to, "1d", false);

        assertThat(result).containsKeys("TCS", "INFY");
        assertThat(result.get("TCS").getDataPoints()).hasSize(2);
        assertThat(result.get("TCS").getDataPoints().get(0).getClose()).isEqualTo(100.0);
        assertThat(result.get("TCS").getDataPoints().get(1).getClose()).isEqualTo(102.0);
        assertThat(result.get("INFY").getDataPoints()).hasSize(1);
        verify(equityService).getPriceHistoryBySymbols(List.of("TCS", "INFY"), from, to, "1d");
    }

    private EquityPrice equityPrice(String symbol, String timestamp, double close) {
        return EquityPrice.builder()
                .symbol(symbol)
                .time(Instant.parse(timestamp))
                .lastPrice(close)
                .ohlcv(OHLCVTPoint.builder().open(close).high(close).low(close).close(close).volume(10L).build())
                .build();
    }
}
