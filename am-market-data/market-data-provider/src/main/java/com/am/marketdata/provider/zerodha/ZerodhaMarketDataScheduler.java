// package com.am.marketdata.provider.zerodha;

// import com.zerodhatech.models.Quote;
// import com.zerodhatech.models.OHLCQuote;
// import com.zerodhatech.models.Instrument;
// import com.zerodhatech.models.HistoricalData;
// import com.zerodhatech.ticker.KiteTicker;
// import com.zerodhatech.ticker.OnTicks;

// import com.am.marketdata.provider.zerodha.event.ZerodhaQuoteEvent;
// import com.am.marketdata.provider.zerodha.event.ZerodhaOHLCEvent;
// import com.am.marketdata.provider.zerodha.event.ZerodhaTickEvent;

// import io.micrometer.core.instrument.MeterRegistry;
// import io.micrometer.core.instrument.Timer;

// import lombok.extern.slf4j.Slf4j;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.ApplicationEventPublisher;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Service;

// import jakarta.annotation.PostConstruct;
// import jakarta.annotation.PreDestroy;
// import java.time.LocalDateTime;
// import java.time.LocalTime;
// import java.time.ZoneId;
// import java.util.*;
// import java.util.concurrent.CompletableFuture;

// /**
//  * Scheduler service for Zerodha market data processing
//  * Handles scheduled fetching and processing of market data from Zerodha
//  */
// @Slf4j
// @Service
// public class ZerodhaMarketDataScheduler {

//     private final ZerodhaApiService zerodhaApiService;
//     private final MeterRegistry meterRegistry;
//     private final ApplicationEventPublisher eventPublisher;
    
//     @Value("${market-data.zerodha.market.hours.start:09:15}")
//     private String marketHoursStart;
    
//     @Value("${market-data.zerodha.market.hours.end:15:35}")
//     private String marketHoursEnd;
    
//     @Value("${market-data.zerodha.market.timezone:Asia/Kolkata}")
//     private String marketTimezone;
    
//     @Value("${market-data.zerodha.market.instruments.nse:NSE:NIFTY 50,NSE:BANKNIFTY,NSE:FINNIFTY}")
//     private String nseInstruments;
    
//     @Value("${market-data.zerodha.market.instruments.bse:BSE:SENSEX,BSE:BANKEX}")
//     private String bseInstruments;
    
//     @Value("${market-data.zerodha.market.instruments.indices:NSE:NIFTY 50,NSE:BANKNIFTY,NSE:FINNIFTY,BSE:SENSEX}")
//     private String indexInstruments;
    
//     @Value("${market-data.zerodha.market.data.enabled:true}")
//     private boolean marketDataEnabled;
    
//     @Value("${market-data.zerodha.ticker.enabled:false}")
//     private boolean tickerEnabled;
    
//     @Value("${market-data.zerodha.api.access.token:}")
//     private String accessToken;
    
//     private KiteTicker tickerProvider;
//     private List<String> nseInstrumentsList;
//     private List<String> bseInstrumentsList;
//     private List<String> indexInstrumentsList;
//     private Map<String, Long> instrumentTokenMap = new HashMap<>();

//     public ZerodhaMarketDataScheduler(ZerodhaApiService zerodhaApiService, 
//                                      MeterRegistry meterRegistry,
//                                      ApplicationEventPublisher eventPublisher) {
//         this.zerodhaApiService = zerodhaApiService;
//         this.meterRegistry = meterRegistry;
//         this.eventPublisher = eventPublisher;
//     }

//     @PostConstruct
//     public void initialize() {
//         log.info("Initializing Zerodha Market Data Scheduler");
        
//         try {
//             // Parse instrument lists
//             nseInstrumentsList = Arrays.asList(nseInstruments.split(","));
//             bseInstrumentsList = Arrays.asList(bseInstruments.split(","));
//             indexInstrumentsList = Arrays.asList(indexInstruments.split(","));
            
//             log.info("Configured NSE instruments: {}", nseInstrumentsList);
//             log.info("Configured BSE instruments: {}", bseInstrumentsList);
//             log.info("Configured index instruments: {}", indexInstrumentsList);
            
//             // Set access token if provided
//             if (accessToken != null && !accessToken.isEmpty()) {
//                 zerodhaApiService.setAccessToken(accessToken);
//                 log.info("Set initial access token from configuration");
//             }
            
//             // Initial market data fetch if within trading hours
//             if (marketDataEnabled && isWithinTradingHours()) {
//                 log.info("Within trading hours at startup, performing initial market data fetch");
//                 //fetchMarketData();
                
//                 // Initialize ticker if enabled
//                 if (tickerEnabled) {
//                     initializeTicker();
//                 }
//             } else {
//                 log.info("Outside trading hours at startup, skipping initial market data fetch");
//             }
//         } catch (Exception e) {
//             log.error("Error during initialization of Zerodha Market Data Scheduler: {}", e.getMessage(), e);
//         }
//     }

//     @PreDestroy
//     public void cleanup() {
//         if (tickerProvider != null && tickerEnabled) {
//             log.info("Disconnecting Zerodha ticker");
//             zerodhaApiService.disconnectTicker();
//         }
//     }

//     /**
//      * Scheduled task to fetch market data during trading hours
//      * Runs every 2 minutes at 25 seconds past the minute during trading hours on weekdays
//      */
//     // @Scheduled(cron = "25 */2 * * * MON-FRI", zone = "${zerodha.market.timezone:Asia/Kolkata}")
//     // public void scheduledMarketDataFetch() {
//     //     if (!marketDataEnabled) {
//     //         log.debug("Zerodha market data fetching is disabled");
//     //         return;
//     //     }
        
//     //     if (!isWithinTradingHours()) {
//     //         log.debug("Outside trading hours, skipping scheduled market data fetch");
//     //         return;
//     //     }
        
//     //     log.info("Executing scheduled Zerodha market data fetch");
//     //     fetchMarketData();
//     // }

//     /**
//      * Fetch market data from Zerodha
//      * Uses parallel processing for different instrument types
//      */
//     public void fetchMarketData() {
//         Timer.Sample sample = Timer.start(meterRegistry);
        
//         try {
//             // Fetch quotes, OHLC, and LTP in parallel
//             CompletableFuture<Map<String, Quote>> quotesFuture = fetchQuotesAsync();
//             CompletableFuture<Map<String, OHLCQuote>> ohlcFuture = fetchOHLCAsync();
            
//             // Wait for all futures to complete
//             CompletableFuture.allOf(quotesFuture, ohlcFuture).join();
            
//             // Process results
//             Map<String, Quote> quotes = quotesFuture.get();
//             Map<String, OHLCQuote> ohlc = ohlcFuture.get();
            
//             // Log success
//             log.info("Successfully fetched Zerodha market data: {} quotes, {} OHLC", 
//                 quotes != null ? quotes.size() : 0, 
//                 ohlc != null ? ohlc.size() : 0);
            
//             // Process and publish data
//             processMarketData(quotes, ohlc);
            
//             // Record success metrics
//             meterRegistry.counter("zerodha.market.data.success").increment();
//             sample.stop(meterRegistry.timer("zerodha.market.data.fetch.time"));
//         } catch (Exception e) {
//             meterRegistry.counter("zerodha.market.data.failure").increment();
//             log.error("Failed to fetch Zerodha market data: {}", e.getMessage(), e);
//         }
//     }

//     /**
//      * Fetch quotes asynchronously
//      * @return CompletableFuture with quotes map
//      */
//     private CompletableFuture<Map<String, Quote>> fetchQuotesAsync() {
//         List<String> allInstruments = new ArrayList<>();
//         allInstruments.addAll(nseInstrumentsList);
//         allInstruments.addAll(bseInstrumentsList);
        
//         String[] instrumentsArray = allInstruments.toArray(new String[0]);
        
//         return zerodhaApiService.executeAsync(() -> zerodhaApiService.getQuotes(instrumentsArray));
//     }

//     /**
//      * Fetch OHLC data asynchronously
//      * @return CompletableFuture with OHLC map
//      */
//     private CompletableFuture<Map<String, OHLCQuote>> fetchOHLCAsync() {
//         String[] instrumentsArray = indexInstrumentsList.toArray(new String[0]);
//         return zerodhaApiService.executeAsync(() -> zerodhaApiService.getOHLC(instrumentsArray));
//     }

//     /**
//      * Process market data and publish events
//      * @param quotes Quote data
//      * @param ohlc OHLC data
//      */
//     private void processMarketData(Map<String, Quote> quotes, Map<String, OHLCQuote> ohlc) {
//         if (quotes == null && ohlc == null) {
//             log.warn("No market data available to process");
//             return;
//         }
        
//         Timer.Sample sample = Timer.start(meterRegistry);
        
//         try {
//             // Process quotes
//             if (quotes != null && !quotes.isEmpty()) {
//                 quotes.forEach((symbol, quote) -> {
//                     log.debug("Processing quote for {}: LTP={}, volume={}", 
//                         symbol, quote.lastPrice, quote);
                    
//                     // Create and publish event for each quote
//                     ZerodhaQuoteEvent event = new ZerodhaQuoteEvent(symbol, quote);
//                     eventPublisher.publishEvent(event);
//                 });
//             }
            
//             // Process OHLC
//             if (ohlc != null && !ohlc.isEmpty()) {
//                 ohlc.forEach((symbol, ohlcQuote) -> {
//                     log.debug("Processing OHLC for {}: open={}, high={}, low={}, close={}", 
//                         symbol, ohlcQuote.ohlc.open, ohlcQuote.ohlc.high, 
//                         ohlcQuote.ohlc.low, ohlcQuote.ohlc.close);
                    
//                     // Create and publish event for each OHLC
//                     ZerodhaOHLCEvent event = new ZerodhaOHLCEvent(symbol, ohlcQuote);
//                     eventPublisher.publishEvent(event);
//                 });
//             }
            
//             // Record metrics
//             meterRegistry.counter("zerodha.market.data.process.success").increment();
//             sample.stop(meterRegistry.timer("zerodha.market.data.process.time"));
//         } catch (Exception e) {
//             meterRegistry.counter("zerodha.market.data.process.failure").increment();
//             log.error("Failed to process Zerodha market data: {}", e.getMessage(), e);
//         }
//     }

//     /**
//      * Initialize ticker for real-time data
//      */
//     public void initializeTicker() {
//         if (!tickerEnabled) {
//             log.info("Zerodha ticker is disabled");
//             return;
//         }
        
//         try {
//             // Fetch instruments to get tokens
//             loadInstrumentTokens();
            
//             if (instrumentTokenMap.isEmpty()) {
//                 log.warn("No instrument tokens available for ticker");
//                 return;
//             }
            
//             // Get tokens for configured instruments
//             List<Long> tokens = new ArrayList<>();
//             for (String instrument : indexInstrumentsList) {
//                 if (instrumentTokenMap.containsKey(instrument)) {
//                     tokens.add(instrumentTokenMap.get(instrument));
//                 }
//             }
            
//             if (tokens.isEmpty()) {
//                 log.warn("No matching tokens found for configured instruments");
//                 return;
//             }
            
//             log.info("Initializing ticker with {} tokens", tokens.size());
            
//             // Initialize ticker with tokens
//             tickerProvider = zerodhaApiService.initializeTicker(tokens, ticks -> {
//                 if (ticks == null || ticks.isEmpty()) {
//                     return;
//                 }
                
//                 log.debug("Received {} ticks", ticks.size());
                
//                 // Process ticks
//                 ticks.forEach(tick -> {
//                     log.debug("Tick for {}: price={}, volume={}", 
//                         tick.getInstrumentToken(), tick.getLastTradedPrice(), tick.getVolumeTradedToday());
                    
//                     // Create and publish event for each tick
//                     ZerodhaTickEvent event = new ZerodhaTickEvent(tick);
//                     eventPublisher.publishEvent(event);
//                 });
//             });
            
//             log.info("Zerodha ticker initialized successfully");
//         } catch (Exception e) {
//             log.error("Failed to initialize Zerodha ticker: {}", e.getMessage(), e);
//         }
//     }

//     /**
//      * Load instrument tokens for ticker
//      */
//     private void loadInstrumentTokens() {
//         try {
//             List<Instrument> instruments = zerodhaApiService.getAllInstruments();
            
//             if (instruments == null || instruments.isEmpty()) {
//                 log.warn("No instruments returned from Zerodha API");
//                 return;
//             }
            
//             log.info("Loaded {} instruments from Zerodha", instruments.size());
            
//             // Create mapping of symbol to token
//             for (Instrument instrument : instruments) {
//                 String key = instrument.exchange + ":" + instrument.tradingsymbol;
//                 instrumentTokenMap.put(key, instrument.instrument_token);
//             }
            
//             log.info("Created instrument token map with {} entries", instrumentTokenMap.size());
//         } catch (Exception e) {
//             log.error("Failed to load instrument tokens: {}", e.getMessage(), e);
//         }
//     }

//     /**
//      * Check if current time is within trading hours
//      * @return true if within trading hours, false otherwise
//      */
//     private boolean isWithinTradingHours() {
//         ZoneId zoneId = ZoneId.of(marketTimezone);
//         LocalTime currentTime = LocalDateTime.now(zoneId).toLocalTime();
//         LocalTime startTime = LocalTime.parse(marketHoursStart);
//         LocalTime endTime = LocalTime.parse(marketHoursEnd);
        
//         return !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime);
//     }
// }
