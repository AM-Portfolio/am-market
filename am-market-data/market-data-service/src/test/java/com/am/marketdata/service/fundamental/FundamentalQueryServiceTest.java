package com.am.marketdata.service.fundamental;

import com.am.marketdata.common.provider.FundamentalDataProvider;
import com.am.marketdata.provider.common.FundamentalDataProviderFactory;
import com.am.marketdata.service.repo.FundamentalDataRepository;
import com.am.marketdata.service.repo.SecurityRepository;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.FindIterable;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FundamentalQueryServiceTest {

    @Mock
    private FundamentalDataRepository fundamentalDataRepository;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private FundamentalDataProviderFactory providerFactory;

    @Mock
    private FundamentalCalculationEngine calculationEngine;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private MongoCollection<Document> securitiesCollection;

    @Mock
    private MongoCollection<Document> indicesCollection;

    @Mock
    private MongoCollection<Document> fundamentalCollection;

    @Mock
    private FindIterable<Document> findIterableSecurities;

    @Mock
    private FindIterable<Document> findIterableIndices;

    @Mock
    private FindIterable<Document> findIterableFundamental;

    private FundamentalQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new FundamentalQueryService(
                fundamentalDataRepository,
                securityRepository,
                providerFactory,
                calculationEngine,
                mongoTemplate
        );
    }

    @Test
    @DisplayName("Should resolve direct 12-character ISIN")
    void testDirectIsin() {
        String isin = queryService.resolveIsin("INE040A01034");
        assertEquals("INE040A01034", isin);
    }

    @Test
    @DisplayName("Should resolve exchange-prefixed ISIN")
    void testExchangePrefixedIsin() {
        String isin = queryService.resolveIsin("NSE_EQ|INE040A01034");
        assertEquals("INE040A01034", isin);
    }

    @Test
    @DisplayName("Should resolve space-separated 'HDFC BANK' by compacting to 'HDFCBANK'")
    void testSpaceSeparatedHdfcBank() {
        when(mongoTemplate.getCollection("securities")).thenReturn(securitiesCollection);
        when(mongoTemplate.getCollection("stock_indices_market_data")).thenReturn(indicesCollection);
        when(mongoTemplate.getCollection("fundamental_analysis")).thenReturn(fundamentalCollection);

        // When queried with "HDFCBANK", return matching doc
        when(securitiesCollection.find(any(Document.class))).thenAnswer(invocation -> {
            Document query = invocation.getArgument(0);
            Pattern pattern = (Pattern) query.get("key.symbol");
            if (pattern != null && pattern.pattern().contains("HDFCBANK")) {
                Document key = new Document("symbol", "HDFCBANK").append("isin", "INE040A01034");
                Document found = new Document("key", key);
                FindIterable<Document> mockFind = mock(FindIterable.class);
                when(mockFind.first()).thenReturn(found);
                return mockFind;
            }
            FindIterable<Document> mockEmpty = mock(FindIterable.class);
            when(mockEmpty.first()).thenReturn(null);
            return mockEmpty;
        });

        when(indicesCollection.find(any(Document.class))).thenReturn(findIterableIndices);
        when(findIterableIndices.first()).thenReturn(null);
        when(fundamentalCollection.find(any(Document.class))).thenReturn(findIterableFundamental);
        when(findIterableFundamental.first()).thenReturn(null);

        String isin = queryService.resolveIsin("HDFC BANK");
        assertEquals("INE040A01034", isin);
    }

    @Test
    @DisplayName("Should resolve hyphenated 'BAJAJ-AUTO' when input has space 'BAJAJ AUTO'")
    void testBajajAutoWithSpace() {
        when(mongoTemplate.getCollection("securities")).thenReturn(securitiesCollection);
        when(mongoTemplate.getCollection("stock_indices_market_data")).thenReturn(indicesCollection);
        when(mongoTemplate.getCollection("fundamental_analysis")).thenReturn(fundamentalCollection);

        when(securitiesCollection.find(any(Document.class))).thenAnswer(invocation -> {
            Document query = invocation.getArgument(0);
            Pattern pattern = (Pattern) query.get("key.symbol");
            if (pattern != null && pattern.pattern().contains("BAJAJ-AUTO")) {
                Document key = new Document("symbol", "BAJAJ-AUTO").append("isin", "INE917I01010");
                Document found = new Document("key", key);
                FindIterable<Document> mockFind = mock(FindIterable.class);
                when(mockFind.first()).thenReturn(found);
                return mockFind;
            }
            FindIterable<Document> mockEmpty = mock(FindIterable.class);
            when(mockEmpty.first()).thenReturn(null);
            return mockEmpty;
        });

        when(indicesCollection.find(any(Document.class))).thenReturn(findIterableIndices);
        when(findIterableIndices.first()).thenReturn(null);
        when(fundamentalCollection.find(any(Document.class))).thenReturn(findIterableFundamental);
        when(findIterableFundamental.first()).thenReturn(null);

        String isin = queryService.resolveIsin("BAJAJ AUTO");
        assertEquals("INE917I01010", isin);
    }
}
