// package com.am.marketdata.tradebrain.service.stockdetails;

// import java.util.Collections;
// import java.util.List;

// import org.springframework.stereotype.Service;

// import com.am.marketdata.common.model.events.BoardOfDirector;
// import com.am.common.investment.model.board.BoardOfDirectors;
// import com.am.common.investment.service.BoardOfDirectorsService;
// import com.am.marketdata.external.api.client.TradeBrainClient;
// import com.am.marketdata.external.api.model.ApiResponse;
// import com.am.marketdata.kafka.producer.StockPortfolioProducerService;
// import com.am.marketdata.tradebrain.mapper.MarketBoardOfDirectorsMapper;
// import com.fasterxml.jackson.core.JsonProcessingException;
// import com.fasterxml.jackson.core.type.TypeReference;
// import com.fasterxml.jackson.databind.ObjectMapper;

// import io.micrometer.core.instrument.Counter;
// import io.micrometer.core.instrument.MeterRegistry;
// import io.micrometer.core.instrument.Timer;
// import lombok.extern.slf4j.Slf4j;

// /**
//  * Service for handling board of directors data
//  */
// @Service
// @Slf4j
// public class StockPortfolioService {

//     private final TradeBrainClient tradeBrainClient;
//     private final StockPortfolioProducerService producerService;
//     private final ObjectMapper objectMapper;
//     private final MarketBoardOfDirectorsMapper mapper;
//     private final BoardOfDirectorsService boardOfDirectorsService;
    
//     // Metrics
//     private final Timer fetchTimer;
//     private final Counter successCounter;
//     private final Counter failureCounter;
    
//     /**
//      * Constructor with dependencies
//      * 
//      * @param tradeBrainClient TradeBrain API client
//      * @param producerService Kafka producer service
//      * @param objectMapper JSON object mapper
//      * @param meterRegistry Metrics registry
//      * @param mapper Board of directors mapper
//      */
//     public StockPortfolioService(
//             TradeBrainClient tradeBrainClient,
//             StockPortfolioProducerService producerService,
//             ObjectMapper objectMapper,
//             MeterRegistry meterRegistry,
//             MarketBoardOfDirectorsMapper mapper,
//             BoardOfDirectorsService boardOfDirectorsService) {
//         this.tradeBrainClient = tradeBrainClient;
//         this.producerService = producerService;
//         this.objectMapper = objectMapper;
//         this.mapper = mapper;
//         this.boardOfDirectorsService = boardOfDirectorsService;
        
//         // Initialize metrics
//         this.fetchTimer = Timer.builder("board.directors.fetch.time")
//                 .description("Time taken to fetch board of directors data")
//                 .register(meterRegistry);
//         this.successCounter = Counter.builder("board.directors.success.count")
//                 .description("Number of successful board of directors fetches")
//                 .register(meterRegistry);
//         this.failureCounter = Counter.builder("board.directors.failure.count")
//                 .description("Number of failed board of directors fetches")
//                 .register(meterRegistry);
//     }
    
//     /**
//      * Fetch board of directors data for a symbol, validate it, and publish to Kafka
//      * 
//      * @param symbol Stock symbol
//      * @return List of board of directors
//      */
//     public BoardOfDirectors fetchAndPublishBoardOfDirectors(String symbol) {
//         if (symbol == null || symbol.isEmpty()) {
//             log.error("Invalid symbol provided: {}", symbol);
//             failureCounter.increment();
//             throw new IllegalArgumentException("Symbol cannot be null or empty");
//         }
        
//         log.info("Fetching board of directors data for symbol: {}", symbol);
        
//         return fetchTimer.record(() -> {
//             try {
//                 // Call TradeBrain API
//                 ApiResponse response = tradeBrainClient.getBoardOfDirectors(symbol);
                
//                 if (!response.isSuccess()) {
//                     log.error("Failed to fetch board of directors for {}: {}", 
//                             symbol, response.getErrorMessage());
//                     failureCounter.increment();
//                     return null;
//                 }
                
//                 // Parse response data
//                 List<BoardOfDirector> directors = parseDirectors(response.getData());
                
//                 if (directors.isEmpty()) {
//                     log.warn("No board of directors data found for symbol: {}", symbol);
//                     return null;
//                 }
                
//                 log.info("Successfully fetched {} directors for symbol: {}", 
//                         directors.size(), symbol);
                
//                 // Create BoardOfDirectors object
//                 BoardOfDirectors boardOfDirectors = mapper.toBoardOfDirectors(symbol, directors);
//                 boardOfDirectorsService.saveBoardOfDirectors(boardOfDirectors);
//                 // Publish to Kafka
//                 producerService.sendBoardOfDirectorsUpdate(symbol, boardOfDirectors);
                
//                 successCounter.increment();
//                 return boardOfDirectors;
                
//             } catch (Exception e) {
//                 log.error("Error fetching board of directors for symbol {}: {}", 
//                         symbol, e.getMessage(), e);
//                 failureCounter.increment();
//                 return null;
//             }
//         });
//     }
    
//     /**
//      * Parse directors from JSON string
//      * 
//      * @param jsonData JSON string containing directors data
//      * @return List of parsed BoardOfDirector objects
//      */
//     private List<BoardOfDirector> parseDirectors(String jsonData) {
//         try {
//             return objectMapper.readValue(jsonData, new TypeReference<List<BoardOfDirector>>() {});
//         } catch (JsonProcessingException e) {
//             log.error("Error parsing board of directors data: {}", e.getMessage(), e);
//             return Collections.emptyList();
//         }
//     }
// }
