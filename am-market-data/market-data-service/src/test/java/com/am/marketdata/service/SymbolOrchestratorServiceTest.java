package com.am.marketdata.service;

import com.am.common.investment.service.StockIndicesMarketDataService;
import com.am.common.investment.model.stockindice.StockData;
import com.am.common.investment.model.stockindice.StockIndicesMarketData;
import com.am.marketdata.service.client.ParserApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SymbolOrchestratorServiceTest {

    @Mock
    private ParserApiClient parserApiClient;
    @Mock
    private StockIndicesMarketDataService stockIndicesMarketDataService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private SetOperations<String, String> setOperations;

    private SymbolOrchestratorService service;

    @BeforeEach
    void setUp() {
        service = new SymbolOrchestratorService(parserApiClient, stockIndicesMarketDataService, stringRedisTemplate);
        service.setDefaultSymbols("RELIANCE");
        service.setCacheTtlSeconds(900);
        service.setActiveSetRedisKey("market:active-symbols");
        lenient().when(parserApiClient.getAllEtfSymbols()).thenReturn(List.of("NIFTYBEES"));
    }

    @Test
    void findDistinctSymbols_includesRedisActiveSet() {
        service.setIncludePortfolioActiveSet(true);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("market:active-symbols"))
                .thenReturn(Set.of("GROWWDEFNC", "BANKBARODA"));

        Set<String> symbols = service.findDistinctSymbols();

        assertTrue(symbols.contains("RELIANCE"));
        assertTrue(symbols.contains("NIFTYBEES"));
        assertTrue(symbols.contains("GROWWDEFNC"));
        assertTrue(symbols.contains("BANKBARODA"));
    }

    @Test
    void findDistinctSymbols_redisDown_failOpenKeepsBase() {
        service.setIncludePortfolioActiveSet(true);
        when(stringRedisTemplate.opsForSet()).thenThrow(new RuntimeException("redis down"));

        Set<String> symbols = service.findDistinctSymbols();

        assertTrue(symbols.contains("RELIANCE"));
        assertTrue(symbols.contains("NIFTYBEES"));
        assertFalse(symbols.contains("GROWWDEFNC"));
    }

    @Test
    void findDistinctSymbols_flagOff_ignoresRedis() {
        service.setIncludePortfolioActiveSet(false);

        Set<String> symbols = service.findDistinctSymbols();

        assertTrue(symbols.contains("RELIANCE"));
        assertFalse(symbols.contains("GROWWDEFNC"));
    }

    @Test
    void configuredIndexConstituentCountsAreLoadedInOneBatchAndDeduplicated() {
        when(stockIndicesMarketDataService.findByIndexSymbols(anySet())).thenReturn(List.of(
                StockIndicesMarketData.builder()
                        .indexSymbol("NIFTY 500")
                        .data(java.util.Arrays.asList(
                                StockData.builder().symbol("reliance").build(),
                                StockData.builder().symbol("RELIANCE").build(),
                                StockData.builder().symbol("TCS").build(),
                                StockData.builder().symbol(" ").build(),
                                null))
                        .build()));

        Map<String, Integer> counts = service.getIndexConstituentCounts(Set.of("NIFTY 500", "NIFTY 50"));

        assertTrue(counts.containsKey("NIFTY 500"));
        assertEquals(2, counts.get("NIFTY 500"));
        assertTrue(counts.containsKey("NIFTY 50"));
        assertEquals(0, counts.get("NIFTY 50"));
        org.mockito.Mockito.verify(stockIndicesMarketDataService, org.mockito.Mockito.times(1))
                .findByIndexSymbols(anySet());
    }

    @Test
    void findDistinctSymbols_mergesNewRedisMembersWhileBaseCacheValid() {
        service.setIncludePortfolioActiveSet(true);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("market:active-symbols"))
                .thenReturn(Set.of("GROWWDEFNC"))
                .thenReturn(Set.of("GROWWDEFNC", "NEWETF"));

        Set<String> first = service.findDistinctSymbols();
        assertTrue(first.contains("GROWWDEFNC"));

        Set<String> second = service.findDistinctSymbols();
        assertTrue(second.contains("GROWWDEFNC"));
        assertTrue(second.contains("NEWETF"));
    }
}
