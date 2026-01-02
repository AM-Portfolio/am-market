package com.am.marketdata.api.service.impl;

import com.am.common.investment.model.historical.HistoricalData;
import com.am.common.investment.model.historical.OHLCVTPoint;
import com.am.common.investment.model.stockindice.StockIndicesMarketData;
import com.am.common.investment.service.StockIndicesMarketDataService;
import com.am.marketdata.api.dto.HistoricalDataRequest;
import com.am.marketdata.api.model.HistoricalDataMetadata;
import com.am.marketdata.api.model.HistoricalDataResponseV1;
import com.am.marketdata.api.service.MarketDataFetchService;
import com.am.marketdata.api.util.InstrumentUtils;
import com.am.marketdata.api.util.HistoricalDataFilterUtil;
import com.am.marketdata.common.log.AppLogger;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.service.MarketDataService;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of MarketDataFetchService
 */
@Service
public class MarketDataFetchServiceImpl implements MarketDataFetchService {

    private final AppLogger log = AppLogger.getLogger();

    private final MarketDataService marketDataService;
    private final StockIndicesMarketDataService stockIndicesMarketDataService;
    private final InstrumentUtils instrumentUtils;

    public MarketDataFetchServiceImpl(MarketDataService marketDataService,
            StockIndicesMarketDataService stockIndicesMarketDataService,
            InstrumentUtils instrumentUtils) {
        this.marketDataService = marketDataService;
        this.stockIndicesMarketDataService = stockIndicesMarketDataService;
        this.instrumentUtils = instrumentUtils;
    }

    @Override
    public Map<String, Map<String, Object>> getQuotes(Set<String> tradingSymbols, boolean forceRefresh) {
        // Use marketDataService.getOHLC with DAY timeframe for quotes
        Map<String, OHLCQuote> ohlcData = marketDataService.getOHLC(
                new ArrayList<>(tradingSymbols), TimeFrame.DAY, forceRefresh, null);

        // Convert to expected format
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (Map.Entry<String, OHLCQuote> entry : ohlcData.entrySet()) {
            Map<String, Object> quoteData = new HashMap<>();
            OHLCQuote quote = entry.getValue();
            quoteData.put("lastPrice", quote.getLastPrice());
            if (quote.getOhlc() != null) {
                quoteData.put("open", quote.getOhlc().getOpen());
                quoteData.put("high", quote.getOhlc().getHigh());
                quoteData.put("low", quote.getOhlc().getLow());
                quoteData.put("close", quote.getOhlc().getClose());
            }
            result.put(entry.getKey(), quoteData);
        }
        return result;
    }

    @Override
    public Map<String, Object> getQuotes(Set<String> tradingSymbols, boolean isIndexSymbol, TimeFrame timeFrame,
            boolean forceRefresh) {
        String methodName = "getQuotes";
        log.info(methodName,
                String.format("Getting quotes for %d symbols with timeFrame: %s, isIndexSymbol: %b, forceRefresh: %b",
                        tradingSymbols.size(), timeFrame.getApiValue(), isIndexSymbol, forceRefresh));

        // Resolve symbols using InstrumentUtils
        // isIndexSymbol=true means keep as-is (fetchIndexStocks=false)
        // isIndexSymbol=false means expand indices (fetchIndexStocks=true)
        boolean fetchIndexStocks = !isIndexSymbol;
        Set<String> symbols = instrumentUtils.resolveSymbols(new ArrayList<>(tradingSymbols), fetchIndexStocks);

        // Get OHLC data with timeframe support (pass null provider)
        Map<String, OHLCQuote> ohlcData = marketDataService.getOHLC(new ArrayList<>(symbols), timeFrame, forceRefresh,
                null);

        // Create response with cache status
        Map<String, Object> response = new HashMap<>();
        response.put("quotes", ohlcData);
        response.put("count", ohlcData.size());
        response.put("cached", !forceRefresh);
        response.put("timestamp", System.currentTimeMillis());
        response.put("timeFrame", timeFrame.getApiValue());
        response.put("source", forceRefresh ? "provider" : "cache");

        return response;
    }

    @Override
    public Map<String, Object> getLivePrices(Set<String> symbols, boolean indexSymbol, boolean forceRefresh) {
        // Resolve symbols using InstrumentUtils
        // indexSymbol=true means keep as-is (fetchIndexStocks=false)
        // indexSymbol=false means expand indices (fetchIndexStocks=true)
        boolean fetchIndexStocks = !indexSymbol;
        Set<String> symbolsSet = instrumentUtils.resolveSymbols(new ArrayList<>(symbols), fetchIndexStocks);

        List<com.am.common.investment.model.equity.EquityPrice> prices = marketDataService.getLivePrices(
                new ArrayList<>(symbolsSet), null, forceRefresh);

        Map<String, Object> result = new HashMap<>();
        result.put("prices", prices);
        result.put("count", prices.size());
        return result;
    }

    @Override
    public HistoricalDataResponseV1 getHistoricalDataMultipleSymbols(Set<String> symbols,
            Date fromDate, Date toDate,
            TimeFrame interval, String instrumentType,
            Map<String, Object> additionalParams, boolean forceRefresh, boolean fetchIndexStocks) {
        String methodName = "getHistoricalDataMultipleSymbols";
        log.info(methodName, String.format(
                "[BATCH_HISTORICAL] getHistoricalDataMultipleSymbols: Processing historical data request for %d symbols from %s to %s, interval: %s (apiValue: %s), fetchIndexStocks: %b",
                symbols.size(), fromDate, toDate, interval, interval.getApiValue(), fetchIndexStocks));

        Map<String, HistoricalData> symbolsData = new HashMap<>();

        if (symbols == null || symbols.isEmpty()) {
            log.warn(methodName, "No symbols provided for historical data request");
            return HistoricalDataResponseV1.builder()
                    .data(new HashMap<>())
                    .error("No symbols provided")
                    .build();
        }

        // Resolve symbols using InstrumentUtils based on fetchIndexStocks flag
        Set<String> resolvedSymbols = instrumentUtils.resolveSymbols(new ArrayList<>(symbols), fetchIndexStocks);

        HistoricalDataFilterUtil.FilterParams filterParams = HistoricalDataFilterUtil
                .extractFilterParams(additionalParams);
        long startTime = System.currentTimeMillis();

        try {
            // Use batch retrieval
            log.info(methodName, String.format(
                    "[BATCH_HISTORICAL] Calling marketDataService.getHistoricalDataBatch for %d symbols",
                    resolvedSymbols.size()));

            boolean isIndexSymbol = !fetchIndexStocks; // If we're not fetching stocks, treat as index symbols
            if (additionalParams != null && additionalParams.containsKey("isIndexSymbol")) {
                Object paramValue = additionalParams.get("isIndexSymbol");
                if (paramValue instanceof Boolean) {
                    isIndexSymbol = (Boolean) paramValue;
                } else if (paramValue instanceof String) {
                    isIndexSymbol = Boolean.parseBoolean((String) paramValue);
                }
            }

            Map<String, HistoricalData> batchResult = marketDataService.getHistoricalDataBatch(
                    new ArrayList<>(resolvedSymbols), fromDate, toDate, interval, false, additionalParams, null,
                    isIndexSymbol,
                    forceRefresh);

            int successCount = 0;
            int totalDataPoints = 0;
            int totalFilteredDataPoints = 0;

            // Process batch results
            for (String symbol : symbols) {
                HistoricalData historicalData = batchResult.get(symbol);

                if (historicalData != null && historicalData.getDataPoints() != null
                        && !historicalData.getDataPoints().isEmpty()) {
                    List<OHLCVTPoint> dataPoints = historicalData.getDataPoints();
                    int originalCount = dataPoints.size();

                    // Apply filtering if requested
                    if (filterParams.isFiltered()) {
                        dataPoints = HistoricalDataFilterUtil.applyFilterStrategy(dataPoints, filterParams);
                    }

                    // Create new HistoricalData object with potentially filtered points
                    HistoricalData filteredHistoricalData = new HistoricalData();
                    filteredHistoricalData.setTradingSymbol(symbol);
                    filteredHistoricalData.setInterval(interval.getApiValue());
                    filteredHistoricalData.setDataPoints(dataPoints);

                    symbolsData.put(symbol, filteredHistoricalData);

                    successCount++;
                    totalDataPoints += originalCount;
                    totalFilteredDataPoints += dataPoints.size();
                } else {
                    // Even if no data, we might want to put an empty entry or skip
                    // For now, skipping empty results in the map to reduce noise
                }
            }

            if (!filterParams.isFiltered()) {
                totalFilteredDataPoints = totalDataPoints;
            }

            long endTime = System.currentTimeMillis();

            // Populate metadata
            HistoricalDataMetadata metadata = HistoricalDataMetadata
                    .builder()
                    .fromDate(new SimpleDateFormat("yyyy-MM-dd").format(fromDate))
                    .toDate(new SimpleDateFormat("yyyy-MM-dd").format(toDate))
                    .interval(interval.getApiValue())
                    .intervalEnum(interval.name())
                    .totalSymbols(symbols.size())
                    .successfulSymbols(successCount)
                    .totalDataPoints(totalDataPoints)
                    .filteredDataPoints(totalFilteredDataPoints)
                    .filtered(filterParams.isFiltered())
                    .filterType(filterParams.getFilterType())
                    .filterFrequency(filterParams.isFiltered() ? filterParams.getFilterFrequency() : null)
                    .processingTimeMs(endTime - startTime)
                    .source(forceRefresh ? "provider" : "cache")
                    .build();

            return HistoricalDataResponseV1.builder()
                    .data(symbolsData)
                    .metadata(metadata)
                    .build();

        } catch (Exception e) {
            log.error(methodName, "Error in batch historical data retrieval: " + e.getMessage(), e);
            return HistoricalDataResponseV1.builder()
                    .error("Failed to retrieve batch historical data")
                    .message(e.getMessage())
                    .build();
        }
    }

    @Override
    public Map<String, Object> getOptionChain(String underlyingSymbol, Date expiryDate, boolean forceRefresh) {
        log.debug("getOptionChain",
                "Fetching option chain for symbol: " + underlyingSymbol + " with expiry date: " + expiryDate);
        // Option chain functionality not yet migrated to MarketDataService
        Map<String, Object> result = new HashMap<>();
        result.put("error", "Option chain not yet supported");
        return result;
    }

    @Override
    public Map<String, Object> getMutualFundDetails(String schemeCode, boolean forceRefresh) {
        log.debug("getMutualFundDetails", "Fetching mutual fund details for scheme code: " + schemeCode);
        // Mutual fund functionality not yet migrated to MarketDataService
        Map<String, Object> result = new HashMap<>();
        result.put("error", "Mutual fund details not yet supported");
        return result;
    }

    @Override
    public Map<String, Object> getMutualFundNavHistory(String schemeCode, Date from, Date to, boolean forceRefresh) {
        log.debug("getMutualFundNavHistory",
                "Fetching mutual fund NAV history for scheme code: " + schemeCode + " from: " + from + " to: " + to);
        // Mutual fund functionality not yet migrated to MarketDataService
        Map<String, Object> result = new HashMap<>();
        result.put("error", "Mutual fund NAV history not yet supported");
        return result;
    }

    @Override
    public HistoricalDataResponseV1 processHistoricalDataRequest(
            HistoricalDataRequest request) throws Exception {
        String methodName = "processHistoricalDataRequest";
        log.info(methodName, String.format(
                "[INTERVAL_TRACE] Controller → Service: Processing historical data request for symbols: %s from %s to %s, interval: %s (enum: %s, apiValue: %s), filterType: %s, isIndexSymbol: %b",
                request.getSymbols(), request.getFrom(), request.getTo(),
                request.getInterval(),
                request.getInterval().name(),
                request.getInterval().getApiValue(),
                request.getFilterType(),
                request.isIndexSymbol()));

        // Resolve symbols - DON'T expand if isIndexSymbol is true
        // isIndexSymbol=true means we want the index itself, not its constituents
        // isIndexSymbol=false means expand indices to constituent stocks
        Set<String> symbolList;
        if (request.isIndexSymbol()) {
            log.info(methodName, "[INTERVAL_TRACE] isIndexSymbol=true, returning index symbols as-is: {}",
                    request.getSymbols());
            symbolList = parseSymbols(request.getSymbols());
            // Pass expandIndices=false to keep index symbols as-is
            symbolList = instrumentUtils.resolveSymbols(new ArrayList<>(symbolList), false);
            log.info(methodName, "[INTERVAL_TRACE] Kept {} index symbols without expansion",
                    symbolList.size());
        } else {
            log.info(methodName, "[INTERVAL_TRACE] isIndexSymbol=false, expanding indices to constituent stocks");
            Set<String> parsedSymbols = parseSymbols(request.getSymbols());
            // Pass expandIndices=true to expand indices to constituent stocks
            symbolList = instrumentUtils.resolveSymbols(new ArrayList<>(parsedSymbols), true);
            log.info(methodName, "[INTERVAL_TRACE] Expanded {} symbols to {} stocks",
                    parsedSymbols.size(), symbolList.size());
        }

        if (symbolList.isEmpty()) {
            return HistoricalDataResponseV1.builder()
                    .error("No valid symbols provided")
                    .message("Please provide at least one valid symbol")
                    .build();
        }

        Date fromDate;
        Date toDate;
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            fromDate = dateFormat.parse(request.getFrom());

            // If 'to' date is not provided, use current date
            if (request.getTo() == null || request.getTo().trim().isEmpty()) {
                toDate = new Date(); // Current date
                log.info(methodName, "[INTERVAL_TRACE] 'to' date not provided, using current date: {}",
                        dateFormat.format(toDate));
            } else {
                toDate = dateFormat.parse(request.getTo());
            }
        } catch (ParseException e) {
            return HistoricalDataResponseV1.builder()
                    .error("Invalid date format")
                    .message("Use yyyy-MM-dd format for dates")
                    .build();
        }

        Map<String, Object> additionalParams = request.getAdditionalParams();
        if (additionalParams == null) {
            additionalParams = new HashMap<>();
        }
        additionalParams.put("filterType", request.getFilterType());
        additionalParams.put("filterFrequency", request.getFilterFrequency());

        log.info(methodName, String.format(
                "[INTERVAL_TRACE] Service → getHistoricalDataMultipleSymbols: Calling with interval: %s (apiValue: %s)",
                request.getInterval(), request.getInterval().getApiValue()));

        HistoricalDataResponseV1 response = getHistoricalDataMultipleSymbols(
                symbolList, fromDate, toDate, request.getInterval(),
                request.getInstrumentType(),
                additionalParams, request.isForceRefresh(), !request.isIndexSymbol()); // fetchIndexStocks =
                                                                                       // !isIndexSymbol

        log.info(methodName, String.format(
                "[INTERVAL_TRACE] Service → Controller: Returning response for interval: %s",
                response.getMetadata() != null ? response.getMetadata().getInterval() : "unknown"));

        return response;
    }

    private Set<String> parseSymbols(String symbolsString) {
        if (symbolsString == null || symbolsString.trim().isEmpty()) {
            return new HashSet<>();
        }

        return Arrays.stream(symbolsString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    public Map<String, OHLCQuote> getOHLC(Set<String> symbols, boolean isIndexSymbol, TimeFrame timeFrame,
            boolean forceRefresh) {

        // Resolve symbols using InstrumentUtils
        // isIndexSymbol=true means keep as-is (fetchIndexStocks=false)
        // isIndexSymbol=false means expand indices (fetchIndexStocks=true)
        boolean fetchIndexStocks = !isIndexSymbol;
        symbols = instrumentUtils.resolveSymbols(new ArrayList<>(symbols), fetchIndexStocks);

        Map<String, OHLCQuote> ohlcData = marketDataService.getOHLC(new ArrayList<>(symbols), timeFrame, forceRefresh,
                null);

        if (ohlcData != null) {
            log.info("getOHLC", "Fetched OHLC data for keys: " + ohlcData.keySet());
            return ohlcData;
        } else {
            log.warn("getOHLC", "Fetched OHLC data is null");
            return new HashMap<>();
        }
    }

    @Override
    public StockIndicesMarketData getStockIndexData(String indexSymbol, boolean forceRefresh) {
        return stockIndicesMarketDataService.findByIndexSymbol(indexSymbol);
    }

    @Override
    public Set<StockIndicesMarketData> getStockIndicesData(Set<String> indexSymbols, boolean forceRefresh) {
        Set<StockIndicesMarketData> indicesData = indexSymbols.stream()
                .map(symbol -> stockIndicesMarketDataService.findByIndexSymbol(symbol))
                .filter(data -> data != null)
                .collect(Collectors.toSet());

        return indicesData;
    }

    public List<String> findMissingSymbols(List<String> indexSymbols, List<String> symbolsToCheck) {
        log.debug("findMissingSymbols", "Finding symbols not included in the passed list: " + symbolsToCheck);

        if (symbolsToCheck == null || symbolsToCheck.isEmpty()) {
            return Collections.emptyList();
        }

        Set<StockIndicesMarketData> indicesData = getStockIndicesData(new HashSet<>(indexSymbols), false);

        if (indicesData == null || indicesData.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> symbolsSet = new HashSet<>(symbolsToCheck);

        List<String> missingSymbols = indicesData.stream()
                .filter(data -> data != null && data.getData() != null)
                .flatMap(data -> data.getData().stream())
                .filter(stockData -> stockData != null && stockData.getSymbol() != null)
                .map(stockData -> stockData.getSymbol())
                .distinct()
                .filter(symbol -> !symbolsSet.contains(symbol))
                .collect(Collectors.toList());

        return missingSymbols;
    }

    public Map<String, Object> getHistoricalChartsData(String symbol, String range) {
        String methodName = "getHistoricalChartsData";
        log.info(methodName, String.format("Fetching historical charts for symbol: %s, range: %s",
                symbol, range));

        String interval;
        java.time.LocalDate to = java.time.LocalDate.now();
        java.time.LocalDate from;

        if ("5Y".equalsIgnoreCase(range)) {
            interval = "month"; // Monthly
            from = to.minusYears(5);
        } else {
            // Default to 1Y
            interval = "day"; // Daily
            from = to.minusYears(1);
        }

        // Construct HistoricalDataRequest
        HistoricalDataRequest request = new HistoricalDataRequest();
        request.setSymbols(symbol); // Expects String, not List
        request.setFrom(from.toString());
        request.setTo(to.toString());
        request.setInterval(TimeFrame.fromApiValue(interval)); // Convert string to TimeFrame
        request.setFilterType("price");

        try {
            HistoricalDataResponseV1 response = processHistoricalDataRequest(request);
            // Convert DTO to Map for this specific endpoint (keeping legacy support or
            // refactor later)
            // Ideally getHistoricalChartsData should also return typed object but interface
            // says Map<String, Object>
            Map<String, Object> result = new HashMap<>();
            if (response.getData() != null) {
                result.putAll(response.getData());
            }
            return result;
        } catch (Exception e) {
            log.error(methodName, "Error fetching historical charts for " + symbol + ": " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch chart data");
            errorResponse.put("message", e.getMessage());
            return errorResponse;
        }
    }
}
