package com.am.marketdata.service;

import com.am.common.investment.service.EquityLatestPriceService;
import com.am.common.investment.service.EquityService;
import com.am.common.investment.service.MarketIndexIndicesService;
import com.am.common.investment.service.StockIndicesMarketDataService;
import com.am.common.investment.service.historical.HistoricalDataService;
import com.am.marketdata.common.mapper.OHLCMapper;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.service.global.GlobalIndexCacheWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MarketDataPersistenceServiceRoutingTest {

    private final ThreadPoolTaskExecutor executor = executor();

    @AfterEach
    void stopExecutor() {
        executor.shutdown();
    }

    @Test
    void routesIndexTicksToIndexPersistenceAndEquityTicksToEquityPersistence() {
        HistoricalDataService historicalDataService = mock(HistoricalDataService.class);
        MarketDataCacheService cacheService = mock(MarketDataCacheService.class);
        EquityService equityService = mock(EquityService.class);
        EquityLatestPriceService latestPriceService = mock(EquityLatestPriceService.class);
        OHLCMapper mapper = mock(OHLCMapper.class);
        MarketIndexIndicesService indexService = mock(MarketIndexIndicesService.class);
        StockIndicesMarketDataService indexMetadataService = mock(StockIndicesMarketDataService.class);
        GlobalIndexCacheWriter globalIndexCacheWriter = mock(GlobalIndexCacheWriter.class);

        when(indexMetadataService.findByIndexSymbols(anySet())).thenReturn(List.of());
        when(mapper.toEquityPriceList(anyMap(), any())).thenReturn(List.of());

        MarketDataPersistenceService persistenceService = new MarketDataPersistenceService(
                historicalDataService,
                cacheService,
                equityService,
                latestPriceService,
                mapper,
                indexService,
                indexMetadataService,
                globalIndexCacheWriter,
                executor);

        Map<String, OHLCQuote> quotes = Map.of(
                "NIFTY 50", quote(100.0),
                "NSE_EQ:RELIANCE", quote(200.0));

        persistenceService.saveOHLCData(quotes, "UPSTOX").join();

        verify(indexService).save(any());
        org.mockito.ArgumentCaptor<Map<String, OHLCQuote>> mapperInput =
                org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(mapper).toEquityPriceList(mapperInput.capture(), eq("UPSTOX"));
        assertEquals(Set.of("NSE_EQ:RELIANCE"), mapperInput.getValue().keySet());
        verify(equityService).saveAllPrices(any());
        verifyNoInteractions(globalIndexCacheWriter);
    }

    private static OHLCQuote quote(double lastPrice) {
        return OHLCQuote.builder()
                .lastPrice(lastPrice)
                .previousClose(lastPrice - 1)
                .ohlc(OHLCQuote.OHLC.builder()
                        .open(lastPrice - 2)
                        .high(lastPrice + 2)
                        .low(lastPrice - 3)
                        .close(lastPrice)
                        .build())
                .build();
    }

    private static ThreadPoolTaskExecutor executor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(4);
        executor.initialize();
        return executor;
    }
}
