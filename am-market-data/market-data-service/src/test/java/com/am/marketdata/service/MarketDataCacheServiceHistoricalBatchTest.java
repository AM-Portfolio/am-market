package com.am.marketdata.service;

import com.am.common.investment.model.historical.HistoricalData;
import com.am.common.investment.model.historical.OHLCVTPoint;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.redis.model.StockBars;
import com.am.marketdata.redis.service.StockCacheService;
import com.am.marketdata.service.model.PreviousCloseDocument;
import com.am.marketdata.service.repo.PreviousCloseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class MarketDataCacheServiceHistoricalBatchTest {

    @Test
    void usesOneDatedBaselineReadForIndexAliasesDuringAStreamBurst() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        String today = LocalDate.now(ZoneId.of("Asia/Kolkata")).toString();
        when(values.multiGet(anyList())).thenReturn(Arrays.asList("24000.0", today));
        MarketDataCacheService cache = new MarketDataCacheService(
                mock(StockCacheService.class), new ObjectMapper(), redis, null, null, null);

        assertEquals(24000.0, cache.getPreviousClose("NSE_INDEX|Nifty 500"));
        assertEquals(24000.0, cache.getPreviousClose("NIFTY 500"));
        verify(values, times(1)).multiGet(anyList());
    }

    @Test
    void doesNotUseYesterdayMongoBaselineForTodayLiveQuote() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        PreviousCloseRepository repository = mock(PreviousCloseRepository.class);
        when(repository.findById("TCS")).thenReturn(Optional.of(PreviousCloseDocument.builder()
                .symbol("TCS")
                .previousClose(24000.0)
                .tradeDate(LocalDate.now(ZoneId.of("Asia/Kolkata")).minusDays(1).toString())
                .build()));
        MarketDataCacheService cache = new MarketDataCacheService(
                mock(StockCacheService.class), new ObjectMapper(), redis, repository, null, null);

        assertNull(cache.getPreviousClose("TCS"));
    }

    @Test
    void convertsMultipleSymbolsAndSubmitsOneHistoricalRedisBatch() {
        StockCacheService stockCacheService = mock(StockCacheService.class);
        when(stockCacheService.cacheHistoricalDataBatch(anyList())).thenReturn(true);
        MarketDataCacheService cacheService = new MarketDataCacheService(
                stockCacheService, new ObjectMapper(), null, null, null, null);

        HistoricalData history = HistoricalData.builder()
                .dataPoints(List.of(OHLCVTPoint.builder()
                        .time(LocalDateTime.of(2026, 10, 21, 0, 0))
                        .open(100.0).high(105.0).low(99.0).close(103.0).volume(1000L)
                        .build()))
                .build();

        cacheService.cacheHistoricalDataBatch(Map.of("TCS", history, "INFY", history), TimeFrame.DAY);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<StockBars>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(stockCacheService, times(1)).cacheHistoricalDataBatch(captor.capture());
        List<StockBars> cached = captor.getValue();
        assertEquals(2, cached.size());
        assertEquals("1D", cached.get(0).getInterval());
        assertEquals(1, cached.get(0).getBars().size());
    }

    @Test
    void skipsEmptyHistoryWithoutCallingRedis() {
        StockCacheService stockCacheService = mock(StockCacheService.class);
        MarketDataCacheService cacheService = new MarketDataCacheService(
                stockCacheService, new ObjectMapper(), null, null, null, null);

        cacheService.cacheHistoricalDataBatch(Map.of("TCS", HistoricalData.builder().dataPoints(List.of()).build()),
                TimeFrame.DAY);

        verifyNoInteractions(stockCacheService);
    }

    @Test
    void splitsLargeHistoryResponsesIntoBoundedFiftySymbolPipelines() {
        StockCacheService stockCacheService = mock(StockCacheService.class);
        when(stockCacheService.cacheHistoricalDataBatch(anyList())).thenReturn(true);
        MarketDataCacheService cacheService = new MarketDataCacheService(
                stockCacheService, new ObjectMapper(), null, null, null, null);
        HistoricalData history = HistoricalData.builder()
                .dataPoints(List.of(OHLCVTPoint.builder()
                        .time(LocalDateTime.of(2026, 10, 21, 0, 0))
                        .open(100.0).high(105.0).low(99.0).close(103.0).volume(1000L)
                        .build()))
                .build();
        Map<String, HistoricalData> response = IntStream.range(0, 51)
                .boxed()
                .collect(Collectors.toMap(i -> "S" + i, i -> history));

        cacheService.cacheHistoricalDataBatch(response, TimeFrame.DAY);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<StockBars>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(stockCacheService, times(2)).cacheHistoricalDataBatch(captor.capture());
        assertEquals(50, captor.getAllValues().get(0).size());
        assertEquals(1, captor.getAllValues().get(1).size());
    }
}
