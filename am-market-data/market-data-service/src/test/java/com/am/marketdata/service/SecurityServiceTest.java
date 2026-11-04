package com.am.marketdata.service;

import com.am.marketdata.common.dto.BatchSearchRequest;
import com.am.marketdata.common.dto.BatchSearchResponse;
import com.am.marketdata.common.model.UpstoxInstrument;
import com.am.marketdata.config.BatchSearchProperties;
import com.am.marketdata.provider.upstox.repo.UpstoxInstrumentRepository;
import com.am.marketdata.service.model.security.SecurityDocument;
import com.am.marketdata.service.repo.SecurityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private com.am.common.investment.service.StockIndicesMarketDataService stockIndicesMarketDataService;

    @Mock
    private BatchSearchProperties batchSearchProperties;

    @Mock
    private UpstoxInstrumentRepository upstoxInstrumentRepository;

    private SecurityService securityService;

    @BeforeEach
    void setUp() {
        // Setup RedisTemplate to return our mocked ValueOperations
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        securityService = new SecurityService(
                securityRepository,
                mongoTemplate,
                redisTemplate,
                stockIndicesMarketDataService,
                batchSearchProperties,
                upstoxInstrumentRepository
        );
    }

    @Test
    void testFindBySymbols_WithSymbolAndIsin_CacheMiss_FetchesFromDbAndDualCaches() {
        // GIVEN: We request one ticker (BANKBARODA) and one ISIN (INE028A01039)
        List<String> requestedSymbols = List.of("BANKBARODA", "INE028A01039");

        // Mock Redis cache miss (multiGet returns list of nulls)
        when(valueOperations.multiGet(any())).thenReturn(java.util.Arrays.asList(null, null));

        // Create mock DB documents returned for each candidate
        SecurityDocument barodaDoc = new SecurityDocument();
        SecurityDocument.SecurityKey barodaKey = new SecurityDocument.SecurityKey("BANKBARODA", "INE028A01039");
        barodaDoc.setKey(barodaKey);

        // When DB is queried by symbol, return the baroda doc
        when(securityRepository.findBySymbolIn(List.of("BANKBARODA"))).thenReturn(List.of(barodaDoc));
        // When DB is queried by ISIN, return the baroda doc
        when(securityRepository.findByIsinIn(List.of("INE028A01039"))).thenReturn(List.of(barodaDoc));

        // WHEN: We call the service to fetch the securities
        List<SecurityDocument> results = securityService.findBySymbols(requestedSymbols);

        // THEN: We verify both documents were fetched successfully
        assertNotNull(results);
        assertEquals(2, results.size());

        // Verify that the Redis cache update is invoked with BOTH the symbol and the ISIN keys
        verify(valueOperations, times(1)).multiSet(argThat(map -> {
            boolean hasSymbolKey = map.containsKey("security:metadata:BANKBARODA");
            boolean hasIsinKey = map.containsKey("security:metadata:INE028A01039");
            return hasSymbolKey && hasIsinKey;
        }));
    }

    @Test
    void batchSearch_isinMissOnSecurities_fallsBackToUpstockInstruments_includingInf() {
        lenient().when(batchSearchProperties.getMaxQueries()).thenReturn(100);
        lenient().when(batchSearchProperties.getInternalBatchSize()).thenReturn(50);
        lenient().when(batchSearchProperties.isCacheEnabled()).thenReturn(false);
        lenient().when(batchSearchProperties.getMaxCandidatesPerQuery()).thenReturn(10);
        lenient().when(batchSearchProperties.getMongoQueryLimit()).thenReturn(50);

        // securities empty for both ISINs
        when(mongoTemplate.find(any(Query.class), eq(SecurityDocument.class))).thenReturn(Collections.emptyList());
        when(securityRepository.findByIsin(any())).thenReturn(null);

        UpstoxInstrument pwl = new UpstoxInstrument();
        pwl.setIsin("INE0NNS01011");
        pwl.setTradingSymbol("PWL");
        pwl.setExchange("NSE");
        pwl.setInstrumentType("EQ");
        pwl.setName("PhysicsWallah");

        UpstoxInstrument tataGold = new UpstoxInstrument();
        tataGold.setIsin("INF666M01IO8");
        tataGold.setTradingSymbol("TATAGOLD");
        tataGold.setExchange("NSE");
        tataGold.setInstrumentType("EQ");
        tataGold.setName("Tata Gold ETF");

        when(upstoxInstrumentRepository.findByIsinIn(List.of("INE0NNS01011"))).thenReturn(List.of(pwl));
        when(upstoxInstrumentRepository.findByIsinIn(List.of("INF666M01IO8"))).thenReturn(List.of(tataGold));

        BatchSearchRequest request = BatchSearchRequest.builder()
                .queries(List.of("INE0NNS01011", "INF666M01IO8"))
                .limit(1)
                .minMatchScore(0.0)
                .build();

        BatchSearchResponse response = securityService.batchSearch(request);

        assertEquals(2, response.getTotalMatches());
        assertEquals("PWL", response.getResults().get(0).getMatches().get(0).getSymbol());
        assertEquals("ISIN", response.getResults().get(0).getMatches().get(0).getMatchedField());
        assertEquals("TATAGOLD", response.getResults().get(1).getMatches().get(0).getSymbol());
        assertEquals(1.0, response.getResults().get(1).getMatches().get(0).getMatchScore());
    }
}
