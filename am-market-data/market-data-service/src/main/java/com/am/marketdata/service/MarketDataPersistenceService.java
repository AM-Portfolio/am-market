package com.am.marketdata.service;

import com.am.common.investment.model.equity.EquityPrice;
import com.am.common.investment.model.historical.HistoricalData;
import com.am.common.investment.service.EquityService;
import com.am.common.investment.service.historical.HistoricalDataService;
import com.am.marketdata.common.mapper.OHLCMapper;
import com.am.marketdata.service.MarketDataPersistenceService;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Implementation of MarketDataPersistenceService
 * Handles saving and retrieving market data from both database and cache
 */
@Slf4j
@Service
public class MarketDataPersistenceService implements com.am.marketdata.common.service.MarketDataIngestionService {

    private final HistoricalDataService historicalDataService;
    private final MarketDataCacheService marketDataCacheService;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final EquityService equityService;
    private final OHLCMapper ohlcMapper;

    public MarketDataPersistenceService(
            HistoricalDataService historicalDataService,
            MarketDataCacheService marketDataCacheService,
            EquityService equityService,
            OHLCMapper ohlcMapper,
            @Qualifier("marketDataPersistenceExecutor") ThreadPoolTaskExecutor taskExecutor) {
        this.historicalDataService = historicalDataService;
        this.marketDataCacheService = marketDataCacheService;
        this.equityService = equityService;
        this.ohlcMapper = ohlcMapper;
        this.taskExecutor = taskExecutor;
    }

    /**
     * Get the market data cache service
     * 
     * @return The market data cache service
     */
    public MarketDataCacheService getMarketDataCacheService() {
        return marketDataCacheService;
    }

    public CompletableFuture<Void> saveOHLCData(Map<String, OHLCQuote> ohlcData) {
        if (ohlcData == null || ohlcData.isEmpty()) {
            log.warn("No OHLC data to save");
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                // First save to database using EquityService
                log.debug("Saving {} OHLC data points to database", ohlcData.size());
                List<EquityPrice> equityPrices = ohlcMapper.toEquityPriceList(ohlcData);
                equityService.saveAllPrices(equityPrices);
                log.debug("Successfully saved {} equity prices to database", equityPrices.size());

                // Then update the cache with default timeframe (1D for current day data)
                marketDataCacheService.cacheOHLCData(ohlcData, TimeFrame.DAY);
                log.debug("Successfully cached OHLC data for {} symbols", ohlcData.size());
            } catch (Exception e) {
                log.error("Error saving OHLC data: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to save OHLC data", e);
            }
        }, taskExecutor);
    }

    public CompletableFuture<Void> saveHistoricalData(String symbol, TimeFrame interval,
            HistoricalData historicalData) {
        if (historicalData == null || historicalData.getDataPoints() == null
                || historicalData.getDataPoints().isEmpty()) {
            log.warn("No historical data to save for symbol: {}", symbol);
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                // First save to database
                log.debug("Saving historical data to database for symbol: {}", symbol);
                historicalDataService.saveHistoricalData(historicalData);
                log.debug("Successfully saved historical data for symbol: {}", symbol);

                // Then update the cache
                marketDataCacheService.cacheHistoricalData(symbol, interval, historicalData);
                log.debug("Successfully cached historical data for symbol: {}", symbol);
            } catch (Exception e) {
                log.error("Error saving historical data for symbol {}: {}", symbol, e.getMessage(), e);
                throw new RuntimeException("Failed to save historical data", e);
            }
        }, taskExecutor);
    }

    public Map<String, OHLCQuote> getOHLCData(List<String> tradingSymbols, TimeFrame timeFrame, boolean forceRefresh) {
        if (tradingSymbols == null || tradingSymbols.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            // Filter out index symbols early to prevent false cache misses
            List<String> knownIndices = Arrays.asList("NIFTY 50", "NIFTY BANK", "SENSEX", "NIFTY", "BANKNIFTY");
            List<String> filteredSymbols = tradingSymbols.stream()
                    .filter(symbol -> {
                        String clean = symbol.replace("NSE:", "").replace("NSE_EQ:", "");
                        return !knownIndices.contains(clean);
                    })
                    .collect(Collectors.toList());

            if (filteredSymbols.isEmpty()) {
                log.debug("All symbols were indices, returning empty result");
                return Collections.emptyMap();
            }

            // Use "LIVE" as the cache key for current/live prices (when timeFrame is null)
            String tfValue = timeFrame != null ? timeFrame.getApiValue() : "1D";

            Map<String, OHLCQuote> result = new HashMap<>();
            Set<String> remainingSymbols = new HashSet<>(filteredSymbols);

            if (!forceRefresh) {
                // Try cache first
                Map<String, OHLCQuote> cachedData = marketDataCacheService.getOHLCFromCache(tradingSymbols, timeFrame);
                if (cachedData != null && !cachedData.isEmpty()) {
                    log.debug("Retrieved {} OHLC data from cache with timeFrame {}",
                            cachedData.size(), tfValue);
                    result.putAll(cachedData);

                    // Remove found symbols
                    cachedData.keySet().forEach(key -> {
                        String symbol = key.replace("NSE:", "").replace("NSE_EQ:", "");
                        remainingSymbols.remove(symbol);
                    });

                    log.debug("{} symbols remaining after cache lookup for timeFrame {}",
                            remainingSymbols.size(), tfValue);

                    if (remainingSymbols.isEmpty()) {
                        return result;
                    }
                }
            }

            // If we have remaining symbols or forceRefresh is true, try database
            if (!remainingSymbols.isEmpty() || forceRefresh) {
                log.debug("{} OHLC data from database for {} symbols with timeFrame {}",
                        forceRefresh ? "Forcing refresh of" : "Fetching missing",
                        remainingSymbols.size(), tfValue);

                // Clean symbols (remove NSE: prefix if present)
                List<String> cleanSymbols = remainingSymbols.stream()
                        .map(symbol -> symbol.replace("NSE:", ""))
                        .collect(Collectors.toList());

                // Get equity prices from database
                List<EquityPrice> equityPrices = equityService.getPricesByTradingSymbols(cleanSymbols);

                if (!equityPrices.isEmpty()) {
                    // Convert equity prices to OHLCQuote format
                    for (EquityPrice price : equityPrices) {
                        if (price.getLastPrice() == null) {
                            continue;
                        }
                        OHLCQuote quote = createOHLCQuoteFromEquityPrice(price);
                        // String symbol = "NSE:" + price.getSymbol();
                        result.put(price.getSymbol(), quote);

                        // Remove found symbols from the remaining set
                        remainingSymbols.remove(price.getSymbol());
                    }

                    log.debug("Retrieved OHLC data from database for {} symbols with timeFrame {}",
                            equityPrices.size(), tfValue);
                }
            }

            return result;
        } catch (Exception e) {
            String tfValue = timeFrame != null ? timeFrame.getApiValue() : "LIVE";
            log.error("Error retrieving OHLC data with timeFrame {}: {}",
                    tfValue, e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Maps the interval format from API format to database format
     * 
     * @param apiInterval Interval in API format (e.g., "1d", "day", "1m", "minute")
     * @return Interval in database format
     */
    private String mapIntervalFormat(String apiInterval) {
        if (apiInterval == null || apiInterval.isEmpty()) {
            return "day"; // Default to daily interval
        }

        // Convert to lowercase for case-insensitive comparison
        String interval = apiInterval.toLowerCase();

        // Map common interval formats
        if (interval.contains("day") || interval.equals("1d") || interval.equals("d")) {
            return "day";
        } else if (interval.contains("week") || interval.equals("1w") || interval.equals("w")) {
            return "week";
        } else if (interval.contains("month") || interval.equals("1m") || interval.equals("m")) {
            return "month";
        } else if (interval.contains("minute") || interval.contains("min")) {
            // Extract number of minutes if specified (e.g., "5minute" -> "5minute")
            if (interval.matches("\\d+.*")) {
                String minutes = interval.replaceAll("[^\\d]", "");
                return minutes + "minute";
            }
            return "minute";
        } else if (interval.contains("hour") || interval.contains("hr") || interval.equals("1h")
                || interval.equals("h")) {
            return "hour";
        }

        // Return as is if no mapping found
        return interval;
    }

    /**
     * Creates an OHLCQuote object from an EquityPrice object
     * 
     * @param price EquityPrice object
     * @return OHLCQuote object
     */
    private OHLCQuote createOHLCQuoteFromEquityPrice(EquityPrice price) {
        OHLCQuote quote = new OHLCQuote();
        quote.setLastPrice(price.getLastPrice());

        OHLCQuote.OHLC ohlc = new OHLCQuote.OHLC();
        ohlc.setOpen(price.getOhlcv().getOpen());
        ohlc.setHigh(price.getOhlcv().getHigh());
        ohlc.setLow(price.getOhlcv().getLow());
        ohlc.setClose(price.getOhlcv().getClose());

        quote.setOhlc(ohlc);

        // Set additional fields if available
        // if (price.getVolume() != null) {
        // // Volume is not currently part of the OHLC model
        // }

        return quote;
    }

    public HistoricalData getHistoricalData(String symbol, TimeFrame interval, String fromDate, String toDate) {
        if (symbol == null || symbol.isEmpty() || interval == null) {
            return null;
        }

        try {
            // First try to get from cache
            HistoricalData cachedData = marketDataCacheService.getHistoricalDataFromCache(
                    symbol, interval, fromDate, toDate);

            if (cachedData != null && cachedData.getDataPoints() != null && !cachedData.getDataPoints().isEmpty()) {
                log.debug("Retrieved historical data from cache for symbol: {}", symbol);
                return cachedData;
            }

            // If not in cache, try to get from database
            log.debug("No historical data found in cache, fetching from database for symbol: {}", symbol);

            // Clean symbol (remove NSE: prefix if present)
            String cleanSymbol = symbol.replace("NSE:", "");

            // Parse dates - handle potential format variations
            LocalDate from;
            LocalDate to;
            try {
                from = LocalDate.parse(fromDate, DateTimeFormatter.ISO_LOCAL_DATE);
                to = LocalDate.parse(toDate, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception e) {
                // Try alternative format if ISO format fails
                DateTimeFormatter alternativeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                from = LocalDate.parse(fromDate, alternativeFormatter);
                to = LocalDate.parse(toDate, alternativeFormatter);
            }

            // Map interval to the format expected by the database service
            String mappedInterval = interval.name().toLowerCase();

            // Get historical data from database using HistoricalDataService
            // Convert LocalDate to Instant at the start of the day in UTC
            Instant fromInstant = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
            // For 'to' date, we want to include the entire day, so we move to the start of
            // the next day
            // This also prevents "empty range" errors in InfluxDB if from == to
            Instant toInstant = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

            // Handle Optional return type
            HistoricalData historicalData = historicalDataService.getHistoricalData(
                    cleanSymbol, fromInstant, toInstant, mappedInterval).orElse(null);

            if (historicalData != null && historicalData.getDataPoints() != null
                    && !historicalData.getDataPoints().isEmpty()) {
                log.debug("Retrieved historical data from database for symbol: {}", symbol);
                return historicalData;
            }

            log.debug("No historical data found in database for symbol: {}", symbol);
            return null;
        } catch (Exception e) {
            log.error("Error retrieving historical data for symbol {}: {}", symbol, e.getMessage(), e);
            return null;
        }
    }
}
