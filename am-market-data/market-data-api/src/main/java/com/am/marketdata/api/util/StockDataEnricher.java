package com.am.marketdata.api.util;

import com.am.common.investment.model.stockindice.StockData;
import com.am.marketdata.common.log.AppLogger;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.service.MarketDataService;
import com.am.marketdata.service.SmartStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper class to enrich StockData with live price information
 * Provides reusable logic for fetching prices and calculating metrics
 */
@Component
@RequiredArgsConstructor
public class StockDataEnricher {

    private final AppLogger log = AppLogger.getLogger();
    private final InstrumentUtils instrumentUtils;
    private final MarketDataService marketDataService;
    private final SmartStockService smartStockService;

    /**
     * Enriched stock data with price information
     */
    public static class EnrichedStockData {
        private final StockData stockData;
        private final Double lastPrice;
        private final Double change;
        private final Double percentChange;
        private final Double previousClose;

        public EnrichedStockData(StockData stockData, OHLCQuote quote) {
            this.stockData = stockData;

            if (quote != null) {
                double lp = quote.getLastPrice();
                double pc = quote.getPreviousClose();

                this.lastPrice = lp;
                this.previousClose = pc;

                // Always calculate change and percentChange since OHLCQuote doesn't provide
                // them
                if (pc != 0) {
                    this.change = lp - pc;
                    this.percentChange = (change / pc) * 100.0;
                } else {
                    this.change = null;
                    this.percentChange = null;
                }
            } else {
                this.lastPrice = null;
                this.change = null;
                this.percentChange = null;
                this.previousClose = null;
            }
        }

        public StockData getStockData() {
            return stockData;
        }

        public String getSymbol() {
            return stockData != null ? stockData.getSymbol() : null;
        }

        public Double getLastPrice() {
            return lastPrice;
        }

        public Double getChange() {
            return change;
        }

        public Double getPercentChange() {
            return percentChange != null ? percentChange : 0.0;
        }

        public Double getPreviousClose() {
            return previousClose;
        }

        public boolean hasValidPrice() {
            // Only require lastPrice to be non-null
            // Allow percentChange to be 0 or null (getter will return 0.0)
            return lastPrice != null;
        }
    }

    /**
     * Enrich a list of StockData with live price information
     * 
     * @param stockDataList List of StockData to enrich
     * @return List of EnrichedStockData with price information
     */
    public List<EnrichedStockData> enrichWithPrices(List<StockData> stockDataList) {
        return enrichWithPrices(stockDataList, TimeFrame.DAY, false);
    }

    /**
     * Enrich a list of StockData with price information for a specific time frame
     * 
     * @param stockDataList List of StockData to enrich
     * @param timeFrame     TimeFrame for price data (null for current/live prices)
     * @return List of EnrichedStockData with price information
     */
    public List<EnrichedStockData> enrichWithPrices(List<StockData> stockDataList,
            com.am.marketdata.common.model.TimeFrame timeFrame) {
        return enrichWithPrices(stockDataList, timeFrame, false);
    }

    /**
     * Enrich a list of StockData with price information for a specific time frame
     * 
     * @param stockDataList List of StockData to enrich
     * @param timeFrame     TimeFrame for price data (null for current/live prices)
     * @param expandIndices Whether to expand index symbols to constituent stocks
     * @return List of EnrichedStockData with price information
     */
    public List<EnrichedStockData> enrichWithPrices(List<StockData> stockDataList,
            com.am.marketdata.common.model.TimeFrame timeFrame, boolean expandIndices) {
        if (stockDataList == null || stockDataList.isEmpty()) {
            return Collections.emptyList();
        }

        // Extract symbols (exclude index symbols)
        List<String> knownIndices = Arrays.asList("NIFTY 50", "NIFTY BANK", "SENSEX", "NIFTY", "BANKNIFTY");
        List<String> symbols = stockDataList.stream()
                .filter(sd -> sd != null && sd.getSymbol() != null)
                .map(StockData::getSymbol)
                .filter(symbol -> !knownIndices.contains(symbol)) // Exclude index symbols
                .collect(Collectors.toList());

        if (symbols.isEmpty()) {
            log.warn("enrichWithPrices", "No valid symbols found in stock data list");
            return Collections.emptyList();
        }

        // Fetch prices for all symbols (with optional time frame and expansion control)
        Map<String, OHLCQuote> priceData = fetchLivePrices(symbols, timeFrame, expandIndices);

        // Normalize price data keys to remove exchange prefix (NSE_EQ:, NSE:, etc.)
        Map<String, OHLCQuote> normalizedPriceData = new HashMap<>();
        for (Map.Entry<String, OHLCQuote> entry : priceData.entrySet()) {
            String key = entry.getKey();
            // Remove exchange prefix if present (e.g., NSE_EQ:RELIANCE -> RELIANCE)
            String normalizedKey = key.contains(":") ? key.substring(key.indexOf(":") + 1) : key;
            normalizedPriceData.put(normalizedKey, entry.getValue());
        }

        log.info("enrichWithPrices", "Normalized price data keys: " + normalizedPriceData.keySet());

        // Enrich each StockData with price information
        List<EnrichedStockData> enrichedList = stockDataList.stream()
                .filter(sd -> sd != null && sd.getSymbol() != null)
                .map(sd -> {
                    OHLCQuote quote = normalizedPriceData.get(sd.getSymbol());
                    if (quote == null) {
                        log.warn("enrichWithPrices", "No price data found for symbol: " + sd.getSymbol());
                    }
                    return new EnrichedStockData(sd, quote);
                })
                .collect(Collectors.toList());

        log.info("enrichWithPrices", "Created " + enrichedList.size() + " enriched stock data objects");

        // Filter for valid prices
        List<EnrichedStockData> validData = enrichedList.stream()
                .filter(EnrichedStockData::hasValidPrice)
                .collect(Collectors.toList());

        log.info("enrichWithPrices", "Filtered to " + validData.size() + " stocks with valid prices (removed " +
                (enrichedList.size() - validData.size()) + " stocks)");

        return validData;
    }

    /**
     * Fetch prices for a list of symbols with optional time frame
     * 
     * @param symbols       List of symbols to fetch prices for
     * @param timeFrame     TimeFrame for price data (null for current/live)
     * @param expandIndices Whether to expand index symbols to constituent stocks
     * @return Map of symbol to OHLCQuote
     */
    private Map<String, OHLCQuote> fetchLivePrices(List<String> symbols,
            com.am.marketdata.common.model.TimeFrame timeFrame, boolean expandIndices) {
        try {
            String timeFrameStr = timeFrame != null ? timeFrame.getApiValue() : TimeFrame.DAY.getApiValue();

            // Use the expandIndices parameter to control symbol resolution
            Set<String> resolvedSymbols = instrumentUtils.resolveSymbols(symbols, expandIndices);
            if (resolvedSymbols.isEmpty()) {
                return Collections.emptyMap();
            }

            // Use Smart Service to get quotes (Cache -> DB -> History Fallback)
            Map<String, OHLCQuote> prices = smartStockService.getSmartQuotes(new ArrayList<>(resolvedSymbols));

            if (prices == null) {
                prices = new HashMap<>();
            }

            return prices;
        } catch (Exception e) {
            log.error("fetchLivePrices", "Error fetching prices", e);
            return Collections.emptyMap();
        }
    }

    /**
     * Sort enriched data by percentage change
     * 
     * @param enrichedData List to sort
     * @param descending   True for descending (gainers), false for ascending
     *                     (losers)
     * @return Sorted list
     */
    public List<EnrichedStockData> sortByPercentChange(List<EnrichedStockData> enrichedData, boolean descending) {
        Comparator<EnrichedStockData> comparator = Comparator.comparingDouble(EnrichedStockData::getPercentChange);

        if (descending) {
            comparator = comparator.reversed();
        }

        return enrichedData.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    /**
     * Group enriched data by a custom grouping function
     * 
     * @param enrichedData     List to group
     * @param groupingFunction Function to extract group key
     * @return Map of group key to list of EnrichedStockData
     */
    public Map<String, List<EnrichedStockData>> groupBy(
            List<EnrichedStockData> enrichedData,
            java.util.function.Function<EnrichedStockData, String> groupingFunction) {

        return enrichedData.stream()
                .filter(esd -> groupingFunction.apply(esd) != null)
                .collect(Collectors.groupingBy(groupingFunction));
    }

    /**
     * Calculate average percentage change for a group
     * 
     * @param group List of EnrichedStockData
     * @return Average percentage change
     */
    public double calculateAveragePercentChange(List<EnrichedStockData> group) {
        return group.stream()
                .mapToDouble(EnrichedStockData::getPercentChange)
                .average()
                .orElse(0.0);
    }
}
