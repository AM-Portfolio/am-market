// package com.am.marketdata.service;

// import jakarta.annotation.PostConstruct;
// import jakarta.transaction.Transactional;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.slf4j.MDC;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Service;
// import java.time.DayOfWeek;
// import java.time.LocalDateTime;
// import java.time.LocalTime;
// import java.time.ZoneId;
// import java.util.List;

// @Slf4j
// @Service
// @RequiredArgsConstructor
// @ConditionalOnProperty(value="stock-data-scheduler", havingValue ="true", matchIfMissing = true)
// public class StockDataSchedulerService {
//     private final ISINService isinService;
//     private final EquityPriceProcessingService equityPriceProcessingService;

//     @Value("${app.scheduler.trading.start-time}")
//     private String startTime;

//     @Value("${app.scheduler.trading.end-time}")
//     private String endTime;

//     @Value("${app.scheduler.trading.timezone}")
//     private String timeZone;

//     @PostConstruct
//     public void initialize() {
//         log.info("Initializing StockDataSchedulerService");
//         if (isWithinTradingHours()) {
//             fetchAndPersistStockData();
//         }
//     }
    
//     @Scheduled(cron = "${app.scheduler.market-data.stock.fetch}")  // Runs at 5 seconds past every 2 minutes
//     @Transactional
//     public void fetchAndPersistStockData() {
//         MDC.put("scheduler", "stock-data");
//         MDC.put("execution_time", LocalDateTime.now().toString());
        
//         try {
//             if (!isWithinTradingHours()) {
//                 log.info("Outside trading hours (9:15 AM - 3:35 PM IST), skipping stock data fetch");
//                 return;
//             }

//             List<String> isins = isinService.findDistinctIsins();
//             log.info("Starting stock data fetch for {} ISINs at {}", isins.size(), LocalDateTime.now());
            
//             boolean success = equityPriceProcessingService.processEquityPrices(isins);
//             log.info("=== Completed scheduled stock data fetch and persist job. Success: {} ===", success);
//             log.info("Completed stock data fetch for {} ISINs", isins.size());
//         } catch (Exception e) {
//             log.error("Error in stock data scheduler: {}", e.getMessage(), e);
//         } finally {
//             MDC.remove("scheduler");
//             MDC.remove("execution_time");
//         }
//     }

//     private boolean isWithinTradingHours() {
//         LocalDateTime now = LocalDateTime.now(ZoneId.of(timeZone));
//         DayOfWeek day = now.getDayOfWeek();
        
//         // Only run on weekdays (Monday to Friday)
//         if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
//             return false;
//         }

//         LocalTime time = now.toLocalTime();
//         LocalTime marketOpen = LocalTime.parse(startTime);  // 9:15 AM IST
//         LocalTime marketClose = LocalTime.parse(endTime); // 3:35 PM IST

//         return !time.isBefore(marketOpen) && !time.isAfter(marketClose);
//     }
// }