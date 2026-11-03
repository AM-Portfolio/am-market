package com.am.marketdata.service.websocket.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.am.common.investment.persistence.document.global.GlobalIndexConfigRepository;
import com.am.marketdata.common.MarketDataStreamer;
import com.am.marketdata.common.service.MarketDataPublisher;
import com.am.marketdata.service.MarketDataCacheService;
import com.am.marketdata.service.MarketDataPersistenceService;
import com.am.marketdata.service.MarketHoursService;
import com.am.marketdata.service.SymbolOrchestratorService;
import com.am.marketdata.service.global.GlobalIndexCacheWriter;
import com.am.marketdata.service.global.GlobalMarketScheduleService;
import com.am.marketdata.service.websocket.processor.MarketDataProcessor;
import java.util.concurrent.Executor;
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
    @Mock
    private Executor cacheBackfillExecutor;

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
                cacheBackfillExecutor);
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
}
