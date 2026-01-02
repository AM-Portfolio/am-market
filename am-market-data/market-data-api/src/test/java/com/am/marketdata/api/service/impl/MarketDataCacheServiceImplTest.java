// package com.am.marketdata.api.service.impl;

// import com.am.marketdata.api.service.InvestmentInstrumentService;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.data.redis.core.RedisTemplate;
// import org.springframework.data.redis.core.ValueOperations;

// import java.util.*;
// import java.util.concurrent.TimeUnit;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
// public class MarketDataCacheServiceImplTest {

//     @Mock
//     private InvestmentInstrumentService investmentInstrumentService;

//     @Mock
//     private RedisTemplate<String, Object> redisTemplate;

//     @Mock
//     private ValueOperations<String, Object> valueOperations;

//     private MarketDataCacheServiceImpl cacheService;

//     @BeforeEach
//     void setUp() {
//         when(redisTemplate.opsForValue()).thenReturn(valueOperations);
//         cacheService = new MarketDataCacheServiceImpl(investmentInstrumentService, redisTemplate);
//         cacheService.setCacheEnabled(true);
//         cacheService.setCacheTtlSeconds(300);
//     }

//     @Test
//     void getQuotes_WhenCacheHit_ReturnsCachedData() {
//         // Arrange
//         List<String> symbols = Arrays.asList("AAPL", "MSFT");
//         String cacheKey = "quotes:AAPL,MSFT";
//         Map<String, Map<String, Object>> cachedQuotes = new HashMap<>();
//         cachedQuotes.put("AAPL", Collections.singletonMap("price", 150.0));
//         cachedQuotes.put("MSFT", Collections.singletonMap("price", 250.0));

//         when(redisTemplate.hasKey(cacheKey)).thenReturn(true);
//         when(valueOperations.get(cacheKey)).thenReturn(cachedQuotes);

//         // Act
//         Map<String, Map<String, Object>> result = cacheService.getQuotes(symbols, false);

//         // Assert
//         assertEquals(cachedQuotes, result);
//         verify(investmentInstrumentService, never()).getQuotes(any());
//         verify(redisTemplate).hasKey(cacheKey);
//         verify(valueOperations).get(cacheKey);
//     }

//     @Test
//     void getQuotes_WhenCacheMiss_FetchesAndCachesData() {
//         // Arrange
//         List<String> symbols = Arrays.asList("AAPL", "MSFT");
//         String cacheKey = "quotes:AAPL,MSFT";
//         Map<String, Map<String, Object>> serviceQuotes = new HashMap<>();
//         serviceQuotes.put("AAPL", Collections.singletonMap("price", 150.0));
//         serviceQuotes.put("MSFT", Collections.singletonMap("price", 250.0));

//         when(redisTemplate.hasKey(cacheKey)).thenReturn(false);
//         when(investmentInstrumentService.getQuotes(symbols)).thenReturn(serviceQuotes);

//         // Act
//         Map<String, Map<String, Object>> result = cacheService.getQuotes(symbols, false);

//         // Assert
//         assertEquals(serviceQuotes, result);
//         verify(investmentInstrumentService).getQuotes(symbols);
//         verify(redisTemplate).hasKey(cacheKey);
//         verify(valueOperations).set(eq(cacheKey), eq(serviceQuotes), eq(300L), eq(TimeUnit.SECONDS));
//     }

//     @Test
//     void getQuotes_WhenForceRefresh_FetchesAndCachesData() {
//         // Arrange
//         List<String> symbols = Arrays.asList("AAPL", "MSFT");
//         String cacheKey = "quotes:AAPL,MSFT";
//         Map<String, Map<String, Object>> serviceQuotes = new HashMap<>();
//         serviceQuotes.put("AAPL", Collections.singletonMap("price", 150.0));
//         serviceQuotes.put("MSFT", Collections.singletonMap("price", 250.0));

//         when(investmentInstrumentService.getQuotes(symbols)).thenReturn(serviceQuotes);

//         // Act
//         Map<String, Map<String, Object>> result = cacheService.getQuotes(symbols, true);

//         // Assert
//         assertEquals(serviceQuotes, result);
//         verify(investmentInstrumentService).getQuotes(symbols);
//         verify(redisTemplate, never()).hasKey(cacheKey);
//         verify(valueOperations).set(eq(cacheKey), eq(serviceQuotes), eq(300L), eq(TimeUnit.SECONDS));
//     }

//     @Test
//     void getHistoricalData_WhenCacheHit_ReturnsCachedData() {
//         // Arrange
//         String symbol = "AAPL";
//         Date from = new Date();
//         Date to = new Date();
//         String interval = "day";
//         String instrumentType = "equity";
//         Map<String, Object> additionalParams = Collections.singletonMap("continuous", true);
//         String cacheKey = "historical:AAPL:day:equity:" + from.getTime() + ":" + to.getTime();
        
//         Map<String, Object> cachedData = new HashMap<>();
//         cachedData.put("data", Arrays.asList(
//                 Map.of("date", "2023-01-01", "close", 150.0),
//                 Map.of("date", "2023-01-02", "close", 152.0)
//         ));

//         when(redisTemplate.hasKey(cacheKey)).thenReturn(true);
//         when(valueOperations.get(cacheKey)).thenReturn(cachedData);

//         // Act
//         Map<String, Object> result = cacheService.getHistoricalData(
//                 symbol, from, to, interval, instrumentType, additionalParams, false);

//         // Assert
//         assertEquals(cachedData, result);
//         verify(investmentInstrumentService, never()).getHistoricalData(any(), any(), any(), any(), any(), any());
//         verify(redisTemplate).hasKey(cacheKey);
//         verify(valueOperations).get(cacheKey);
//     }

//     @Test
//     void getLivePrices_WhenCacheDisabled_BypassesCache() {
//         // Arrange
//         cacheService.setCacheEnabled(false);
//         List<String> symbols = Arrays.asList("AAPL", "MSFT");
//         Map<String, Object> serviceData = new HashMap<>();
//         serviceData.put("AAPL", Map.of("price", 150.0));
//         serviceData.put("MSFT", Map.of("price", 250.0));

//         when(investmentInstrumentService.getLivePrices(symbols)).thenReturn(serviceData);

//         // Act
//         Map<String, Object> result = cacheService.getLivePrices(symbols, false);

//         // Assert
//         assertEquals(serviceData, result);
//         verify(investmentInstrumentService).getLivePrices(symbols);
//         verify(redisTemplate, never()).hasKey(anyString());
//         verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
//     }

//     @Test
//     void clearCache_DeletesAllCacheEntries() {
//         // Arrange
//         Set<String> cacheKeys = new HashSet<>(Arrays.asList(
//                 "quotes:AAPL,MSFT", 
//                 "historical:AAPL:day:equity:123456:234567"
//         ));
//         when(redisTemplate.keys("*")).thenReturn(cacheKeys);

//         // Act
//         cacheService.clearCache();

//         // Assert
//         verify(redisTemplate).keys("*");
//         verify(redisTemplate).delete("quotes:AAPL,MSFT");
//         verify(redisTemplate).delete("historical:AAPL:day:equity:123456:234567");
//     }

//     @Test
//     void getCacheStatistics_ReturnsCorrectStats() {
//         // Arrange
//         Set<String> quoteKeys = new HashSet<>(Arrays.asList("quotes:AAPL", "quotes:MSFT"));
//         Set<String> historicalKeys = new HashSet<>(Arrays.asList("historical:AAPL:day", "historical:MSFT:day"));
//         Set<String> livePriceKeys = new HashSet<>(Collections.singletonList("live-prices:AAPL,MSFT"));
        
//         when(redisTemplate.keys("quotes:*")).thenReturn(quoteKeys);
//         when(redisTemplate.keys("historical:*")).thenReturn(historicalKeys);
//         when(redisTemplate.keys("live-prices:*")).thenReturn(livePriceKeys);
//         when(redisTemplate.keys("option-chain:*")).thenReturn(Collections.emptySet());
//         when(redisTemplate.keys("mutual-fund:*")).thenReturn(Collections.emptySet());
//         when(redisTemplate.keys("mutual-fund-nav:*")).thenReturn(Collections.emptySet());

//         // Set some hit/miss counts
//         cacheService.incrementHitCount();
//         cacheService.incrementHitCount();
//         cacheService.incrementMissCount();

//         // Act
//         Map<String, Object> stats = cacheService.getCacheStatistics();

//         // Assert
//         assertEquals(2L, stats.get("hits"));
//         assertEquals(1L, stats.get("misses"));
//         assertEquals(2.0/3.0, (double)stats.get("hitRatio"), 0.001);
        
//         Map<String, Integer> typeCounts = (Map<String, Integer>)stats.get("keyCountsByType");
//         assertEquals(2, typeCounts.get("quotes"));
//         assertEquals(2, typeCounts.get("historical"));
//         assertEquals(1, typeCounts.get("livePrices"));
//         assertEquals(0, typeCounts.get("optionChain"));
//         assertEquals(0, typeCounts.get("mutualFund"));
//         assertEquals(0, typeCounts.get("mutualFundNav"));
//     }
// }
