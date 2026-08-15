package com.am.marketdata.service;

import com.am.common.investment.service.StockIndicesMarketDataService;
import com.am.marketdata.service.client.ParserApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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
        when(parserApiClient.getAllEtfSymbols()).thenReturn(List.of("NIFTYBEES"));
        when(stockIndicesMarketDataService.findByIndexSymbol(anyString())).thenReturn(null);
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
