package com.am.marketdata.service;

import com.am.common.investment.model.historical.HistoricalData;
import com.am.common.investment.model.historical.OHLCVTPoint;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.InstrumentIdentity;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.service.model.security.SecurityDocument;
import com.am.marketdata.service.repo.SecurityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartStockServiceTest {

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private MarketDataCacheService marketDataCacheService;

    private SmartStockService smartStockService;

    @BeforeEach
    void setUp() {
        smartStockService = new SmartStockService(marketDataService, marketDataCacheService);
    }

    private static OHLCVTPoint createCandle(LocalDate date, double open, double high, double low, double close) {
        return OHLCVTPoint.builder()
                .time(LocalDateTime.of(date, LocalTime.of(15, 30)))
                .open(open)
                .high(high)
                .low(low)
                .close(close)
                .volume(10000L)
                .build();
    }

    @Test
    void missingCurrentQuoteDoesNotUseHistoricalCandleAsLivePrice() {
        when(marketDataService.getOHLC(eq(List.of("RELIANCE")), eq(TimeFrame.DAY), eq(false), isNull(), eq(false)))
                .thenReturn(Collections.emptyMap()); // Redis cache miss

        // Historical data may be used as a comparison base, never as today's LTP.
        Map<String, OHLCQuote> quotes = smartStockService.getSmartQuotes(List.of("RELIANCE"));

        assertNotNull(quotes);
        assertTrue(quotes.isEmpty());
        verify(marketDataService, never()).getHistoricalDataBatch(
                anyList(), any(Date.class), any(Date.class), any(), anyBoolean(), any(), any(), anyBoolean(), anyBoolean());
        verify(marketDataCacheService, never()).cacheLatestPrices(anyMap());
    }

    @Test
    void missingIpoQuoteIsNotSynthesizedFromItsOnlyHistoricalCandle() {
        when(marketDataService.getOHLC(eq(List.of("NEWIPO")), eq(TimeFrame.DAY), eq(false), isNull(), eq(false)))
                .thenReturn(Collections.emptyMap());

        Map<String, OHLCQuote> quotes = smartStockService.getSmartQuotes(List.of("NEWIPO"));

        assertNotNull(quotes);
        assertTrue(quotes.isEmpty());
        verify(marketDataService, never()).getHistoricalDataBatch(
                anyList(), any(Date.class), any(Date.class), any(), anyBoolean(), any(), any(), anyBoolean(), anyBoolean());
    }

    @Test
    void timeframeUsesLastCloseOnOrBeforePeriodBoundary() {
        OHLCQuote liveQuote = OHLCQuote.builder()
                .lastPrice(210.0)
                .previousClose(205.0)
                .ohlc(OHLCQuote.OHLC.builder().open(205.0).high(211.0).low(204.0).close(210.0).build())
                .build();
        when(marketDataService.getOHLC(eq(List.of("ABC")), eq(TimeFrame.DAY), eq(false), isNull(), eq(false)))
                .thenReturn(Map.of("ABC", liveQuote));

        LocalDate boundary = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).minusMonths(1);
        HistoricalData history = HistoricalData.builder()
                .dataPoints(List.of(
                        createCandle(boundary.minusDays(1), 98.0, 101.0, 97.0, 100.0),
                        createCandle(boundary.plusDays(2), 118.0, 121.0, 117.0, 120.0)))
                .build();
        when(marketDataService.getHistoricalDataBatch(
                eq(List.of("ABC")), any(Date.class), any(Date.class), eq(TimeFrame.DAY),
                eq(false), isNull(), isNull(), eq(false), eq(false), eq(false)))
                .thenReturn(Map.of("ABC", history));

        Map<String, OHLCQuote> result = smartStockService.getSmartQuotes(List.of("ABC"), TimeFrame.MONTH);

        assertEquals(100.0, result.get("ABC").getPreviousClose(), 0.001);
        verify(marketDataService).getHistoricalDataBatch(
                eq(List.of("ABC")), any(Date.class), any(Date.class), eq(TimeFrame.DAY),
                eq(false), isNull(), isNull(), eq(false), eq(false), eq(false));
    }

    @Test
    void missingCurrentQuoteUsesOneRedisLeasedBatchOhlcRecovery() {
        when(marketDataService.getOHLC(eq(List.of("ABC")), eq(TimeFrame.DAY), eq(false), isNull(), eq(false)))
                .thenReturn(Collections.emptyMap());
        OHLCQuote recovered = OHLCQuote.builder()
                .lastPrice(101.5)
                .previousClose(100.0)
                .ohlc(OHLCQuote.OHLC.builder().open(99.0).high(102.0).low(98.0).close(101.5).build())
                .build();

        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(org.springframework.data.redis.core.script.RedisScript.class), anyList(), any(), any()))
                .thenReturn(List.of(1));
        ReflectionTestUtils.setField(smartStockService, "stringRedisTemplate", redis);
        when(marketDataService.getOHLCForIdentities(anyList(), isNull()))
                .thenReturn(Map.of("ABC", recovered));

        Map<String, OHLCQuote> quotes = smartStockService.getSmartQuotes(List.of("ABC"));

        assertEquals(101.5, quotes.get("ABC").getLastPrice(), 0.001);
        assertEquals(100.0, quotes.get("ABC").getPreviousClose(), 0.001);
        assertEquals(101.5, quotes.get("ABC").getOhlc().getClose(), 0.001);
        verify(marketDataService).getOHLCForIdentities(argThat(identities ->
                identities.size() == 1 && identities.get(0).symbol().equals("ABC")), isNull());
        verify(marketDataCacheService).cacheLatestPrices(argThat(map ->
                map.containsKey("ABC") && map.get("ABC").getLastPrice() == 101.5));
    }

    @Test
    void missingMemberQuoteUsesIsinOnlyWhenSecurityRegistryConfirmsTickerPair() {
        when(marketDataCacheService.getLatestPrices(eq(Set.of("TECHM")))).thenReturn(Collections.emptyMap());
        when(marketDataService.getOHLC(eq(List.of("TECHM")), eq(TimeFrame.DAY), eq(false), isNull(), eq(false)))
                .thenReturn(Collections.emptyMap());
        SecurityRepository securities = mock(SecurityRepository.class);
        when(securities.findBySymbolIn(List.of("TECHM"))).thenReturn(List.of(security("TECHM", "INE669C01036")));
        ReflectionTestUtils.setField(smartStockService, "securityRepository", securities);
        setQuoteRecoveryLease();
        when(marketDataService.getOHLCForIdentities(anyList(), isNull())).thenReturn(Map.of("TECHM",
                OHLCQuote.builder().lastPrice(101.5).previousClose(100.0).build()));

        smartStockService.getSmartQuotesForIdentities(
                List.of(new InstrumentIdentity("TECHM", "INE669C01036", "NSE")), TimeFrame.DAY);

        verify(marketDataService).getOHLCForIdentities(argThat(identities ->
                identities.size() == 1 && identities.get(0).isin().equals("INE669C01036")), isNull());
    }

    @Test
    void missingMemberQuoteDisablesIsinFallbackWhenSecurityRegistryDisagrees() {
        when(marketDataCacheService.getLatestPrices(eq(Set.of("TECHM")))).thenReturn(Collections.emptyMap());
        when(marketDataService.getOHLC(eq(List.of("TECHM")), eq(TimeFrame.DAY), eq(false), isNull(), eq(false)))
                .thenReturn(Collections.emptyMap());
        SecurityRepository securities = mock(SecurityRepository.class);
        when(securities.findBySymbolIn(List.of("TECHM"))).thenReturn(List.of(security("TECHM", "INE467B01029")));
        ReflectionTestUtils.setField(smartStockService, "securityRepository", securities);
        setQuoteRecoveryLease();
        when(marketDataService.getOHLCForIdentities(anyList(), isNull())).thenReturn(Collections.emptyMap());

        smartStockService.getSmartQuotesForIdentities(
                List.of(new InstrumentIdentity("TECHM", "INE669C01036", "NSE")), TimeFrame.DAY);

        verify(marketDataService).getOHLCForIdentities(argThat(identities ->
                identities.size() == 1 && identities.get(0).isin().isEmpty()), isNull());
    }

    private void setQuoteRecoveryLease() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(org.springframework.data.redis.core.script.RedisScript.class), anyList(), any(), any()))
                .thenReturn(List.of(1));
        ReflectionTestUtils.setField(smartStockService, "stringRedisTemplate", redis);
    }

    private SecurityDocument security(String symbol, String isin) {
        return SecurityDocument.builder()
                .key(SecurityDocument.SecurityKey.builder().symbol(symbol).isin(isin).build())
                .metadata(SecurityDocument.SecurityMetadata.builder().status("ACTIVE").build())
                .build();
    }

    @Test
    void testBackfillPreviousCloseForDay_whenQuoteHasZeroPreviousClose() {
        // Given: A live quote exists in Redis but has previousClose == 0.0
        OHLCQuote liveQuote = OHLCQuote.builder()
                .lastPrice(2500.0)
                .previousClose(0.0)
                .build();

        when(marketDataService.getOHLC(eq(List.of("TCS")), eq(TimeFrame.DAY), eq(false), isNull(), eq(false)))
                .thenReturn(new HashMap<>(Map.of("TCS", liveQuote)));

        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));
        List<OHLCVTPoint> candles = List.of(
                createCandle(today.minusDays(1), 2450.0, 2480.0, 2440.0, 2460.0), // C_N-1
                createCandle(today, 2470.0, 2510.0, 2465.0, 2500.0)  // C_N
        );
        HistoricalData historicalData = HistoricalData.builder()
                .dataPoints(candles)
                .build();

        when(marketDataService.getHistoricalDataBatch(
                eq(List.of("TCS")), any(Date.class), any(Date.class), eq(TimeFrame.DAY),
                eq(false), isNull(), isNull(), eq(false), eq(false), eq(false)))
                .thenReturn(Map.of("TCS", historicalData));

        // When: getSmartQuotes(symbols, TimeFrame.DAY) is called
        Map<String, OHLCQuote> quotes = smartStockService.getSmartQuotes(List.of("TCS"), TimeFrame.DAY);

        // Then:
        OHLCQuote updated = quotes.get("TCS");
        assertEquals(2500.0, updated.getLastPrice(), 0.001);
        assertEquals(2460.0, updated.getPreviousClose(), 0.001); // Successfully backfilled from C_N-1

        // And: cacheLatestPrices is called to update Redis
        verify(marketDataCacheService, atLeastOnce()).cacheLatestPrices(argThat(map ->
                map.containsKey("TCS") && map.get("TCS").getPreviousClose() == 2460.0
        ));
    }

    @Test
    void testEmptyHistoricalData_handlesGracefully() {
        when(marketDataService.getOHLC(eq(List.of("UNKNOWN")), eq(TimeFrame.DAY), eq(false), isNull(), eq(false)))
                .thenReturn(Collections.emptyMap());

        Map<String, OHLCQuote> quotes = smartStockService.getSmartQuotes(List.of("UNKNOWN"));

        assertNotNull(quotes);
        assertTrue(quotes.isEmpty());
        verify(marketDataService, never()).getHistoricalDataBatch(
                anyList(), any(Date.class), any(Date.class), any(), anyBoolean(), any(), any(), anyBoolean(), anyBoolean());
        verify(marketDataCacheService, never()).cacheLatestPrices(anyMap());
    }

    @Test
    void testQualifiedCacheKey_satisfiesBareSymbolWithoutHistoricalFallback() {
        // Redis returns qualified keys, while index constituents are stored as bare tickers.
        OHLCQuote cachedQuote = OHLCQuote.builder()
                .lastPrice(1172.3)
                .previousClose(1182.0)
                .ohlc(OHLCQuote.OHLC.builder().open(1179.0).high(1179.0).low(1160.2).close(1172.3).build())
                .build();
        when(marketDataService.getOHLC(eq(List.of("RELIANCE")), eq(TimeFrame.DAY), eq(false), isNull(), eq(false)))
                .thenReturn(Map.of("NSE:RELIANCE", cachedQuote));

        Map<String, OHLCQuote> quotes = smartStockService.getSmartQuotes(List.of("RELIANCE"));

        assertEquals(cachedQuote, quotes.get("NSE:RELIANCE"));
        verify(marketDataService, never()).getHistoricalDataBatch(
                anyList(), any(Date.class), any(Date.class), any(), anyBoolean(), any(), any(), anyBoolean(), anyBoolean());
    }

    @Test
    void latestPriceCacheSatisfiesMoversBeforeIntradayLookupCanRecoverIt() {
        OHLCQuote latest = OHLCQuote.builder()
                .lastPrice(2156.0)
                .previousClose(2036.0)
                .build();
        when(marketDataCacheService.getLatestPrices(eq(Set.of("TCS"))))
                .thenReturn(Map.of("TCS", latest));
        when(marketDataService.getOHLC(eq(List.of("TCS")), eq(TimeFrame.DAY), eq(false), isNull(), eq(false)))
                .thenReturn(Collections.emptyMap());

        Map<String, OHLCQuote> result = smartStockService.getSmartQuotes(List.of("TCS"));

        assertEquals(2156.0, result.get("TCS").getLastPrice(), 0.001);
        verify(marketDataService, never()).getLivePrices(anyList(), any(), anyBoolean());
        verify(marketDataCacheService).getLatestPrices(eq(Set.of("TCS")));
    }

    // -------------------------------------------------------------------------
    // Noop-sentinel tests (recovery-noop distributed lock fix)
    // -------------------------------------------------------------------------

    /**
     * When the lock-holder's Upstox call returns data but every quote has LTP ≤ 0
     * (market closed / holiday / provider stub), recoveredCount == 0 and a noop
     * sentinel must be written so waiting pods don't burn the full 8s budget.
     */
    @Test
    void lockHolderWritesRecoveryNoopSentinelWhenUpstoxReturnsAllZeroPrices() {
        when(marketDataService.getOHLC(eq(List.of("ZEROSTOCK")), eq(TimeFrame.DAY), eq(false), isNull(), eq(false)))
                .thenReturn(Collections.emptyMap());

        // Upstox returns a stub with LTP = 0 (market closed)
        OHLCQuote closedMarketQuote = OHLCQuote.builder().lastPrice(0.0).previousClose(0.0).build();
        when(marketDataService.getOHLCForIdentities(anyList(), isNull()))
                .thenReturn(Map.of("ZEROSTOCK", closedMarketQuote));

        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        // Lock acquired: index 1 returned (lock-holder path)
        when(redis.execute(any(org.springframework.data.redis.core.script.RedisScript.class), anyList(), any(), any()))
                .thenReturn(List.of(1));
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        ReflectionTestUtils.setField(smartStockService, "stringRedisTemplate", redis);

        Map<String, OHLCQuote> quotes = smartStockService.getSmartQuotes(List.of("ZEROSTOCK"));

        // No valid quotes returned (all LTP ≤ 0)
        assertFalse(quotes.containsKey("ZEROSTOCK"));
        // Noop sentinel must have been written to Redis
        verify(valueOps).set(argThat(key -> key.startsWith("market:movers:recovery-noop:")),
                eq("1"), eq(Duration.ofSeconds(60)));
        // No prices cached (nothing valid to cache)
        verify(marketDataCacheService, never()).cacheLatestPrices(anyMap());
    }

    /**
     * When the lock-holder's Upstox call throws an exception, the noop sentinel
     * must still be written so waiting pods exit immediately rather than waiting
     * the full 8s timeout for data that will never arrive.
     */
    @Test
    void lockHolderWritesRecoveryNoopSentinelWhenUpstoxThrows() {
        when(marketDataService.getOHLC(eq(List.of("ERRSTOCK")), eq(TimeFrame.DAY), eq(false), isNull(), eq(false)))
                .thenReturn(Collections.emptyMap());
        when(marketDataService.getOHLCForIdentities(anyList(), isNull()))
                .thenThrow(new RuntimeException("Upstox timeout"));

        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(org.springframework.data.redis.core.script.RedisScript.class), anyList(), any(), any()))
                .thenReturn(List.of(1));
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        ReflectionTestUtils.setField(smartStockService, "stringRedisTemplate", redis);

        Map<String, OHLCQuote> quotes = smartStockService.getSmartQuotes(List.of("ERRSTOCK"));

        assertTrue(quotes.isEmpty());
        // Even on exception the noop key must be written
        verify(valueOps).set(argThat(key -> key.startsWith("market:movers:recovery-noop:")),
                eq("1"), eq(Duration.ofSeconds(60)));
    }

    /**
     * When a waiter pod detects the noop sentinel (hasKey returns true), it must
     * return immediately with whatever partial quotes it already has instead of
     * waiting the full 8s budget.
     */
    @Test
    void waiterReturnsPartialQuotesImmediatelyWhenNoopSentinelDetected() {
        // Symbol A is already in cache (waiter has it)
        OHLCQuote cachedA = OHLCQuote.builder().lastPrice(100.0).previousClose(95.0).build();
        when(marketDataCacheService.getLatestPrices(anySet()))
                .thenReturn(Collections.emptyMap()); // B is never populated
        when(marketDataService.getOHLC(eq(List.of("A", "B")), eq(TimeFrame.DAY), eq(false), isNull(), eq(false)))
                .thenReturn(Map.of("A", cachedA)); // A is in intraday DB

        // Waiter path: 0 locks acquired (another pod holds them)
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(org.springframework.data.redis.core.script.RedisScript.class), anyList(), any(), any()))
                .thenReturn(Collections.emptyList()); // 0 acquired → waiter path
        // hasKey for the noop sentinel returns true immediately (lock-holder already wrote it)
        when(redis.hasKey(argThat(key -> key != null && key.startsWith("market:movers:recovery-noop:"))))
                .thenReturn(Boolean.TRUE);
        ReflectionTestUtils.setField(smartStockService, "stringRedisTemplate", redis);
        // Set a short wait budget so the test doesn't actually sleep 8s
        ReflectionTestUtils.setField(smartStockService, "refreshLockWaitMs", 8000L);

        long start = System.currentTimeMillis();
        Map<String, OHLCQuote> quotes = smartStockService.getSmartQuotes(List.of("A", "B"));
        long elapsed = System.currentTimeMillis() - start;

        // Waiter must exit well before the 8s budget (sentinel detected after first poll at ~100ms)
        assertTrue(elapsed < 3000, "Waiter should exit early on noop sentinel but took " + elapsed + "ms");
        // A is present (from intraday DB), B is absent (lock-holder found nothing)
        assertEquals(100.0, quotes.get("A").getLastPrice(), 0.001);
        assertFalse(quotes.containsKey("B"));
        // Provider must NOT have been called (waiter never calls Upstox directly)
        verify(marketDataService, never()).getOHLCForIdentities(anyList(), any());
    }
}
