// package com.am.marketdata.service.scheduler;

// import com.am.marketdata.service.MarketDataService;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Component;

// import java.time.LocalDateTime;
// import java.time.ZoneId;
// import java.time.format.DateTimeFormatter;
// import java.util.concurrent.CompletableFuture;

// /**
//  * Scheduler for market data operations
//  * Handles periodic fetching and processing of market data
//  */
// @Slf4j
// @Component
// public class MarketDataScheduler {

//     private final MarketDataService marketDataService;
    
//     @Value("${market.data.processing.enabled:true}")
//     private boolean processingEnabled;
    
//     @Value("${market.timezone:Asia/Kolkata}")
//     private String marketTimeZone;
    
//     @Value("${market.hours.start:09:15}")
//     private String marketOpenTime;
    
//     @Value("${market.hours.end:15:30}")
//     private String marketCloseTime;

//     @Autowired
//     public MarketDataScheduler(MarketDataService marketDataService) {
//         this.marketDataService = marketDataService;
//     }

//     /**
//      * Scheduled task to fetch and process market data
//      * Default: Every 5 minutes during market hours
//      */
//     @Scheduled(cron = "${market.data.processing.cron:0 */5 * * * *}")
//     public void processMarketData() {
//         if (!processingEnabled) {
//             log.debug("Market data processing is disabled");
//             return;
//         }
        
//         LocalDateTime now = LocalDateTime.now(ZoneId.of(marketTimeZone));
//         String timestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
//         log.info("Starting scheduled market data processing at {}", timestamp);
        
//         if (!isMarketOpen()) {
//             log.info("Market is closed. Skipping scheduled processing.");
//             return;
//         }
        
//         try {
//             // Get all instruments asynchronously
//             CompletableFuture<Void> instrumentsFuture = CompletableFuture.runAsync(() -> {
//                 try {
//                     log.info("Fetching all instruments");
//                     marketDataService.getAllInstruments();
//                     log.info("Successfully fetched all instruments");
//                 } catch (Exception e) {
//                     log.error("Error fetching all instruments", e);
//                 }
//             });
            
//             // Process different exchanges in parallel
//             CompletableFuture<Void> nseFuture = CompletableFuture.runAsync(() -> {
//                 try {
//                     log.info("Fetching NSE instruments");
//                     marketDataService.getInstrumentsForExchange("NSE");
//                     log.info("Successfully fetched NSE instruments");
//                 } catch (Exception e) {
//                     log.error("Error fetching NSE instruments", e);
//                 }
//             });
            
//             CompletableFuture<Void> bseFuture = CompletableFuture.runAsync(() -> {
//                 try {
//                     log.info("Fetching BSE instruments");
//                     marketDataService.getInstrumentsForExchange("BSE");
//                     log.info("Successfully fetched BSE instruments");
//                 } catch (Exception e) {
//                     log.error("Error fetching BSE instruments", e);
//                 }
//             });
            
//             // Wait for all operations to complete
//             CompletableFuture.allOf(instrumentsFuture, nseFuture, bseFuture).join();
            
//             log.info("Scheduled market data processing completed successfully at {}", 
//                     LocalDateTime.now(ZoneId.of(marketTimeZone))
//                             .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
//         } catch (Exception e) {
//             log.error("Error during scheduled market data processing", e);
//         }
//     }
    
//     /**
//      * Check if the market is currently open
//      * @return true if market is open, false otherwise
//      */
//     private boolean isMarketOpen() {
//         try {
//             ZoneId zoneId = ZoneId.of(marketTimeZone);
//             LocalDateTime now = LocalDateTime.now(zoneId);
            
//             // Check if it's a weekend (Saturday or Sunday)
//             int dayOfWeek = now.getDayOfWeek().getValue();
//             if (dayOfWeek > 5) {
//                 return false;
//             }
            
//             // Parse market hours
//             DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
//             LocalDateTime openTime = LocalDateTime.of(
//                     now.toLocalDate(),
//                     LocalDateTime.parse(marketOpenTime, timeFormatter).toLocalTime()
//             );
            
//             LocalDateTime closeTime = LocalDateTime.of(
//                     now.toLocalDate(),
//                     LocalDateTime.parse(marketCloseTime, timeFormatter).toLocalTime()
//             );
            
//             // Check if current time is between market open and close times
//             return now.isAfter(openTime) && now.isBefore(closeTime);
//         } catch (Exception e) {
//             log.error("Error checking market hours", e);
//             return false;
//         }
//     }
// }
