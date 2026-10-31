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
    private com.am.common.investment.persistence.repository.measurement.impl.GlobalIndexInfluxRepository globalIndexInfluxRepository;

    @Mock
    private com.am.common.investment.persistence.document.global.GlobalIndexConfigRepository globalIndexConfigRepository;

    private HistoricalDataServiceImpl historicalDataService;

    @BeforeEach
    void setUp() {
        historicalDataService = new HistoricalDataServiceImpl(
                equityService, 
                marketIndexIndicesService,
                globalIndexInfluxRepository,
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
