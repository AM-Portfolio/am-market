package com.am.marketdata.scraper.service;

import com.am.marketdata.common.model.NSEStockInsidicesData;
import com.am.marketdata.kafka.producer.KafkaProducerService;
import com.am.marketdata.redis.cache.StockRedisCache;
import com.am.marketdata.scraper.client.NSEApiClient;
import com.am.marketdata.scraper.config.NSEIndicesConfig;
import com.am.marketdata.scraper.cookie.CookieManager;
import com.am.common.investment.service.MarketIndexIndicesService;
import com.am.common.investment.service.StockIndicesMarketDataService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class MarketDataProcessingMembershipValidationTest {
    private static final DateTimeFormatter TRADE_DATE = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm", Locale.ENGLISH);

    @Test
    void deduplicatesSameTickerAndIdentityBeforeValidatingMinimum() {
        MarketDataProcessingService service = service();
        List<NSEStockInsidicesData.StockData> data = new ArrayList<>();
        data.add(stock("TCS", "INE467B01029"));
        data.add(stock(" tcs ", "INE467B01029"));
        for (int i = 0; i < 48; i++) {
            data.add(stock("MEMBER" + i, "IN" + String.format("%010d", i)));
        }
        NSEStockInsidicesData response = response(data);

        assertTrue(invokeValidation(service, response));
        assertEquals(49, response.getData().size());
    }

    @Test
    void rejectsDuplicateTickerWithConflictingInstrumentIdentity() {
        MarketDataProcessingService service = service();
        NSEStockInsidicesData response = response(List.of(
                stock("TCS", "INE467B01029"), stock("TCS", "INE467B01999")));

        assertFalse(invokeValidation(service, response));
    }

    private static MarketDataProcessingService service() {
        return new MarketDataProcessingService(
                mock(NSEApiClient.class), Optional.<KafkaProducerService>empty(),
                mock(MarketIndexIndicesService.class), mock(StockIndicesMarketDataService.class),
                mock(MeterRegistry.class), mock(CookieManager.class), mock(NSEIndicesConfig.class),
                mock(StockRedisCache.class));
    }

    private static NSEStockInsidicesData response(List<NSEStockInsidicesData.StockData> data) {
        return NSEStockInsidicesData.builder()
                .name("NIFTY 50")
                .data(new ArrayList<>(data))
                .marketStatus(NSEStockInsidicesData.MarketStatus.builder()
                        .tradeDate(TRADE_DATE.format(LocalDateTime.now()))
                        .marketStatus("Open")
                        .build())
                .build();
    }

    private static NSEStockInsidicesData.StockData stock(String symbol, String isin) {
        return NSEStockInsidicesData.StockData.builder()
                .symbol(symbol)
                .meta(NSEStockInsidicesData.Metadata.builder().isin(isin).build())
                .build();
    }

    private static boolean invokeValidation(MarketDataProcessingService service, NSEStockInsidicesData response) {
        Boolean result = ReflectionTestUtils.invokeMethod(service, "validateStockIndicesData", response);
        return Boolean.TRUE.equals(result);
    }
}
