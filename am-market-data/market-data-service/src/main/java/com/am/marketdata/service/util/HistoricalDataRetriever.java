package com.am.marketdata.service.util;

import com.am.common.investment.model.historical.HistoricalData;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.service.MarketDataPersistenceService;
import com.marketdata.common.MarketDataProvider;
import com.am.marketdata.provider.common.MarketDataProviderFactory;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Concrete implementation of AbstractMarketDataRetriever for Historical data.
 * Handles retrieval of historical data from cache, database, and provider.
 */
@Slf4j
public class HistoricalDataRetriever extends AbstractMarketDataRetriever<String, HistoricalData> {

    private final Date fromDate;
    private final Date toDate;
    private final TimeFrame interval;
    private final boolean continuous;
    private final Map<String, Object> additionalParams;
    private final boolean isIndexSymbol;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final com.am.marketdata.service.kafka.producer.MarketDataProducer producer;

    private HistoricalDataRetriever(
            MarketDataPersistenceService persistenceService,
            MarketDataProviderFactory providerFactory,
            List<DataSourceType> retrievalOrder,
            boolean cacheResults,
            Date fromDate,
            Date toDate,
            TimeFrame interval,
            boolean continuous,
            Map<String, Object> additionalParams,
            String targetProviderName,
            boolean isIndexSymbol,
            com.am.marketdata.service.kafka.producer.MarketDataProducer producer) {
        super(persistenceService, providerFactory, retrievalOrder, cacheResults, targetProviderName);
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.interval = interval;
        this.continuous = continuous;
        this.additionalParams = additionalParams != null ? additionalParams : new HashMap<>();
        this.isIndexSymbol = isIndexSymbol;
        this.producer = producer;
    }

    /**
     * Retrieve historical data from cache
     *
     * @param allSymbols       All symbols being requested
     * @param remainingSymbols Set of symbols that still need to be retrieved (will
     *                         be modified)
     * @param timeFrame        The time frame for the data (ignored as we use the
     *                         interval from constructor)
     * @return Map of symbol to historical data
     */
    @Override
    protected Map<String, HistoricalData> retrieveFromCache(List<String> allSymbols, Set<String> remainingSymbols,
            TimeFrame timeFrame) {
        if (remainingSymbols.isEmpty()) {
            return Collections.emptyMap();
        }

        log.info("[CACHE] Attempting to fetch historical data from cache for {} symbols",
                remainingSymbols.size());

        String fromDateStr = dateFormat.format(fromDate);
        String toDateStr = dateFormat.format(toDate);

        // Use batch retrieval with isIndexSymbol parameter
        Map<String, HistoricalData> result = persistenceService.getMarketDataCacheService()
                .getHistoricalDataFromCacheBatch(new ArrayList<>(remainingSymbols), interval, fromDateStr, toDateStr,
                        isIndexSymbol);

        // VALIDATION: Strict Date Range Check
        // Filter out cached data that does not cover the full requested range
        // This prevents partial cache hits (e.g., finding only 1 month of data when 1
        // year is requested)
        long requiredStartMs = fromDate.getTime();
        long requiredEndMs = toDate.getTime();
        
        // [Intraday validation threshold fix]
        // Indices are stored as daily snapshots (only 2 scheduler points per day) in the database.
        // If the user requests 5m or 1H candles, a 7-day tolerance would allow daily snapshots to pass validation,
        // skipping Upstox fallback. We resolve this by using a strict 30-minute tolerance for intraday intervals
        // to force fallback fetching from the provider when 5m candles are missing.
        // Tolerance is only used for daily/weekly/monthly intervals.
        // For intraday (5m, 1H), we use date-based comparison instead (see below).
        long toleranceMs;
        String val = interval.getApiValue().toLowerCase();
        boolean isIntraday = val.contains("m") || val.contains("h");
        if (!isIntraday) {
            toleranceMs = 7L * 24 * 60 * 60 * 1000; // 7 days tolerance for daily/weekly/monthly charts
        } else {
            toleranceMs = 0; // Not used for intraday — date comparison is used instead
        }

        List<String> keysToRemove = new ArrayList<>();

        for (Map.Entry<String, HistoricalData> entry : result.entrySet()) {
            String symbol = entry.getKey();
            HistoricalData data = entry.getValue();

            if (data == null || data.getDataPoints() == null || data.getDataPoints().isEmpty()) {
                keysToRemove.add(symbol);
                continue;
            }

            List<com.am.common.investment.model.historical.OHLCVTPoint> points = data.getDataPoints();

            // Check start date (ascending order assumed)
            java.time.LocalDateTime firstPointTime = points.get(0).getTime();
            java.time.LocalDateTime lastPointTime = points.get(points.size() - 1).getTime();

            long dataStartMs = firstPointTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            long dataEndMs = lastPointTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

            // Logic:
            // 1. Data Start must be <= Request Start + Tolerance (Data shouldn't start too
            // late)
            // 2. Data End must be >= Request End - Tolerance (Data shouldn't end too early)

            long validationStartMs = getFirstExpectedCandleTimeMs(requiredStartMs);
            long validationEndMs = getLastExpectedCandleTimeMs(requiredEndMs);
            
            // For intraday: check if first candle date is after the expected start date.
            // This prevents false discards if the first candle is slightly after 09:15 (e.g. 09:16).
            boolean missingEarlyData;
            if (isIntraday) {
                java.time.LocalDate firstCandleDate = firstPointTime.toLocalDate();
                java.time.LocalDate validationStartDate = java.time.Instant.ofEpochMilli(validationStartMs)
                        .atZone(java.time.ZoneId.of("Asia/Kolkata")).toLocalDate();
                missingEarlyData = firstCandleDate.isAfter(validationStartDate);
            } else {
                missingEarlyData = dataStartMs > (validationStartMs + toleranceMs);
            }

            // [Intraday Recent-Data Check - Date Comparison]
            // The DB scheduler saves only 1-2 candles per day (morning snapshot).
            // So the last 5m or 1H candle may be at 09:38 AM even though market closes at 15:30.
            // Comparing raw timestamps would flag this as missing (gap = 5h52m > any small tolerance).
            // Fix: for intraday intervals, check only the DATE of the last candle vs the validation target date.
            //   - Last candle from correct trading date -> valid, no matter what time -> accept.
            //   - Last candle from a previous date -> genuinely stale -> reject, fallback to Upstox.
            boolean missingRecentData;
            if (isIntraday) {
                java.time.LocalDate lastCandleDate = lastPointTime.toLocalDate();
                java.time.LocalDate validationEndDate = java.time.Instant.ofEpochMilli(validationEndMs)
                        .atZone(java.time.ZoneId.of("Asia/Kolkata")).toLocalDate();
                missingRecentData = lastCandleDate.isBefore(validationEndDate);
            } else {
                // For daily/weekly/monthly candles: use 7-day tolerance timestamp comparison
                missingRecentData = dataEndMs < (validationEndMs - toleranceMs);
            }

            if (missingEarlyData || missingRecentData) {
                log.info("[CACHE_VALIDATION] Partial cache hit detected for {}. Invalidating cache entry. " +
                        "Req: {} to {} (validation target: {} to {}), Found: {} to {}. MissingEarly: {}, MissingRecent: {}",
                        symbol, fromDateStr, toDateStr,
                        new java.sql.Timestamp(validationStartMs), new java.sql.Timestamp(validationEndMs),
                        firstPointTime.toLocalDate(), lastPointTime.toLocalDate(),
                        missingEarlyData, missingRecentData);
                keysToRemove.add(symbol);
            }
        }

        // Remove invalid entries
        for (String key : keysToRemove) {
            result.remove(key);
        }

        if (!result.isEmpty()) {
            remainingSymbols.removeAll(result.keySet());
            log.info("[CACHE] Found valid historical data for {}/{} symbols interval={}",
                    result.size(), allSymbols.size(), interval.getApiValue());
        }

        log.info("[CACHE] {} symbols remaining after cache lookup interval={}", remainingSymbols.size(), interval.getApiValue());

        return result;
    }

    /**
     * Retrieve historical data from database
     *
     * @param remainingSymbols Set of symbols that still need to be retrieved (will
     *                         be modified)
     * @param timeFrame        The time frame for the data (ignored as we use the
     *                         interval from constructor)
     * @return Map of symbol to historical data
     */
    @Override
    protected Map<String, HistoricalData> retrieveFromDatabase(Set<String> remainingSymbols, TimeFrame timeFrame) {
        if (remainingSymbols.isEmpty()) {
            return Collections.emptyMap();
        }

        log.info("[DATABASE] Attempting to fetch historical data from database for {} symbols",
                remainingSymbols.size());

        Map<String, HistoricalData> result = new HashMap<>();
        String fromDateStr = dateFormat.format(fromDate);
        String toDateStr = dateFormat.format(toDate);

        for (String symbol : new ArrayList<>(remainingSymbols)) {
            try {
                // Force database lookup by setting forceRefresh to true in the persistence
                // service
                HistoricalData data = persistenceService.getHistoricalData(symbol, interval, fromDateStr, toDateStr, isIndexSymbol);
                if (data != null && data.getDataPoints() != null && !data.getDataPoints().isEmpty()) {
                    List<com.am.common.investment.model.historical.OHLCVTPoint> points = data.getDataPoints();
                    
                    // VALIDATION: Strict Date Range Check
                    // Filter out database data that does not cover the full requested range
                    // This prevents partial database hits (e.g., finding only 1 point)
                    long requiredStartMs = fromDate.getTime();
                    long requiredEndMs = toDate.getTime();
                    
                    // [Intraday validation threshold fix]
                    // Strict 30-minute tolerance check for intraday candles to discard incomplete
                    // daily scheduler snapshots and fallback to the provider.
                    // Tolerance is only used for daily/weekly/monthly intervals.
                    // For intraday (5m, 1H), we use date-based comparison instead (see below).
                    long toleranceMs;
                    String val = interval.getApiValue().toLowerCase();
                    boolean isIntraday = val.contains("m") || val.contains("h");
                    if (!isIntraday) {
                        toleranceMs = 7L * 24 * 60 * 60 * 1000; // 7 days for daily/weekly/monthly charts
                    } else {
                        toleranceMs = 0; // Not used for intraday - date comparison is used instead
                    }

                    java.time.LocalDateTime firstPointTime = points.get(0).getTime();
                    java.time.LocalDateTime lastPointTime = points.get(points.size() - 1).getTime();

                    long dataStartMs = firstPointTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                    long dataEndMs = lastPointTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

                    long validationStartMs = getFirstExpectedCandleTimeMs(requiredStartMs);
                    long validationEndMs = getLastExpectedCandleTimeMs(requiredEndMs);
                    
                    // For intraday: check if first candle date is after the expected start date.
                    // This prevents false discards if the first candle is slightly after 09:15 (e.g. 09:16).
                    boolean missingEarlyData;
                    if (isIntraday) {
                        java.time.LocalDate firstCandleDate = firstPointTime.toLocalDate();
                        java.time.LocalDate validationStartDate = java.time.Instant.ofEpochMilli(validationStartMs)
                                .atZone(java.time.ZoneId.of("Asia/Kolkata")).toLocalDate();
                        missingEarlyData = firstCandleDate.isAfter(validationStartDate);
                    } else {
                        missingEarlyData = dataStartMs > (validationStartMs + toleranceMs);
                    }

                    // [Intraday Recent-Data Check - Date Comparison]
                    // The DB scheduler saves only 1-2 candles per day (morning snapshot).
                    // So the last 5m or 1H candle may be at 09:38 AM even though market closes at 15:30.
                    // Comparing raw timestamps would flag this as missing (gap = 5h52m > any small tolerance).
                    // Fix: for intraday intervals, check only the DATE of the last candle vs the validation target date.
                    //   - Last candle from correct trading date -> valid, no matter what time -> accept.
                    //   - Last candle from a previous date -> genuinely stale -> reject, fallback to Upstox.
                    boolean missingRecentData;
                    if (isIntraday) {
                        java.time.LocalDate lastCandleDate = lastPointTime.toLocalDate();
                        java.time.LocalDate validationEndDate = java.time.Instant.ofEpochMilli(validationEndMs)
                                .atZone(java.time.ZoneId.of("Asia/Kolkata")).toLocalDate();
                        missingRecentData = lastCandleDate.isBefore(validationEndDate);
                    } else {
                        // For daily/weekly/monthly candles: use 7-day tolerance timestamp comparison
                        missingRecentData = dataEndMs < (validationEndMs - toleranceMs);
                    }

                    if (missingEarlyData || missingRecentData) {
                        log.info("[DATABASE_VALIDATION] Partial database data detected for {}. Discarding database entry to force fallback to provider. " +
                                "Req: {} to {} (validation target: {} to {}), Found: {} to {} (lastTime={}, dataEndMs={}, validationEndMs={}, toleranceMs={}). MissingEarly: {}, MissingRecent: {}",
                                symbol, fromDateStr, toDateStr,
                                new java.sql.Timestamp(validationStartMs), new java.sql.Timestamp(validationEndMs),
                                firstPointTime.toLocalDate(), lastPointTime.toLocalDate(),
                                lastPointTime, dataEndMs, validationEndMs, toleranceMs,
                                missingEarlyData, missingRecentData);
                    } else {
                        result.put(symbol, data);
                        remainingSymbols.remove(symbol);
                        log.debug("[DATABASE] Found valid historical data for symbol: {}", symbol);
                    }
                }
            } catch (Exception e) {
                log.warn("[DATABASE] Error retrieving historical data for symbol {}: {}", symbol, e.getMessage());
            }
        }

        log.info("[DATABASE] Found historical data for {} symbols in database interval={}", result.size(), interval.getApiValue());
        log.info("[DATABASE] {} symbols remaining after database lookup interval={}", remainingSymbols.size(), interval.getApiValue());

        return result;
    }

    /**
     * Retrieve historical data from provider
     *
     * @param provider The market data provider
     * @param symbols  List of symbols to retrieve
     * @return Map of symbol to historical data
     */
    @Override
    protected Map<String, HistoricalData> retrieveFromProvider(MarketDataProvider provider, List<String> symbols) {
        if (symbols.isEmpty()) {
            return Collections.emptyMap();
        }

        log.info("[PROVIDER] Fetching historical data from provider for {} symbols", symbols.size());

        Map<String, HistoricalData> result = new HashMap<>();
        // Mapper not needed anymore as provider returns the correct model

        int callCount = 0;
        for (String symbol : symbols) {
            try {
                if ("upstox".equalsIgnoreCase(provider.getProviderName())) {
                    if (callCount > 0) {
                        try {
                            Thread.sleep(300); // Respect Upstox rate limits (3 requests/sec to be safe)
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.warn("[PROVIDER] Interrupted during historical data fetch sleep");
                            break;
                        }
                    }
                    callCount++;
                }

                // Provider now returns the common HistoricalData model directly
                HistoricalData historicalData = provider.getHistoricalData(symbol,
                        fromDate, toDate, interval, continuous, additionalParams);

                if (historicalData != null && historicalData.getDataPoints() != null
                        && !historicalData.getDataPoints().isEmpty()) {
                    // Ensure trading symbol is set
                    if (historicalData.getTradingSymbol() == null || historicalData.getTradingSymbol().isEmpty()) {
                        historicalData.setTradingSymbol(symbol);
                    }
                    result.put(symbol, historicalData);
                    log.debug("[PROVIDER] Successfully fetched historical data for symbol: {}", symbol);
                } else {
                    log.warn("[PROVIDER] No historical data returned for symbol: {}", symbol);
                }
            } catch (Exception e) {
                log.error("[PROVIDER] Error fetching historical data for symbol {}: {}", symbol, e.getMessage(), e);
            }
        }

        log.info("[PROVIDER] Successfully fetched historical data for {}/{} symbols interval={} provider={}",
                result.size(), symbols.size(), interval.getApiValue(), provider.getProviderName());
        return result;
    }

    /**
     * Save historical data to persistence asynchronously (both database and cache)
     *
     * @param data The data to save
     */
    @Override
    protected void saveDataAsync(Map<String, HistoricalData> data) {
        if (data == null || data.isEmpty()) {
            return;
        }

        String methodName = "saveDataAsync";
        try {
            log.info(methodName, "[PROVIDER_ASYNC] Sending {} historical data records to KAFKA for ingestion",
                    data.size());

            if (producer != null) {
                // Use producer to send to Kafka (Fire and Forget)
                producer.sendHistoricalData(data, this.interval, targetProviderName);
                log.info(methodName, "[PROVIDER_ASYNC] Successfully sent ingestion event to Kafka");
            } else {
                log.warn(methodName, "[PROVIDER_ASYNC] Skipping Kafka ingestion (Kafka disabled/Producer null)");
            }
        } catch (Exception e) {
            log.error(methodName,
                    "[PROVIDER_ASYNC] FAILED to send historical data to KAFKA: " + e.getMessage(), e);
        }
    }

    /**
     * Update only the cache with the provided historical data, without saving to
     * database
     *
     * @param data The data to update in the cache
     */
    @Override
    protected void updateCacheOnly(Map<String, HistoricalData> data) {
        if (data == null || data.isEmpty()) {
            return;
        }

        try {
            for (Map.Entry<String, HistoricalData> entry : data.entrySet()) {
                // Use the MarketDataCacheService directly to update only the cache
                persistenceService.getMarketDataCacheService().cacheHistoricalData(
                        entry.getKey(), interval, entry.getValue());
            }
            log.debug("Updated cache with historical data for {} symbols", data.size());
        } catch (Exception e) {
            log.error("Error updating cache with historical data: {}", e.getMessage(), e);
        }
    }

    /**
     * Builder for HistoricalDataRetriever
     */
    public static class Builder extends AbstractBuilder<String, HistoricalData, Builder, HistoricalDataRetriever> {
        private Date fromDate;
        private Date toDate;
        private TimeFrame interval;
        private boolean continuous;
        private Map<String, Object> additionalParams;
        private boolean isIndexSymbol;
        private com.am.marketdata.service.kafka.producer.MarketDataProducer producer;

        public Builder producer(com.am.marketdata.service.kafka.producer.MarketDataProducer producer) {
            this.producer = producer;
            return this;
        }

        public Builder fromDate(Date fromDate) {
            this.fromDate = fromDate;
            return this;
        }

        public Builder toDate(Date toDate) {
            this.toDate = toDate;
            return this;
        }

        public Builder interval(TimeFrame interval) {
            this.interval = interval;
            return this;
        }

        public Builder continuous(boolean continuous) {
            this.continuous = continuous;
            return this;
        }

        public Builder additionalParams(Map<String, Object> additionalParams) {
            this.additionalParams = additionalParams;
            return this;
        }

        public Builder isIndexSymbol(boolean isIndexSymbol) {
            this.isIndexSymbol = isIndexSymbol;
            return this;
        }

        @Override
        public HistoricalDataRetriever build() {
            if (persistenceService == null) {
                throw new IllegalStateException("PersistenceService must be provided");
            }
            if (providerFactory == null) {
                throw new IllegalStateException("ProviderFactory must be provided");
            }
            if (fromDate == null) {
                throw new IllegalStateException("FromDate must be provided");
            }
            if (toDate == null) {
                throw new IllegalStateException("ToDate must be provided");
            }
            if (interval == null) {
                throw new IllegalStateException("Interval must be provided");
            }

            // Producer is optional now
            // if (producer == null) {
            // throw new IllegalStateException("MarketDataProducer must be provided");
            // }

            return new HistoricalDataRetriever(
                    persistenceService,
                    providerFactory,
                    retrievalOrder,
                    cacheResults != null ? cacheResults : true,
                    fromDate,
                    toDate,
                    interval,
                    continuous,
                    additionalParams,
                    targetProviderName,
                    isIndexSymbol,
                    producer);
        }
    }

    private long getFirstExpectedCandleTimeMs(long requestedFromDateMs) {
        java.time.ZonedDateTime requestDateTime = java.time.Instant.ofEpochMilli(requestedFromDateMs)
                .atZone(java.time.ZoneId.of("Asia/Kolkata"));
        java.time.LocalDate requestDate = requestDateTime.toLocalDate();
        
        // Roll forward to Monday if start date is on weekend
        java.time.LocalDate targetDate = requestDate;
        if (requestDate.getDayOfWeek() == java.time.DayOfWeek.SATURDAY) {
            targetDate = requestDate.plusDays(2);
        } else if (requestDate.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            targetDate = requestDate.plusDays(1);
        }
        
        java.time.LocalDateTime startDateTime = java.time.LocalDateTime.of(targetDate, java.time.LocalTime.of(9, 15));
        return startDateTime.atZone(java.time.ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli();
    }

    private long getLastExpectedCandleTimeMs(long requestedToDateMs) {
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        java.time.LocalDate requestedDate = java.time.Instant.ofEpochMilli(requestedToDateMs)
                .atZone(java.time.ZoneId.of("Asia/Kolkata")).toLocalDate();
        java.time.LocalDate today = now.toLocalDate();
        
        // If the requested date is strictly in the past (before today), expect full data for it
        if (requestedDate.isBefore(today)) {
            return requestedToDateMs;
        }
        
        // Otherwise, cap expected end time based on the active trading session
        java.time.LocalTime time = now.toLocalTime();
        
        // Determine the target date for the latest candles
        java.time.LocalDate targetDate = today;
        
        // If it's weekend, roll back to Friday
        if (today.getDayOfWeek() == java.time.DayOfWeek.SATURDAY) {
            targetDate = today.minusDays(1);
        } else if (today.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            targetDate = today.minusDays(2);
        } else if (today.getDayOfWeek() == java.time.DayOfWeek.MONDAY && time.isBefore(java.time.LocalTime.of(9, 15))) {
            targetDate = today.minusDays(3);
        } else if (time.isBefore(java.time.LocalTime.of(9, 15))) {
            targetDate = today.minusDays(1);
            if (targetDate.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                targetDate = targetDate.minusDays(2);
            } else if (targetDate.getDayOfWeek() == java.time.DayOfWeek.MONDAY) {
                targetDate = targetDate.minusDays(3);
            }
        }
        
        // Determine the target time
        java.time.LocalDateTime targetDateTime;
        if (today.equals(targetDate) && time.isAfter(java.time.LocalTime.of(9, 15)) && time.isBefore(java.time.LocalTime.of(15, 30))) {
            targetDateTime = java.time.LocalDateTime.of(targetDate, time);
        } else {
            targetDateTime = java.time.LocalDateTime.of(targetDate, java.time.LocalTime.of(15, 30));
        }
        
        return targetDateTime.atZone(java.time.ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli();
    }

    /**
     * Create a new builder for HistoricalDataRetriever
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}
