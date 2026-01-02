// package com.am.common.investment.app.service;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertNotNull;

// import com.am.common.investment.app.AmCommonInvestmentApplication;
// import com.am.common.investment.app.config.MongoTestConfig;
// import com.am.common.investment.app.config.TestContainersConfig;
// import com.am.common.investment.model.events.StockInsidicesEventData;
// import com.am.common.investment.model.events.mapper.StockIndicesEventDataMapper;
// import com.am.common.investment.model.stockindice.StockIndicesMarketData;
// import com.am.common.investment.service.StockIndicesMarketDataService;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.test.context.ActiveProfiles;
// import org.testcontainers.junit.jupiter.Testcontainers;

// import java.io.IOException;
// import java.nio.file.Files;
// import java.nio.file.Paths;
// import java.util.List;
// import java.util.Set;

// @SpringBootTest(classes = {
//     AmCommonInvestmentApplication.class,
//     MongoTestConfig.class,
//     TestContainersConfig.class
// })
// @ActiveProfiles("test")
// @Testcontainers
// public class StockIndicesMarketDataServiceIntegrationTest {

//     @Autowired
//     private StockIndicesMarketDataService marketDataService;

//     private StockInsidicesEventData eventData;

//     @BeforeEach
//     void setup() throws IOException {
//         // Load test data from JSON file
//         String json = new String(Files.readAllBytes(Paths.get("src/test/resources/stockindices.json")));
//         ObjectMapper objectMapper = new ObjectMapper();
//         eventData = objectMapper.readValue(json, StockInsidicesEventData.class);
//     }

//     @Test
//     void shouldSaveAndRetrieveStockIndicesMarketData() {
//         // Given
//         StockIndicesMarketData marketData = StockIndicesEventDataMapper.toMarketData(eventData);

//         // When
//         StockIndicesMarketData savedData = marketDataService.save(marketData);

//         // Then
//         assertNotNull(savedData);
//         assertEquals("NIFTY 50", savedData.getIndexSymbol());

//         // Verify retrieval by index symbol
//         StockIndicesMarketData retrievedData = marketDataService.findByIndexSymbol("NIFTY 50");
//         assertNotNull(retrievedData);
//         assertEquals(savedData.getIndexSymbol(), retrievedData.getIndexSymbol());
//         assertEquals(savedData.getMetadata().getLast(), retrievedData.getMetadata().getLast(), 0.01);
//     }

//     @Test
//     void shouldRetrieveMultipleIndices() {
//         // Given
//         StockIndicesMarketData marketData = StockIndicesEventDataMapper.toMarketData(eventData);
//         marketDataService.save(marketData);
//         marketDataService.save(marketData);
//         marketDataService.save(marketData);

//         // When
//         List<StockIndicesMarketData> results = marketDataService.findByIndexSymbols(Set.of("NIFTY 50"));

//         // Then
//         assertNotNull(results);
//         assertEquals(1, results.size());
//         StockIndicesMarketData result = results.get(0);
//         assertEquals("NIFTY 50", result.getIndexSymbol());
//         assertEquals(23658.35, result.getMetadata().getLast(), 0.01);
//     }

//     @Test
//     void shouldHandleAuditData() {
//         // Given
//         StockIndicesMarketData marketData = StockIndicesEventDataMapper.toMarketData(eventData);

//         // When
//         StockIndicesMarketData savedData = marketDataService.save(marketData);

//         // Then
//         assertNotNull(savedData.getAudit());
//         assertNotNull(savedData.getAudit().getCreatedAt());
//         assertNotNull(savedData.getAudit().getUpdatedAt());
//     }
// }
