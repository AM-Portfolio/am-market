package com.am.marketdata.api.service;

import com.am.common.investment.model.stockindice.StockIndicesMarketData;
import com.am.common.investment.model.stockindice.StockData;
import com.am.marketdata.api.util.StockDataEnricher;
import com.am.marketdata.api.util.StockDataEnricher.EnrichedStockData;
import com.am.marketdata.common.log.AppLogger;
import com.am.marketdata.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketAnalyticsService {

    private final AppLogger log = AppLogger.getLogger();
    private final StockIndicesService stockIndicesService;
    private final SecurityService securityService;
    private final StockDataEnricher stockDataEnricher;

    // Default broad index for "entire market" analytics
    private static final String DEFAULT_MARKET_INDEX = "NIFTY 50";

    /**
     * Get Top Gainers or Losers
     * 
     * @param limit         Number of records
     * @param type          "gainers" or "losers"
     * @param indexSymbol   Index to use for filtering (e.g., "NIFTY 50", "NIFTY
     *                      500")
     * @param timeFrame     Time frame for price data (e.g., 1D, 1W, 1M)
     * @param expandIndices Whether to expand index symbols to constituent stocks
     * @return List of enriched stock data sorted by percentage change
     */
    public List<Map<String, Object>> getMovers(int limit, String type, String indexSymbol,
            com.am.marketdata.common.model.TimeFrame timeFrame, boolean expandIndices) {
        // Use provided index or default
        String targetIndex = indexSymbol != null && !indexSymbol.isEmpty() ? indexSymbol : DEFAULT_MARKET_INDEX;

        // Fetch enriched data
        List<EnrichedStockData> enrichedData = fetchEnrichedData(targetIndex, timeFrame, expandIndices);

        if (enrichedData.isEmpty()) {
            return Collections.emptyList();
        }

        // Sort by percentage change
        boolean descending = "gainers".equalsIgnoreCase(type);
        List<EnrichedStockData> sortedData = stockDataEnricher.sortByPercentChange(enrichedData, descending);

        // Convert to response format and limit results
        return sortedData.stream()
                .limit(limit)
                .map(this::enrichedDataToMap)
                .collect(Collectors.toList());
    }

    /**
     * Get Top Gainers AND Losers (Unified)
     */
    public Map<String, List<Map<String, Object>>> getMoversUnified(int limit, String indexSymbol,
            com.am.marketdata.common.model.TimeFrame timeFrame, boolean expandIndices) {

        String targetIndex = indexSymbol != null && !indexSymbol.isEmpty() ? indexSymbol : DEFAULT_MARKET_INDEX;

        // 1. Fetch data ONCE
        List<EnrichedStockData> enrichedData = fetchEnrichedData(targetIndex, timeFrame, expandIndices);

        if (enrichedData.isEmpty()) {
            return Map.of("gainers", Collections.emptyList(), "losers", Collections.emptyList());
        }

        // 2. Sort for Gainers (Descending)
        List<EnrichedStockData> allSorted = stockDataEnricher.sortByPercentChange(enrichedData, true);

        // 3. Extract Top Gainers (Strictly Positive)
        List<Map<String, Object>> gainers = allSorted.stream()
                .filter(d -> d.getPercentChange() > 0)
                .limit(limit)
                .map(this::enrichedDataToMap)
                .collect(Collectors.toList());

        // 4. Extract Top Losers (Reverse of the descending list is ascending)
        // Or re-sort ascending
        List<EnrichedStockData> allSortedAsc = stockDataEnricher.sortByPercentChange(enrichedData, false);

        List<Map<String, Object>> losers = allSortedAsc.stream()
                .filter(d -> d.getPercentChange() < 0)
                .limit(limit)
                .map(this::enrichedDataToMap)
                .collect(Collectors.toList());

        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        result.put("gainers", gainers);
        result.put("losers", losers);

        return result;
    }

    private List<EnrichedStockData> fetchEnrichedData(String targetIndex,
            com.am.marketdata.common.model.TimeFrame timeFrame, boolean expandIndices) {
        // Fetch index constituent data
        StockIndicesMarketData indexData = stockIndicesService.getLatestIndexData(targetIndex);

        if (indexData == null || indexData.getData() == null) {
            log.warn("getMovers", "Market index data not found: " + targetIndex);
            return Collections.emptyList();
        }

        // Enrich stock data with live prices, passing expandIndices parameter
        List<EnrichedStockData> enrichedData = stockDataEnricher.enrichWithPrices(
                indexData.getData(),
                timeFrame != null ? timeFrame : com.am.marketdata.common.model.TimeFrame.DAY,
                expandIndices);

        if (enrichedData.isEmpty()) {
            log.warn("getMovers", "No price data available for index: " + targetIndex);
            return Collections.emptyList();
        }

        return enrichedData;
    }

    /**
     * Get Sector Performance
     * Aggregates performance of stocks grouped by their Industry (Sector)
     * 
     * @param indexSymbol   Index to use for filtering (e.g., "NIFTY 50", "NIFTY
     *                      500")
     * @param timeFrame     Time frame for price data (e.g., 1D, 1W, 1M)
     * @param expandIndices Whether to expand index symbols to constituent stocks
     * @return List of sector performance data
     */
    public List<Map<String, Object>> getSectorPerformance(String indexSymbol,
            com.am.marketdata.common.model.TimeFrame timeFrame, boolean expandIndices) {
        // Use provided index or default
        String targetIndex = indexSymbol != null && !indexSymbol.isEmpty() ? indexSymbol : DEFAULT_MARKET_INDEX;

        // Fetch index constituent data
        StockIndicesMarketData indexData = stockIndicesService.getLatestIndexData(targetIndex);

        if (indexData == null || indexData.getData() == null) {
            log.warn("getSectorPerformance", "Market index data not found: " + targetIndex);
            return Collections.emptyList();
        }

        // Enrich stock data with live prices, passing expandIndices parameter
        List<EnrichedStockData> enrichedData = stockDataEnricher.enrichWithPrices(
                indexData.getData(),
                timeFrame != null ? timeFrame : com.am.marketdata.common.model.TimeFrame.DAY,
                expandIndices);

        if (enrichedData.isEmpty()) {
            log.warn("getSectorPerformance", "No price data available for index: " + targetIndex);
            return Collections.emptyList();
        }

        // Fetch security details (sectors) for all symbols
        List<String> symbols = enrichedData.stream()
                .map(EnrichedStockData::getSymbol)
                .collect(Collectors.toList());

        Map<String, String> symbolToSector = securityService.getSymbolToSectorMap(symbols);

        // Group by sector
        Map<String, List<EnrichedStockData>> bySector = stockDataEnricher.groupBy(
                enrichedData,
                esd -> symbolToSector.getOrDefault(esd.getSymbol(), "Unknown"));

        // Calculate sector performance
        List<Map<String, Object>> sectorPerformance = new ArrayList<>();

        bySector.forEach((sector, stocks) -> {
            double avgChange = stockDataEnricher.calculateAveragePercentChange(stocks);

            Map<String, Object> sectorData = new HashMap<>();
            sectorData.put("sector", sector);
            sectorData.put("change", avgChange);
            sectorData.put("stockCount", stocks.size());
            sectorData.put("status", avgChange >= 0 ? "Positive" : "Negative");

            sectorPerformance.add(sectorData);
        });

        // Sort by Performance Descending and limit to top 15
        sectorPerformance.sort((a, b) -> Double.compare((Double) b.get("change"), (Double) a.get("change")));

        return sectorPerformance.stream().limit(15).collect(Collectors.toList());
    }

    /**
     * Get Index Performance (All Constituents)
     * Returns performance data for all stocks in the index for the given timeframe.
     */
    public List<Map<String, Object>> getIndexPerformance(String indexSymbol,
            com.am.marketdata.common.model.TimeFrame timeFrame) {
        String targetIndex = indexSymbol != null && !indexSymbol.isEmpty() ? indexSymbol : DEFAULT_MARKET_INDEX;

        // Fetch enriched data for ALL constituents (expandIndices=true)
        List<EnrichedStockData> enrichedData = fetchEnrichedData(targetIndex, timeFrame, true);

        if (enrichedData.isEmpty()) {
            return Collections.emptyList();
        }

        // Return all data mapped to response format (optionally sorted by pChange desc)
        return enrichedData.stream()
                .sorted((a, b) -> Double.compare(b.getPercentChange(), a.getPercentChange()))
                .map(this::enrichedDataToMap)
                .collect(Collectors.toList());
    }

    /**
     * Convert EnrichedStockData to Map for API response
     */
    private Map<String, Object> enrichedDataToMap(EnrichedStockData data) {
        Map<String, Object> map = new HashMap<>();
        map.put("symbol", data.getSymbol());
        map.put("lastPrice", data.getLastPrice());
        map.put("change", data.getChange());
        map.put("pChange", data.getPercentChange());
        map.put("previousClose", data.getPreviousClose());
        return map;
    }
}
