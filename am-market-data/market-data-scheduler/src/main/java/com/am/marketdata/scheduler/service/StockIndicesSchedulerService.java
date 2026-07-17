 package com.am.marketdata.scheduler.service;

import com.am.marketdata.scraper.exception.CookieException;
import com.am.marketdata.scraper.service.MarketDataProcessingService;
import com.am.marketdata.provider.upstox.UpstoxApiService;
import com.am.marketdata.provider.upstox.UpstoxIndexIdentifier;
import com.am.marketdata.provider.upstox.model.MarketQuoteResponse;
import com.am.marketdata.provider.upstox.model.common.StockQuote;
import com.am.common.investment.service.MarketIndexIndicesService;
import com.am.common.investment.service.StockIndicesMarketDataService;
import com.am.common.investment.model.stockindice.StockIndicesMarketData;
import com.am.common.investment.model.equity.MarketIndexIndices;
import com.am.common.investment.model.equity.MarketData;
import com.am.marketdata.scraper.config.NSEIndicesConfig;
import com.am.marketdata.kafka.producer.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicBoolean;

import com.am.observability.trace.IgnoreTracing;

/**
 * Scheduler service specifically for stock indices data.
 * Runs at 9:30 AM and 4:00 PM with retry mechanism.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "scheduler.stock-indices.enabled", havingValue = "true", matchIfMissing = true)
@IgnoreTracing
public class StockIndicesSchedulerService {
    @Value("${scheduler.stock-indices.retry.interval-minutes:15}")
    private int retryIntervalMinutes;

    @Value("${scheduler.stock-indices.retry.max-retries:20}")
    private int maxRetries;

    // Dependent service from scraper module
    private final MarketDataProcessingService marketDataProcessingService;
    private final UpstoxApiService upstoxApiService;
    private final UpstoxIndexIdentifier upstoxIndexIdentifier;
    private final MarketIndexIndicesService marketIndexIndicesService;
    private final StockIndicesMarketDataService stockIndicesMarketDataService;
    private final NSEIndicesConfig nseIndicesConfig;
    private final Optional<KafkaProducerService> kafkaProducer;

    private final AtomicBoolean morningFetchCompleted = new AtomicBoolean(false);
    private final AtomicBoolean eveningFetchCompleted = new AtomicBoolean(false);
    private int currentRetryCount = 0;
    private LocalDate lastProcessedDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));

    @PostConstruct
    public void initialize() {
        log.info("Initializing StockIndicesSchedulerService with retry interval: {} minutes, max retries: {}",
                retryIntervalMinutes, maxRetries);

        // Reset flags at startup based on current time
        resetFlagsIfNeeded();
    }

    /**
     * Morning schedule at 9:30 AM IST
     */
    /**
     * Morning schedule at 9:30 AM IST
     */
    public void executeMorningStockIndicesFetch() {
        resetFlagsIfNeeded();

        if (!morningFetchCompleted.get()) {
            log.info("Starting scheduled morning stock indices fetch at 9:30 AM");
            fetchStockIndicesWithRetry(true);
        } else {
            log.debug("Morning stock indices already fetched successfully today");
        }
    }

    /**
     * Evening schedule at 4:00 PM IST
     */
    public void executeEveningStockIndicesFetch() {
        resetFlagsIfNeeded();

        if (!eveningFetchCompleted.get()) {
            log.info("Starting scheduled evening stock indices fetch at 4:00 PM");
            fetchStockIndicesWithRetry(false);
        } else {
            log.debug("Evening stock indices already fetched successfully today");
        }
    }

    /**
     * Retry scheduler that runs every X minutes if needed
     */
    public void executeRetryJob() {
        resetFlagsIfNeeded();

        // Only retry if we have active failures and haven't exceeded max retries
        if (currentRetryCount > 0 && currentRetryCount < maxRetries) {
            // In dev mode, don't check trading hours
            if (isDevMode()) {
                log.info("Development mode: Attempting retry regardless of time");
                fetchStockIndicesWithRetry(true); // Always retry morning session in dev
                return;
            }

            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
            int currentHour = now.getHour();

            // Morning session: retry between 9:30 AM and 3:30 PM if morning fetch failed
            if (currentHour >= 9 && currentHour < 15 && !morningFetchCompleted.get()) {
                log.info("Retry attempt #{} for morning stock indices fetch", currentRetryCount + 1);
                fetchStockIndicesWithRetry(true);
            }
            // Evening session: retry between 4:00 PM and 6:00 PM if evening fetch failed
            else if (currentHour >= 16 && currentHour < 18 && !eveningFetchCompleted.get()) {
                log.info("Retry attempt #{} for evening stock indices fetch", currentRetryCount + 1);
                fetchStockIndicesWithRetry(false);
            }
        } else if (currentRetryCount >= maxRetries) {
            log.warn("Max retry attempts ({}) reached for stock indices fetch", maxRetries);
            // Reset retry count but keep the completion flags as is
            currentRetryCount = 0;
        }
    }

    /**
     * Fetch stock indices with retry logic
     * 
     * @param isMorningSession true if this is the morning session, false for
     *                         evening
     */
    private void fetchStockIndicesWithRetry(boolean isMorningSession) {
        try {
            log.info("Attempting to fetch stock indices data from Upstox (session: {})",
                    isMorningSession ? "morning" : "evening");

            boolean success = fetchAndProcessStockIndicesFromUpstox();

            if (success) {
                log.info("Successfully fetched and processed stock indices data from Upstox (session: {})",
                        isMorningSession ? "morning" : "evening");

                // Mark the appropriate session as completed
                if (isMorningSession) {
                    morningFetchCompleted.set(true);
                } else {
                    eveningFetchCompleted.set(true);
                }

                // Reset retry count on success
                currentRetryCount = 0;
            } else {
                // Increment retry count on failure
                currentRetryCount++;
                log.warn("Failed to fetch stock indices data from Upstox (session: {}). Will retry in {} minutes. Attempt {}/{}",
                        isMorningSession ? "morning" : "evening",
                        retryIntervalMinutes,
                        currentRetryCount,
                        maxRetries);
            }
        } catch (Exception e) {
            currentRetryCount++;
            log.error(
                    "Unexpected error during stock indices fetch from Upstox (session: {}): {}. Will retry in {} minutes. Attempt {}/{}",
                    isMorningSession ? "morning" : "evening",
                    e.getMessage(),
                    retryIntervalMinutes,
                    currentRetryCount,
                    maxRetries,
                    e);
        }
    }

    private boolean fetchAndProcessStockIndicesFromUpstox() {
        try {
            log.info("Fetching stock indices using Upstox API...");
            List<String> allIndices = new ArrayList<>();
            if (nseIndicesConfig.getBroadMarketIndices() != null) {
                allIndices.addAll(nseIndicesConfig.getBroadMarketIndices());
            }
            if (nseIndicesConfig.getSectorIndices() != null) {
                allIndices.addAll(nseIndicesConfig.getSectorIndices());
            }

            // Map symbols to instrument keys
            Map<String, String> resolved = upstoxIndexIdentifier.resolveIndices(allIndices);
            if (resolved.isEmpty()) {
                log.warn("No index symbols resolved to Upstox instrument keys.");
                return false;
            }

            List<String> instrumentKeys = new ArrayList<>(resolved.values());
            log.info("Resolved instrument keys to fetch: {}", instrumentKeys);
            MarketQuoteResponse response = upstoxApiService.getLtp(instrumentKeys);
            if (response == null) {
                log.warn("Received null response from Upstox API.");
                return false;
            }
            if (response.getData() == null) {
                log.warn("Received response with null data from Upstox API. Status: {}", response.getStatus());
                return false;
            }
            log.info("Upstox response data keys: {}", response.getData().keySet());

            List<MarketIndexIndices> indicesToSave = new ArrayList<>();
            for (Map.Entry<String, String> entry : resolved.entrySet()) {
                String symbol = entry.getKey();
                String key = entry.getValue();
                StockQuote quote = response.getData().get(key);
                if (quote == null) {
                    // Try alternative key format (e.g. colon instead of pipe)
                    String altKey = key.replace('|', ':');
                    quote = response.getData().get(altKey);
                }
                if (quote == null) {
                    log.warn("No quote found in Upstox response for key: {} (altKey: {})", key, key.replace('|', ':'));
                    continue;
                }

                log.info("Found quote for index {}: lastPrice={}, openPrice={}", symbol, quote.getLastPrice(), quote.getOpenPrice());

                double lastPrice = quote.getLastPrice() != null ? quote.getLastPrice() : 0.0;
                double change = quote.getChange() != null ? quote.getChange() : 0.0;
                double previousClose = quote.getPreviousClose() != null ? quote.getPreviousClose() : 0.0;
                if (previousClose == 0.0 && lastPrice != 0.0) {
                    previousClose = lastPrice - change;
                }
                double changePercent = quote.getChangePercent() != null ? quote.getChangePercent() : 0.0;
                if (changePercent == 0.0 && previousClose != 0.0) {
                    changePercent = (change / previousClose) * 100.0;
                }

                MarketData marketData = MarketData.builder()
                    .last(lastPrice)
                    .open(quote.getOpenPrice() != null ? quote.getOpenPrice() : 0.0)
                    .high(quote.getHighPrice() != null ? quote.getHighPrice() : 0.0)
                    .low(quote.getLowPrice() != null ? quote.getLowPrice() : 0.0)
                    .previousClose(previousClose)
                    .variation(change)
                    .percentChange(changePercent)
                    .build();

                MarketIndexIndices indexData = MarketIndexIndices.builder()
                    .key(key)
                    .indexSymbol(symbol)
                    .index(symbol)
                    .marketData(marketData)
                    .timestamp(LocalDateTime.now(ZoneId.of("Asia/Kolkata")))
                    .build();

                marketIndexIndicesService.save(indexData);
                indicesToSave.add(indexData);

                // Update MongoDB permanent StockIndicesMarketData collection
                try {
                    StockIndicesMarketData mongoDoc = stockIndicesMarketDataService.findByIndexSymbol(symbol);
                    if (mongoDoc != null) {
                        com.am.common.investment.model.events.StockInsidicesEventData.IndexMetadata meta = mongoDoc.getMetadata();
                        if (meta == null) {
                            meta = new com.am.common.investment.model.events.StockInsidicesEventData.IndexMetadata();
                            mongoDoc.setMetadata(meta);
                        }
                        meta.setLast(lastPrice);
                        meta.setOpen(quote.getOpenPrice() != null ? quote.getOpenPrice() : 0.0);
                        meta.setHigh(quote.getHighPrice() != null ? quote.getHighPrice() : 0.0);
                        meta.setLow(quote.getLowPrice() != null ? quote.getLowPrice() : 0.0);
                        meta.setPreviousClose(previousClose);
                        meta.setChange(change);
                        meta.setPercChange(changePercent);
                        meta.setTimeVal(String.valueOf(System.currentTimeMillis()));

                        if (mongoDoc.getAudit() == null) {
                            mongoDoc.setAudit(new com.am.common.investment.model.stockindice.AuditData());
                        }
                        mongoDoc.getAudit().setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));

                        stockIndicesMarketDataService.save(mongoDoc);
                        log.info("Successfully updated MongoDB StockIndicesMarketData for index symbol: {}", symbol);
                    } else {
                        log.warn("StockIndicesMarketData document not found in MongoDB for symbol: {}", symbol);
                    }
                } catch (Exception ex) {
                    log.error("Failed to update MongoDB StockIndicesMarketData for symbol: {}", symbol, ex);
                }
            }

            if (!indicesToSave.isEmpty()) {
                kafkaProducer.ifPresent(producer -> producer.sendIndicesUpdate(indicesToSave));
                log.info("Successfully saved {} indices from Upstox and sent updates to Kafka.", indicesToSave.size());
                return true;
            }
            log.warn("No indices were saved because indicesToSave is empty.");
            return false;
        } catch (Exception e) {
            log.error("Failed to fetch/process indices from Upstox", e);
            return false;
        }
    }

    /**
     * Reset flags if it's a new day
     */
    private void resetFlagsIfNeeded() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

        // If it's a new day, reset all flags
        if (!today.equals(lastProcessedDate)) {
            log.info("New day detected. Resetting stock indices fetch flags");
            lastProcessedDate = today;
            morningFetchCompleted.set(false);
            eveningFetchCompleted.set(false);
            currentRetryCount = 0;
        }

        // In dev mode, don't reset flags based on time
        if (isDevMode()) {
            log.debug("Development mode: Not resetting flags based on time");
            return;
        }

        // Also reset flags based on current time
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        int currentHour = now.getHour();

        // Before 9:30 AM, reset morning flag
        if (currentHour < 9 || (currentHour == 9 && now.getMinute() < 30)) {
            if (morningFetchCompleted.get()) {
                log.debug("Resetting morning fetch flag as it's before 9:30 AM");
                morningFetchCompleted.set(false);
            }
        }

        // Before 4:00 PM, reset evening flag
        if (currentHour < 16) {
            if (eveningFetchCompleted.get()) {
                log.debug("Resetting evening fetch flag as it's before 4:00 PM");
                eveningFetchCompleted.set(false);
            }
        }
    }

    /**
     * Check if we're running in development mode
     */
    private boolean isDevMode() {
        return System.getenv("SPRING_PROFILES_ACTIVE") != null &&
                System.getenv("SPRING_PROFILES_ACTIVE").equals("dev");
    }
}
