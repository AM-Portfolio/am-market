// package com.am.common.investment.app.service;

// import static com.am.common.investment.app.constant.AppConstants.InfluxDB.*;
// import static com.am.common.investment.app.constant.AppConstants.TestData.*;
// import static com.am.common.investment.app.constant.AppConstants.Query.*;
// import static com.am.common.investment.app.util.TestAssertionUtil.*;

// import com.am.common.investment.app.InvestmentDataApplication;
// import com.am.common.investment.app.config.MongoTestConfig;
// import com.am.common.investment.app.config.TestContainersConfig;
// import com.am.common.investment.app.util.TestDataUtil;
// import com.am.common.investment.model.equity.EquityPrice;
// import com.am.common.investment.persistence.influx.measurement.EquityPriceMeasurement;
// import com.am.common.investment.service.EquityService;
// import com.influxdb.client.InfluxDBClient;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.test.context.ActiveProfiles;
// import org.testcontainers.junit.jupiter.Testcontainers;

// import java.time.Instant;
// import java.time.OffsetDateTime;
// import java.time.ZoneOffset;
// import java.util.List;
// import java.util.Optional;

// @SpringBootTest(classes = {
//     InvestmentDataApplication.class,
//     MongoTestConfig.class,
//     TestContainersConfig.class
// })
// @ActiveProfiles("test")
// @Testcontainers
// @ExtendWith(TestContainersConfig.class)
// public class EquityServiceIntegrationTest {

//     @Autowired
//     private EquityService equityService;

//     @Autowired
//     private InfluxDBClient influxDBClient;

//     @BeforeEach
//     void cleanup() {
//         System.out.println("Running cleanup before test...");
//         // Delete all data in the bucket
//         influxDBClient.getDeleteApi().delete(
//             OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
//             OffsetDateTime.of(2050, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
//             String.format("_measurement=\"%s\"", MEASUREMENT_EQUITY_PRICE),
//             BUCKET,
//             ORG
//         );
        
//         // Add a small delay to ensure delete is processed
//         try {
//             Thread.sleep(1000);
//         } catch (InterruptedException e) {
//             Thread.currentThread().interrupt();
//         }
//     }

//     @Test
//     void shouldSaveAndRetrieveEquityPrice() throws InterruptedException {
//         // Given
//         double open = 149.0;
//         double high = 151.0;
//         double low = 148.0;
//         double close = 150.0;
//         long volume = 1000000L;
//         Instant timestamp = Instant.now();

//         EquityPrice equityPrice = TestDataUtil.createEquityPrice(
//             SYMBOL_AAPL, ISIN_AAPL, open, high, low, close, volume, 
//             EXCHANGE_NASDAQ, CURRENCY_USD, timestamp
//         );

//         // When
//         equityService.savePrice(equityPrice);
        
//         // Add a small delay to ensure InfluxDB processes the write
//         Thread.sleep(1000);

//         // Verify directly with InfluxDB client
//         String query = String.format(EQUITY_PRICE_QUERY_TEMPLATE,
//             BUCKET, 1, "h", MEASUREMENT_EQUITY_PRICE, SYMBOL_AAPL);
            
//         List<EquityPriceMeasurement> directResults = influxDBClient.getQueryApi().query(query, EquityPriceMeasurement.class);
        
//         if (!directResults.isEmpty()) {
//             EquityPriceMeasurement measurement = directResults.get(0);
//             measurement.setSymbol(SYMBOL_AAPL);
//             measurement.setIsin(ISIN_AAPL);
//             measurement.setCurrency(CURRENCY_USD);
//         }
        
//         System.out.println("Direct InfluxDB query results: " + directResults);
        
//          Optional<EquityPrice> retrievedPrice = equityService.getLatestPriceByKey(ISIN_AAPL);
//         // Then
//         assertEquityPrice(retrievedPrice, equityPrice);

//         Optional<EquityPrice> retrievedPrice2 = equityService.getLatestPriceByKey(SYMBOL_AAPL);
//         // Then
//         assertEquityPrice(retrievedPrice2, equityPrice);
//     }

//     @Test
//     void shouldRetrievePriceHistoryByKey() throws InterruptedException {
//         // Given
//         Instant now = Instant.now();
//         Instant startTime = now.minusSeconds(3600); // 1 hour ago
//         Instant endTime = now.plusSeconds(3600);    // 1 hour from now

//         EquityPrice price1 = TestDataUtil.createEquityPrice(
//             SYMBOL_AAPL, ISIN_AAPL, 149.0, 151.0, 148.0, 150.0, 1000000L,
//             EXCHANGE_NASDAQ, CURRENCY_USD, now.minusSeconds(1800) // 30 mins ago
//         );

//         EquityPrice price2 = TestDataUtil.createEquityPrice(
//             SYMBOL_AAPL, ISIN_AAPL, 150.0, 152.0, 149.0, 151.0, 1100000L,
//             EXCHANGE_NASDAQ, CURRENCY_USD, now
//         );

//         equityService.saveAllPrices(List.of(price1, price2));
//         // When
//         // equityService.savePrice(price1);
//         // equityService.savePrice(price2);
        
//         // Add a small delay to ensure InfluxDB processes the writes
//         Thread.sleep(1000);

//         // Then - Test by Symbol
//         List<EquityPrice> historyBySymbol = equityService.getPriceHistoryByKey(SYMBOL_AAPL, startTime, endTime);
//         assertEquityPriceList(historyBySymbol, List.of(price1, price2));

//         // Then - Test by ISIN
//         List<EquityPrice> historyByIsin = equityService.getPriceHistoryByKey(ISIN_AAPL, startTime, endTime);
//         assertEquityPriceList(historyByIsin, List.of(price1, price2));
//     }

//     @Test
//     void shouldRetrievePricesByExchange() throws InterruptedException {
//         // Given
//         Instant now = Instant.now();
        
//         EquityPrice applePrice = TestDataUtil.createEquityPrice(
//             SYMBOL_AAPL, ISIN_AAPL, 149.0, 151.0, 148.0, 150.0, 1000000L,
//             EXCHANGE_NASDAQ, CURRENCY_USD, now
//         );

//         EquityPrice microsoftPrice = TestDataUtil.createEquityPrice(
//             "MSFT", "US5949181045", 300.0, 305.0, 299.0, 302.0, 2000000L,
//             EXCHANGE_NASDAQ, CURRENCY_USD, now
//         );

//         EquityPrice teslaPrice = TestDataUtil.createEquityPrice(
//             "TSLA", "US88160R1014", 240.0, 245.0, 238.0, 242.0, 1500000L,
//             EXCHANGE_NASDAQ, CURRENCY_USD, now
//         );

//         // When
//         // equityService.savePrice(applePrice);
//         // equityService.savePrice(microsoftPrice);
//         // equityService.savePrice(teslaPrice);

//         equityService.saveAllPrices(List.of(applePrice, microsoftPrice, teslaPrice));
        
//         // Add a small delay to ensure InfluxDB processes the writes
//         Thread.sleep(1000);

//         // Then
//         List<EquityPrice> nasdaqPrices = equityService.getPricesByExchange(EXCHANGE_NASDAQ);
//         assertEquityPriceList(nasdaqPrices, List.of(applePrice, microsoftPrice, teslaPrice));
//     }
// }
