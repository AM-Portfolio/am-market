package com.am.marketdata.api.service.impl;

import com.am.common.investment.model.historical.HistoricalData;
import com.am.common.investment.model.historical.OHLCVTPoint;
import com.am.common.investment.model.stockindice.StockIndicesMarketData;
import com.am.common.investment.service.StockIndicesMarketDataService;
import com.am.marketdata.api.util.InstrumentUtils;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.service.MarketDataService;
import com.am.marketdata.service.MarketHoursService;
import com.am.marketdata.service.calendar.MarketCalendarService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketDataFetchServiceRoutingTest {

    @Test
    void routesPlainIndexNamesToIndexHistoricalMeasurementPath() {
        MarketDataService marketDataService = mock(MarketDataService.class);
        StockIndicesMarketDataService indexService = mock(StockIndicesMarketDataService.class);
        InstrumentUtils instrumentUtils = mock(InstrumentUtils.class);
        MarketHoursService marketHoursService = mock(MarketHoursService.class);
        MarketCalendarService calendarService = mock(MarketCalendarService.class);

        when(instrumentUtils.resolveSymbols(anyList(), eq(false)))
                .thenReturn(Set.of("NIFTY 50"));
        when(marketDataService.getHistoricalDataBatch(
                anyList(), any(Date.class), any(Date.class), eq(TimeFrame.MINUTE), eq(false),
                anyMap(), isNull(), eq(true), eq(false)))
                .thenReturn(Map.of("NIFTY 50", historicalData()));

        MarketDataFetchServiceImpl service = new MarketDataFetchServiceImpl(
                java.util.Optional.empty(),
                marketDataService,
                indexService,
                instrumentUtils,
                marketHoursService,
                calendarService);

        service.getHistoricalDataMultipleSymbols(
                Set.of("NIFTY 50"),
                new Date(0),
                new Date(1_000),
                TimeFrame.MINUTE,
                "STOCK",
                Map.of("isIndexSymbol", true),
                false,
                false);

        ArgumentCaptor<Boolean> indexFlag = ArgumentCaptor.forClass(Boolean.class);
        verify(marketDataService).getHistoricalDataBatch(
                anyList(), any(Date.class), any(Date.class), eq(TimeFrame.MINUTE), eq(false),
                anyMap(), isNull(), indexFlag.capture(), eq(false));
        assertTrue(indexFlag.getValue(), "plain index names must use the index repository path");
    }

    @Test
    void detectsPlainIndexNamesFromIndexMetadataWhenCallerOmitsType() {
        MarketDataService marketDataService = mock(MarketDataService.class);
        StockIndicesMarketDataService indexService = mock(StockIndicesMarketDataService.class);
        InstrumentUtils instrumentUtils = mock(InstrumentUtils.class);
        MarketHoursService marketHoursService = mock(MarketHoursService.class);
        MarketCalendarService calendarService = mock(MarketCalendarService.class);

        when(instrumentUtils.resolveSymbols(anyList(), eq(false)))
                .thenReturn(Set.of("NIFTY 50"));
        when(indexService.findByIndexSymbols(anySet()))
                .thenReturn(List.of(StockIndicesMarketData.builder().indexSymbol("NIFTY 50").build()));
        when(marketDataService.getHistoricalDataBatch(
                anyList(), any(Date.class), any(Date.class), eq(TimeFrame.MINUTE), eq(false),
                anyMap(), isNull(), eq(true), eq(false)))
                .thenReturn(Map.of("NIFTY 50", historicalData()));

        MarketDataFetchServiceImpl service = new MarketDataFetchServiceImpl(
                java.util.Optional.empty(),
                marketDataService,
                indexService,
                instrumentUtils,
                marketHoursService,
                calendarService);

        service.getHistoricalDataMultipleSymbols(
                Set.of("NIFTY 50"),
                new Date(0),
                new Date(1_000),
                TimeFrame.MINUTE,
                "STOCK",
                Map.of(),
                false,
                false);

        verify(marketDataService).getHistoricalDataBatch(
                anyList(), any(Date.class), any(Date.class), eq(TimeFrame.MINUTE), eq(false),
                anyMap(), isNull(), eq(true), eq(false));
    }

    private static HistoricalData historicalData() {
        return HistoricalData.builder()
                .dataPoints(List.of(OHLCVTPoint.builder()
                        .time(LocalDateTime.of(2026, 10, 30, 9, 15))
                        .open(100.0)
                        .high(101.0)
                        .low(99.0)
                        .close(100.5)
                        .volume(1L)
                        .build()))
                .build();
    }
}
