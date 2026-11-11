package com.am.marketdata.service.websocket.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.am.common.investment.persistence.document.global.GlobalIndexConfigRepository;
import com.am.marketdata.common.MarketDataStreamer;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.service.MarketDataPublisher;
import com.am.marketdata.service.MarketDataCacheService;
import com.am.marketdata.service.MarketDataPersistenceService;
import com.am.marketdata.service.MarketHoursService;
import com.am.marketdata.service.SymbolOrchestratorService;
import com.am.marketdata.service.global.GlobalIndexCacheWriter;
import com.am.marketdata.service.global.GlobalMarketScheduleService;
import com.am.marketdata.service.websocket.processor.MarketDataProcessor;
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
                Runnable::run);
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
