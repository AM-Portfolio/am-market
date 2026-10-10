package com.am.marketdata.analysis.util;

import com.am.common.investment.model.stockindice.StockData;
import com.am.marketdata.common.log.AppLogger;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
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
            // A positive LTP without its comparison base cannot produce a trustworthy
            // ranking. Equality is valid and correctly represents a 0.0% move.
            return lastPrice != null && Double.isFinite(lastPrice) && lastPrice > 0.0
                    && previousClose != null && Double.isFinite(previousClose) && previousClose > 0.0
                    && percentChange != null && Double.isFinite(percentChange);
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
        Set<String> knownIndices = new HashSet<>(Arrays.asList(
            "INDIA VIX", "NIFTY 50", "NIFTY NEXT 50", "NIFTY 100", "NIFTY 200", 
            "NIFTY 500", "NIFTY MIDCAP 50", "NIFTY MIDCAP 100", "NIFTY SMLCAP 100", 
            "NIFTY MIDCAP 150", "NIFTY SMLCAP 50", "NIFTY SMLCAP 250", "NIFTY SMLCAP 500", 
            "NIFTY BANK", "NIFTY AUTO", "NIFTY FMCG", "NIFTY MEDIA", "NIFTY METAL", 
            "NIFTY PHARMA", "NIFTY PSU BANK", "NIFTY PVT BANK", "NIFTY REALTY", 
            "NIFTY HEALTHCARE", "NIFTY CONSR DURABLE", "NIFTY OIL AND GAS", 
            "NIFTY MIDSML HLTH", "NIFTY IT", "NIFTY FIN SERVICES", "NIFTY ENERGY", 
            "NIFTY PHARMACEUTICALS", "SENSEX", "NIFTY", "BANKNIFTY"
        ));
        List<String> symbols = stockDataList.stream()
                .filter(sd -> sd != null && sd.getSymbol() != null)
                .map(StockData::getSymbol)
                .filter(symbol -> {
                    String clean = symbol.replace("NSE:", "").replace("NSE_EQ:", "").trim().toUpperCase();
                    return !knownIndices.contains(clean) && 
                           !clean.startsWith("NIFTY ") && 
                           !clean.contains("VIX") && 
                           !clean.startsWith("SENSEX") && 
                           !clean.equals("FINNIFTY") &&
                           !clean.equals("MIDCPNIFTY");
                }) // Exclude index symbols
                .collect(Collectors.toList());

        if (symbols.isEmpty()) {
            log.warn("enrichWithPrices", "No valid symbols found in stock data list");
            return Collections.emptyList();
        }

        // Fetch prices for all symbols (with optional time frame and expansion control)
        Map<String, OHLCQuote> priceData = fetchLivePrices(symbols, timeFrame, expandIndices);

        // Normalize price data keys to include both prefixed and base symbol variants (e.g., NSE_EQ:RELIANCE & RELIANCE)
        Map<String, OHLCQuote> normalizedPriceData = new HashMap<>();
        for (Map.Entry<String, OHLCQuote> entry : priceData.entrySet()) {
            String key = entry.getKey();
            normalizedPriceData.put(key, entry.getValue());
            if (key.contains(":")) {
                normalizedPriceData.put(key.substring(key.indexOf(":") + 1), entry.getValue());
            }
        }

        log.info("enrichWithPrices", "Normalized price data keys count: " + normalizedPriceData.size());

        // Enrich each StockData with price information
        List<EnrichedStockData> enrichedList = stockDataList.stream()
                .filter(sd -> sd != null && sd.getSymbol() != null)
                .map(sd -> {
                    String sym = sd.getSymbol();
                    String cleanSym = sym.contains(":") ? sym.substring(sym.indexOf(":") + 1) : sym;
                    OHLCQuote quote = normalizedPriceData.get(sym);
                    if (quote == null) {
                        quote = normalizedPriceData.get(cleanSym);
                    }
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

            /*
             * Movers receives known constituent stocks, not an index query. Keep their
             * identity as the bare trading ticker so it matches Redis/Influx history
             * keys (for example RELIANCE). Converting it to NSE_EQ:RELIANCE here made
             * the Movers path miss data that the Heatmap path could already read.
             */
            List<String> canonicalSymbols = symbols.stream()
                    .map(this::canonicalTradingSymbol)
                    .filter(symbol -> !symbol.isBlank())
                    .distinct()
                    .collect(Collectors.toList());
            if (canonicalSymbols.isEmpty()) {
                return Collections.emptyMap();
            }

            // Use Smart Service to get quotes (Cache -> DB -> History Fallback)
            Map<String, OHLCQuote> prices = smartStockService.getSmartQuotes(canonicalSymbols,
                    timeFrame);

            if (prices == null) {
                prices = new HashMap<>();
            }

            return prices;
        } catch (Exception e) {
            log.error("fetchLivePrices", "Error fetching prices", e);
            return Collections.emptyMap();
        }
    }

    private String canonicalTradingSymbol(String symbol) {
        String normalized = symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
        while (normalized.contains(":")) {
            String prefix = normalized.substring(0, normalized.indexOf(':'));
            if (!Set.of("NSE", "BSE", "NSE_EQ", "BSE_EQ").contains(prefix)) {
                break;
            }
            normalized = normalized.substring(normalized.indexOf(':') + 1).trim();
        }
        return normalized;
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
