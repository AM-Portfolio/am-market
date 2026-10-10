package com.am.marketdata.analysis.service;

// Trigger Push CI Action

import com.am.common.investment.model.stockindice.StockIndicesMarketData;
import com.am.common.investment.model.stockindice.StockData;
import com.am.marketdata.analysis.util.StockDataEnricher;
import com.am.marketdata.analysis.util.StockDataEnricher.EnrichedStockData;
import com.am.marketdata.common.log.AppLogger;
import com.am.marketdata.service.SecurityService;
import com.am.marketdata.api.service.StockIndicesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import com.am.marketdata.analysis.dto.StockMoverDTO;
import com.am.marketdata.analysis.dto.SectorPerformanceDTO;
import com.am.marketdata.service.model.security.SecurityDocument;

@Service
@RequiredArgsConstructor
public class MarketAnalyticsService {

    private final AppLogger log = AppLogger.getLogger();
    private final StockIndicesService stockIndicesService;
    private final SecurityService securityService;
    private final StockDataEnricher stockDataEnricher;
    private final com.am.marketdata.api.service.MarketDataFetchService marketDataFetchService;

    // Default broad index for "entire market" analytics
    private static final String DEFAULT_MARKET_INDEX = "NIFTY 50";

    /**
     * Get Historical Charts (Batch or Single)
     */
    public com.am.marketdata.api.model.HistoricalDataResponseV1 getHistoricalCharts(
            String symbols, String range, Boolean requestedIndexSymbol) {

        boolean isIndexSymbol = resolveChartSymbolType(symbols, requestedIndexSymbol);

        String interval = "1D";
        java.time.LocalDateTime to = java.time.LocalDateTime.now();
        java.time.LocalDateTime from = to.minusDays(1);

        // Determine Interval and From Time based on Range
        switch (range.toUpperCase()) {
            case "10M":
                interval = "1m";
                from = to.minusMinutes(10);
                break;
            case "15M":
                interval = "1m";
                from = to.minusMinutes(15);
                break;
            case "30M":
                interval = "1m";
                from = to.minusMinutes(30);
                break;
            case "1H":
                interval = "1m";
                from = to.minusHours(1);
                break;
            case "4H":
                interval = "5m";
                from = to.minusHours(4);
                break;
            case "1D":
                interval = "5m";
                from = to.minusDays(1);
                break;
            case "1W":
                // [Upstox Compatible Interval Mapping]
                // Upstox API accepts intervals: '1minute', '30minute', 'day', 'week', 'month'.
                // '1H' is not supported by Upstox v2/v3 and causes 0 data points.
                // Using '30m' provides rich 30-minute historical candle progression for the 1-week window across both Stocks & Indices.
                interval = "30m";
                from = to.minusWeeks(1);
                break;
            case "1M":
                interval = "1D";
                from = to.minusMonths(1);
                break;
            case "3M":
                interval = "1D";
                from = to.minusMonths(3);
                break;
            case "6M":
                interval = "1D";
                from = to.minusMonths(6);
                break;
            case "5Y":
                interval = "1W";
                from = to.minusYears(5);
                break;
            default: // Default 1Y
                interval = "1D";
                from = to.minusYears(1);
        }

        // Construct Request
        com.am.marketdata.api.dto.HistoricalDataRequest request = com.am.marketdata.api.dto.HistoricalDataRequest
                .builder()
                .symbols(symbols)
                .from(from.toLocalDate().toString())
                .to(to.toLocalDate().toString())
                .interval(com.am.marketdata.common.model.TimeFrame.fromApiValue(interval)) // Convert string to
                                                                                           // TimeFrame
                .filterType("price")
                .indexSymbol(isIndexSymbol)
                .build();

        // Fetch Data
        try {
            com.am.marketdata.api.model.HistoricalDataResponseV1 response = marketDataFetchService
                    .processHistoricalDataRequest(request);

            if (response.getError() != null) {
                return response;
            }

            // Filter data points by time range for each symbol
            if (response.getData() != null) {
                // [1D Timeframe Chart Optimization]
                // 1. Detect if today is a weekend or before market open. If so, roll back the query date target to the previous trading day.
                //    This prevents the chart from being empty when viewed on a Saturday, Sunday, or early morning before open.
                java.time.ZonedDateTime istNow = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
                java.time.DayOfWeek day = istNow.getDayOfWeek();
                java.time.LocalDate targetDate = istNow.toLocalDate();
                java.time.LocalTime time = istNow.toLocalTime();
                
                boolean isBeforeMarketOpen = time.isBefore(java.time.LocalTime.of(9, 15));
                if (day == java.time.DayOfWeek.SATURDAY) {
                    targetDate = targetDate.minusDays(1);
                } else if (day == java.time.DayOfWeek.SUNDAY) {
                    targetDate = targetDate.minusDays(2);
                } else if (isBeforeMarketOpen) {
                    // It's a weekday, but before market opens. Roll back to the previous trading day.
                    targetDate = targetDate.minusDays(1);
                    if (targetDate.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                        targetDate = targetDate.minusDays(2);
                    }
                }

                // 2. Bound the filter to the starting bell of the active market session (9:15 AM IST).
                //    By using 9:15 AM today/Friday/yesterday instead of "now - 24 hours" (which points to yesterday afternoon),
                //    we preserve the complete intraday chart progression rather than stripping out all daily candles.
                java.time.LocalDateTime filterFrom = "1D".equalsIgnoreCase(range)
                        ? targetDate.atTime(9, 15)
                        : from;
                java.time.ZoneId kolkataZone = java.time.ZoneId.of("Asia/Kolkata");
                long minTime = filterFrom.atZone(kolkataZone).toInstant().toEpochMilli();

                response.getData().forEach((s, symbolData) -> {
                    if (symbolData != null && symbolData.getDataPoints() != null) {
                        List<com.am.common.investment.model.historical.OHLCVTPoint> filteredPoints = symbolData
                                .getDataPoints().stream()
                                .filter(p -> {
                                    try {
                                        // Performance Optimization: Use pre-cached kolkataZone instance instead of calling ZoneId.of() inside stream loop
                                        long timestamp = p.getTime().atZone(kolkataZone)
                                                .toInstant()
                                                .toEpochMilli();
                                        return timestamp >= minTime;
                                    } catch (Exception e) {
                                        return true;
                                    }
                                })
                                .collect(Collectors.toList());

                        if (!filteredPoints.isEmpty()) {
                            symbolData.setDataPoints(filteredPoints);
                        }
                    }
                });
            }
            return response;
        } catch (Exception e) {
            log.error("getHistoricalCharts", "Error fetching historical charts: " + e.getMessage());
            return com.am.marketdata.api.model.HistoricalDataResponseV1.builder()
                    .error("Failed to fetch chart data")
                    .message(e.getMessage())
                    .build();
        }
    }

    /**
     * Chooses the chart retrieval path only when an older client omitted the flag.
     *
     * <p>The legacy endpoint defaulted every request to an index. A request for a
     * normal share such as TCS could therefore take the index path and return no
     * candles in one environment while appearing to work in another. Explicit
     * client values still win, so existing index callers keep their current
     * behaviour.</p>
     */
    private boolean resolveChartSymbolType(String symbols, Boolean requestedIndexSymbol) {
        if (requestedIndexSymbol != null) {
            return requestedIndexSymbol;
        }

        Set<String> requestedSymbols = splitChartSymbols(symbols).stream()
                .map(this::removeExchangePrefix)
                .filter(symbol -> !symbol.isBlank())
                .collect(Collectors.toSet());
        if (requestedSymbols.isEmpty()) {
            // Preserve the legacy default for invalid input. The normal request
            // validation below returns the existing empty/error response shape.
            return true;
        }

        try {
            Set<String> securitySymbols = securityService.findBySymbols(new ArrayList<>(requestedSymbols)).stream()
                    .map(SecurityDocument::getKey)
                    .filter(Objects::nonNull)
                    .map(SecurityDocument.SecurityKey::getSymbol)
                    .filter(Objects::nonNull)
                    .map(symbol -> symbol.trim().toUpperCase(Locale.ROOT))
                    .collect(Collectors.toSet());

            if (securitySymbols.containsAll(requestedSymbols)) {
                log.info("resolveChartSymbolType",
                        "Auto-resolved equity chart request for " + requestedSymbols.size() + " symbol(s)");
                return false;
            }

            boolean allIndices = requestedSymbols.stream()
                    .allMatch(stockIndicesService::hasStoredIndexSymbol);
            if (allIndices) {
                log.info("resolveChartSymbolType",
                        "Auto-resolved index chart request for " + requestedSymbols.size() + " symbol(s)");
                return true;
            }

            // A batch cannot safely mix index and equity semantics behind one
            // boolean. Keep the historical default until a caller sends the flag.
            log.warn("resolveChartSymbolType",
                    "Could not auto-resolve one chart type for " + requestedSymbols.size()
                            + " symbol(s); using legacy index path");
        } catch (Exception e) {
            // Resolution is an improvement for legacy callers, not a reason to
            // turn a previously accepted request into an HTTP failure.
            log.warn("resolveChartSymbolType",
                    "Chart type lookup failed; using legacy index path: " + e.getMessage());
        }

        return true;
    }

    private String removeExchangePrefix(String symbol) {
        String normalized = symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
        int separator = normalized.lastIndexOf(':');
        return separator >= 0 ? normalized.substring(separator + 1).trim() : normalized;
    }

    private Set<String> splitChartSymbols(String symbols) {
        if (symbols == null || symbols.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(symbols.split(","))
                .map(String::trim)
                .filter(symbol -> !symbol.isEmpty())
                .collect(Collectors.toSet());
    }

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
    public List<StockMoverDTO> getMovers(int limit, String type, String indexSymbol,
            com.am.marketdata.common.model.TimeFrame timeFrame, boolean expandIndices) {
        // Use provided index or default
        String targetIndex = indexSymbol != null && !indexSymbol.isEmpty() ? indexSymbol : DEFAULT_MARKET_INDEX;

        // Fetch enriched data
        List<EnrichedStockData> enrichedData = fetchEnrichedData(targetIndex, timeFrame, expandIndices, true);

        if (enrichedData.isEmpty()) {
            return Collections.emptyList();
        }

        // Sort by percentage change
        boolean descending = "gainers".equalsIgnoreCase(type);
        List<EnrichedStockData> filteredData = enrichedData.stream()
                .filter(data -> data.getChange() != null && Double.isFinite(data.getChange())
                        && (descending ? data.getChange() > 0.0 : data.getChange() < 0.0))
                .collect(Collectors.toList());
        List<EnrichedStockData> sortedData = stockDataEnricher.sortByPercentChange(filteredData, descending);

        // Convert to response format and limit results
        return sortedData.stream()
                .limit(limit)
                .map(this::enrichedDataToMap)
                .collect(Collectors.toList());
    }

    /**
     * Get Top Gainers AND Losers (Unified)
     */
    public Map<String, List<StockMoverDTO>> getMoversUnified(int limit, String indexSymbol,
            com.am.marketdata.common.model.TimeFrame timeFrame, boolean expandIndices) {
        String targetIndex = indexSymbol != null && !indexSymbol.isEmpty() ? indexSymbol : DEFAULT_MARKET_INDEX;
        /*
         * Read prices once, then sort the same snapshot in both directions.
         * This avoids two slow provider calls and mismatched gainers/losers.
         */
        List<EnrichedStockData> enrichedData = fetchEnrichedData(targetIndex, timeFrame, expandIndices, true);

        // Flat stocks are not movers. Keeping zero-change rows in both sorted lists
        // made the same unchanged stocks appear as both gainers and losers.
        List<EnrichedStockData> gainersData = enrichedData.stream()
                .filter(data -> data.getChange() != null && Double.isFinite(data.getChange()) && data.getChange() > 0.0)
                .collect(Collectors.toList());
        List<EnrichedStockData> losersData = enrichedData.stream()
                .filter(data -> data.getChange() != null && Double.isFinite(data.getChange()) && data.getChange() < 0.0)
                .collect(Collectors.toList());

        List<StockMoverDTO> gainers = stockDataEnricher
                .sortByPercentChange(gainersData, true)
                .stream()
                .limit(limit)
                .map(this::enrichedDataToMap)
                .collect(Collectors.toList());
        List<StockMoverDTO> losers = stockDataEnricher
                .sortByPercentChange(losersData, false)
                .stream()
                .limit(limit)
                .map(this::enrichedDataToMap)
                .collect(Collectors.toList());

        Map<String, List<StockMoverDTO>> result = new HashMap<>();
        result.put("gainers", gainers);
        result.put("losers", losers);
        return result;
    }

    private List<EnrichedStockData> fetchEnrichedData(String targetIndex,
            com.am.marketdata.common.model.TimeFrame timeFrame, boolean expandIndices) {
        return fetchEnrichedData(targetIndex, timeFrame, expandIndices, false);
    }

    private List<EnrichedStockData> fetchEnrichedData(String targetIndex,
            com.am.marketdata.common.model.TimeFrame timeFrame, boolean expandIndices, boolean requireFullCoverage) {
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

        if (requireFullCoverage) {
            long expectedMembers = indexData.getData().stream()
                    .filter(Objects::nonNull)
                    .map(StockData::getSymbol)
                    .filter(symbol -> symbol != null && !symbol.isBlank())
                    .map(String::trim)
                    .distinct()
                    .count();
            int minimumMembers = minimumVerifiedMemberCount(targetIndex);
            if (expectedMembers == 0 || expectedMembers < minimumMembers || enrichedData.size() != expectedMembers) {
                log.warn("getMovers",
                        "Incomplete quote coverage for index {}: validPrices={}, members={}, minimumMembers={}; returning no ranking",
                        targetIndex, enrichedData.size(), expectedMembers, minimumMembers);
                return Collections.emptyList();
            }
        }

        if (enrichedData.isEmpty()) {
            log.warn("getMovers", "No price data available for index: " + targetIndex);
            return Collections.emptyList();
        }

        return enrichedData;
    }

    /** Mirrors the scraper's existing minimums so a truncated Mongo roster is not treated as complete. */
    private int minimumVerifiedMemberCount(String indexName) {
        return com.am.marketdata.common.util.IndexMembershipUtils.minimumMembershipSize(indexName);
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
    public List<SectorPerformanceDTO> getSectorPerformance(String indexSymbol,
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
        List<SectorPerformanceDTO> sectorPerformance = new ArrayList<>();

        bySector.forEach((sector, stocks) -> {
            double avgChange = stockDataEnricher.calculateAveragePercentChange(stocks);

            sectorPerformance.add(SectorPerformanceDTO.builder()
                    .sector(sector)
                    .change(avgChange)
                    .stockCount(stocks.size())
                    .status(avgChange >= 0 ? "Positive" : "Negative")
                    .build());
        });

        // Sort by Performance Descending and limit to top 15
        sectorPerformance.sort((a, b) -> Double.compare(b.getChange(), a.getChange()));

        return sectorPerformance.stream().limit(15).collect(Collectors.toList());
    }

    /**
     * Get Index Performance (All Constituents)
     * Returns performance data for all stocks in the index for the given timeframe.
     */
    public List<StockMoverDTO> getIndexPerformance(String indexSymbol,
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
    private StockMoverDTO enrichedDataToMap(EnrichedStockData data) {
        return StockMoverDTO.builder()
                .symbol(data.getSymbol())
                .lastPrice(data.getLastPrice())
                .change(data.getChange())
                .pChange(data.getPercentChange())
                .percentChange(data.getPercentChange())
                .previousClose(data.getPreviousClose())
                .build();
    }
}
