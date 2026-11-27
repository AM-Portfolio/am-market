package com.am.marketdata.service.websocket.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.am.common.investment.persistence.document.global.GlobalIndexConfigRepository;
import com.am.marketdata.common.MarketDataStreamer;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.MarketDataUpdate;
import com.am.marketdata.common.service.MarketDataPublisher;
import com.am.marketdata.service.MarketDataCacheService;
import com.am.marketdata.service.MarketDataPersistenceService;
import com.am.marketdata.service.MarketHoursService;
import com.am.marketdata.service.SymbolOrchestratorService;
import com.am.marketdata.service.global.GlobalIndexCacheWriter;
import com.am.marketdata.service.global.GlobalMarketScheduleService;
import com.am.marketdata.service.websocket.processor.MarketDataProcessor;
import com.am.marketdata.provider.upstox.converter.UpstoxV3FeedConverter;
import com.upstox.feeder.MarketUpdateV3;
import java.util.concurrent.Executor;
import java.util.Map;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;

class StreamerManagerResilienceTest {

    @Mock
    private MarketDataStreamer streamer;
    @Mock
    private MarketDataPersistenceService persistenceService;
    @Mock
    private MarketDataProcessor processor;
    @Mock
    private SymbolOrchestratorService symbolService;
    @Mock
    private MarketDataPublisher publisher;
    @Mock
    private MarketDataCacheService cacheService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private GlobalIndexCacheWriter globalIndexCacheWriter;
    @Mock
    private GlobalIndexConfigRepository globalIndexConfigRepository;
    @Mock
    private GlobalMarketScheduleService globalMarketScheduleService;
    @Mock
    private MarketHoursService marketHoursService;
    @Mock
    private UpstoxV3FeedConverter v3FeedConverter;
    private StreamerManager manager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("false");
        when(globalMarketScheduleService.isAnyMarketCurrentlyOpen(any())).thenReturn(false);
        when(marketHoursService.isMarketOpen()).thenReturn(false);
        when(streamer.isConnected()).thenReturn(false);

        manager = new StreamerManager(
                streamer,
                persistenceService,
                processor,
                symbolService,
                publisher,
                cacheService,
                redisTemplate,
                globalIndexCacheWriter,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                globalIndexConfigRepository,
                globalMarketScheduleService,
                marketHoursService,
                Runnable::run,
                v3FeedConverter);
        ReflectionTestUtils.setField(manager, "ingestionEnabled", true);
        ReflectionTestUtils.setField(manager, "reconnectInitialDelayMs", 1000L);
        ReflectionTestUtils.setField(manager, "reconnectMaxDelayMs", 5000L);
        ReflectionTestUtils.setField(manager, "noTickTimeoutMs", 1000L);
    }

    @Test
    void redisVoteFailureDoesNotDisconnectAnActiveStream() {
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis unavailable"));
        when(streamer.isConnected()).thenReturn(true);

        manager.reconcileWebSocketConnection();

        verify(streamer, never()).disconnect();
    }

    @Test
    void localIndianCalendarCanStartStreamWhenRedisVotesAreFalse() {
        when(marketHoursService.isMarketOpen()).thenReturn(true);

        manager.reconcileWebSocketConnection();

        verify(streamer).setListener(manager);
        verify(streamer).connect();
    }

    @Test
    void disabledIngestionStopsTheStreamWithoutReadingRedisVotes() {
        ReflectionTestUtils.setField(manager, "ingestionEnabled", false);
        when(streamer.isConnected()).thenReturn(true);

        manager.reconcileWebSocketConnection();

        verify(streamer).disconnect();
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void connectedStreamIsNotReadyUntilARealTickArrives() {
        when(streamer.isConnected()).thenReturn(true);
        when(cacheService.cacheLatestPrices(any())).thenReturn(true);

        manager.onOpen();

        org.junit.jupiter.api.Assertions.assertFalse(manager.isStreaming());

        OHLCQuote quote = OHLCQuote.builder()
                .lastPrice(25000.0)
                .previousClose(24900.0)
                .quoteTimestamp(Instant.now())
                .build();
        manager.onMessage(Map.of("NIFTY 50", quote));

        org.junit.jupiter.api.Assertions.assertTrue(manager.isStreaming());
    }

    @Test
    void invalidOrStaleTickDoesNotMarkStreamReady() {
        when(streamer.isConnected()).thenReturn(true);
        manager.onOpen();

        OHLCQuote quote = OHLCQuote.builder()
                .lastPrice(0.0)
                .quoteTimestamp(Instant.now())
                .build();
        manager.onMessage(Map.of("NIFTY 50", quote));

        org.junit.jupiter.api.Assertions.assertFalse(manager.isStreaming());
        verify(cacheService, never()).cacheLatestPrices(any());
    }

    @Test
    void cacheFailureKeepsStreamUnreadySoFallbackCanRun() {
        when(streamer.isConnected()).thenReturn(true);
        when(cacheService.cacheLatestPrices(any())).thenReturn(false);
        manager.onOpen();

        OHLCQuote quote = OHLCQuote.builder()
                .lastPrice(25000.0)
                .quoteTimestamp(Instant.now())
                .build();
        manager.onMessage(Map.of("NIFTY 50", quote));

        org.junit.jupiter.api.Assertions.assertFalse(manager.isStreaming());
    }

    @Test
    void savedBaselineEnrichesAnIndexQuoteBeforeCacheAndKafka() {
        when(cacheService.getPreviousClose("NIFTY 500")).thenReturn(24000.0);
        when(cacheService.cacheLatestPrices(any())).thenReturn(true);
        OHLCQuote quote = OHLCQuote.builder()
                .lastPrice(23852.55)
                .quoteTimestamp(Instant.now())
                .build();

        manager.onMessage(Map.of("NIFTY 500", quote));

        org.junit.jupiter.api.Assertions.assertEquals(24000.0, quote.getPreviousClose());
        org.junit.jupiter.api.Assertions.assertEquals(-147.45, quote.getChange(), 0.001);
        org.junit.jupiter.api.Assertions.assertEquals(-0.614375, quote.getChangePercent(), 0.000001);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, OHLCQuote>> kafkaInput = ArgumentCaptor.forClass(Map.class);
        verify(processor).processUpdate(kafkaInput.capture());
        org.junit.jupiter.api.Assertions.assertSame(quote, kafkaInput.getValue().get("NIFTY 500"));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, OHLCQuote>> cacheInput = ArgumentCaptor.forClass(Map.class);
        verify(cacheService).cacheLatestPrices(cacheInput.capture());
        org.junit.jupiter.api.Assertions.assertSame(quote, cacheInput.getValue().get("NIFTY 500"));
        ArgumentCaptor<MarketDataUpdate> published = ArgumentCaptor.forClass(MarketDataUpdate.class);
        verify(publisher).publish(published.capture());
        org.junit.jupiter.api.Assertions.assertEquals(-147.45,
                published.getValue().getQuotes().get("NIFTY 500").getChange(), 0.001);
    }

    @Test
    void providerChangeCanSupplyTheBaselineWhenPreviousCloseIsAbsent() {
        when(cacheService.getPreviousClose("NIFTY 50")).thenReturn(null);
        when(cacheService.cacheLatestPrices(any())).thenReturn(true);
        OHLCQuote quote = OHLCQuote.builder()
                .lastPrice(25838.95)
                .change(38.35)
                .quoteTimestamp(Instant.now())
                .build();

        manager.onMessage(Map.of("NIFTY 50", quote));

        org.junit.jupiter.api.Assertions.assertEquals(25800.60, quote.getPreviousClose(), 0.001);
        org.junit.jupiter.api.Assertions.assertEquals(38.35, quote.getChange(), 0.001);
        org.junit.jupiter.api.Assertions.assertEquals(0.14864, quote.getChangePercent(), 0.00001);
    }

    @Test
    void missingBaselineRemainsUnavailableInsteadOfPublishingFalseZero() {
        when(cacheService.getPreviousClose("TCS")).thenReturn(null);
        when(cacheService.cacheLatestPrices(any())).thenReturn(true);
        OHLCQuote quote = OHLCQuote.builder()
                .lastPrice(2450.0)
                .quoteTimestamp(Instant.now())
                .build();

        manager.onMessage(Map.of("TCS", quote));

        org.junit.jupiter.api.Assertions.assertNull(quote.getChange());
        org.junit.jupiter.api.Assertions.assertNull(quote.getChangePercent());
        ArgumentCaptor<MarketDataUpdate> published = ArgumentCaptor.forClass(MarketDataUpdate.class);
        verify(publisher).publish(published.capture());
        org.junit.jupiter.api.Assertions.assertNull(
                published.getValue().getQuotes().get("TCS").getChangePercent());
    }

    @Test
    void marketUpdateV3UsesTheSameEnrichmentAndPersistencePath() {
        when(cacheService.getPreviousClose("NIFTY 500")).thenReturn(24000.0);
        when(cacheService.cacheLatestPrices(any())).thenReturn(true);
        OHLCQuote quote = OHLCQuote.builder()
                .lastPrice(23852.55)
                .quoteTimestamp(Instant.now())
                .build();
        when(v3FeedConverter.convert(any(MarketUpdateV3.class)))
                .thenReturn(Map.of("NIFTY 500", quote));

        manager.onMessage(org.mockito.Mockito.mock(MarketUpdateV3.class));

        org.junit.jupiter.api.Assertions.assertEquals(-147.45, quote.getChange(), 0.001);
        org.junit.jupiter.api.Assertions.assertEquals(-0.614375, quote.getChangePercent(), 0.000001);
        ArgumentCaptor<MarketDataUpdate> published = ArgumentCaptor.forClass(MarketDataUpdate.class);
        verify(publisher).publish(published.capture());
        org.junit.jupiter.api.Assertions.assertEquals(-0.614375,
                published.getValue().getQuotes().get("NIFTY 500").getChangePercent(), 0.000001);
        verify(processor).processUpdate(any());
    }

    @Test
    void duplicateConnectRequestsAreCoalescedWhileHandshakeIsInFlight() {
        when(streamer.isConnected()).thenReturn(false);

        manager.startStreaming();
        manager.startStreaming();

        verify(streamer, org.mockito.Mockito.times(1)).connect();
    }

    @Test
    void externalSubscriptionsUseTheSameSingleFlightConnectionGuard() {
        when(streamer.isConnected()).thenReturn(false);

        manager.subscribe(java.util.Set.of("NIFTY 50"));
        manager.subscribe(java.util.Set.of("NIFTY BANK"));

        verify(streamer, org.mockito.Mockito.times(1)).connect();
        verify(streamer, org.mockito.Mockito.times(1)).setListener(manager);
    }

    @Test
    void lateOpenAfterStopDisconnectsWithoutResubscribing() {
        when(streamer.isConnected()).thenReturn(false);

        manager.startStreaming();
        manager.stopStreaming();
        manager.onOpen();

        verify(streamer).disconnect();
        verify(streamer, never()).subscribe(any(), anyString());
    }

    @Test
    void openCallbackDoesNotResurrectDisabledIngestion() {
        ReflectionTestUtils.setField(manager, "ingestionEnabled", false);

        manager.onOpen();

        verify(streamer).disconnect();
        verify(streamer, never()).subscribe(any(), anyString());
    }
}
