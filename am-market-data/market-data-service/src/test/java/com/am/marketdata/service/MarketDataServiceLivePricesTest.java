package com.am.marketdata.service;

import com.am.common.investment.model.equity.EquityPrice;
import com.am.common.investment.service.StockIndicesMarketDataService;
import com.am.common.investment.service.instrument.InstrumentService;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.mapper.InstrumentMapper;
import com.am.marketdata.mapper.MarketDataGenericMapper;
import com.am.marketdata.provider.common.MarketDataProviderFactory;
import com.am.marketdata.service.kafka.producer.MarketDataProducer;
import com.am.marketdata.service.util.MarketDataRetrievalUtil;
import com.am.observability.flow.FlowLogger;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class MarketDataServiceLivePricesTest {

    @Test
    void marketOpenLivePricesUseCurrentTickCacheInsteadOfHistoricalOhlc() {
        MarketDataCacheService cacheService = mock(MarketDataCacheService.class);
        MarketDataPersistenceService persistenceService = mock(MarketDataPersistenceService.class);
        MarketHoursService marketHoursService = mock(MarketHoursService.class);
        when(marketHoursService.isMarketOpen()).thenReturn(true);

        OHLCQuote quote = OHLCQuote.builder()
                .lastPrice(24398.10)
                .previousClose(25044.65)
                .ohlc(OHLCQuote.OHLC.builder()
                        .open(25000.0)
                        .high(25100.0)
                        .low(24300.0)
                        .close(24398.10)
                        .build())
                .build();
        when(cacheService.getLatestPrices(java.util.Set.of("NIFTY 50")))
                .thenReturn(Map.of("NIFTY 50", quote));

        MarketDataService service = new MarketDataService(
                mock(MarketDataProviderFactory.class),
                mock(InstrumentService.class),
                new SimpleMeterRegistry(),
                mock(InstrumentMapper.class),
                mock(MarketDataGenericMapper.class),
                persistenceService,
                mock(MarketDataRetrievalUtil.class),
                Optional.<MarketDataProducer>empty(),
                mock(StockIndicesMarketDataService.class),
                mock(FlowLogger.class),
                Optional.of(marketHoursService),
                cacheService);

        List<EquityPrice> prices = service.getLivePrices(List.of("NIFTY 50"), null, false);

        assertEquals(1, prices.size());
        assertEquals(24398.10, prices.get(0).getLastPrice(), 0.001);
        verify(persistenceService, never()).getOHLCData(anyList(), any(), anyBoolean());
    }
}
