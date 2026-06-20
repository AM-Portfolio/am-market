package com.am.common.investment.service.historical.impl;

import com.am.common.investment.model.equity.EquityPrice;
import com.am.common.investment.model.equity.MarketIndexIndices;
import com.am.common.investment.model.equity.MarketData;
import com.am.common.investment.model.historical.HistoricalData;
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

@ExtendWith(MockitoExtension.class)
class HistoricalDataServiceImplTest {

    @Mock
    private EquityService equityService;

    @Mock
    private MarketIndexIndicesService marketIndexIndicesService;

    private HistoricalDataServiceImpl historicalDataService;

    @BeforeEach
    void setUp() {
        historicalDataService = new HistoricalDataServiceImpl(equityService, marketIndexIndicesService);
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
}
