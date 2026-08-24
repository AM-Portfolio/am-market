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
import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import lombok.extern.slf4j.Slf4j;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.service.MarketDataService;
import com.am.marketdata.service.MarketHoursService;
import com.am.marketdata.service.calendar.MarketCalendarService;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of MarketDataFetchService
 */
@Slf4j
@Service
public class MarketDataFetchServiceImpl implements MarketDataFetchService {

    private final FlowLogger flowLogger;
    private final MarketDataService marketDataService;
    private final StockIndicesMarketDataService stockIndicesMarketDataService;
    private final InstrumentUtils instrumentUtils;
    private final MarketHoursService marketHoursService;
    private final MarketCalendarService marketCalendarService;

    public MarketDataFetchServiceImpl(FlowLogger flowLogger,
            MarketDataService marketDataService,
            StockIndicesMarketDataService stockIndicesMarketDataService,
            InstrumentUtils instrumentUtils,
            MarketHoursService marketHoursService,
            MarketCalendarService marketCalendarService) {
        this.flowLogger = flowLogger;
        this.marketDataService = marketDataService;
        this.stockIndicesMarketDataService = stockIndicesMarketDataService;
        this.instrumentUtils = instrumentUtils;
        this.marketHoursService = marketHoursService;
        this.marketCalendarService = marketCalendarService;
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

        if (tradingSymbols == null || tradingSymbols.isEmpty()) {
            return new HashMap<>();
        }

        try (FlowSpan span = flowLogger.start("market.fetch.quotes",
                "symbolsCount", tradingSymbols.size(), "isIndex", isIndexSymbol, "timeFrame", timeFrame.getApiValue(),
                "forceRefresh", forceRefresh)) {

            boolean fetchIndexStocks = !isIndexSymbol;
            Set<String> symbols = instrumentUtils.resolveSymbols(new ArrayList<>(tradingSymbols), fetchIndexStocks);
            // Fixed SLF4J pattern: was log.info("methodName", "msg") which failed to log params
            log.info("Resolved {} symbols from {} input symbols fetchIndexStocks={}", symbols.size(),
                    tradingSymbols.size(), fetchIndexStocks);

            Map<String, OHLCQuote> ohlcData = marketDataService.getOHLC(new ArrayList<>(symbols), timeFrame,
                    forceRefresh, null);
            if (ohlcData != null) {
                ohlcData = new HashMap<>(ohlcData);
                instrumentUtils.aliasQuotesUnderOriginalIsins(tradingSymbols, ohlcData);
            } else {
                ohlcData = new HashMap<>();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("quotes", ohlcData);
            response.put("count", ohlcData.size());
            response.put("cached", !forceRefresh);
            response.put("timestamp", System.currentTimeMillis());
            response.put("timeFrame", timeFrame.getApiValue());
            response.put("source", forceRefresh ? "provider" : "cache");

            flowLogger.complete(span, "resultCount", ohlcData.size());
            return response;
        }
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

        if (symbols == null || symbols.isEmpty()) {
            log.warn("No symbols provided for historical data request");
            return HistoricalDataResponseV1.builder()
                    .data(new HashMap<>())
                    .error("No symbols provided")
                    .build();
        }

        try (FlowSpan span = flowLogger.start("market.fetch.historical.batch",
                "symbolsCount", symbols.size(), "interval", interval.getApiValue(), "fetchIndexStocks",
                fetchIndexStocks, "forceRefresh", forceRefresh)) {
            try {
                Map<String, HistoricalData> symbolsData = new HashMap<>();

                Set<String> resolvedSymbols = instrumentUtils.resolveSymbols(new ArrayList<>(symbols), fetchIndexStocks);
                // Fixed SLF4J pattern: removed redundant methodName param
                log.info("Resolved {} symbols from {} input symbols", resolvedSymbols.size(), symbols.size());

                HistoricalDataFilterUtil.FilterParams filterParams = HistoricalDataFilterUtil
                        .extractFilterParams(additionalParams);

                // Partition symbols into stocks and indices to query with correct isIndexSymbol flags
                Set<String> indexSymbols = new java.util.HashSet<>();
                Set<String> stockSymbols = new java.util.HashSet<>();
                for (String sym : resolvedSymbols) {
                    if (sym.startsWith("NSE_EQ:")) {
                        stockSymbols.add(sym);
                    } else {
                        indexSymbols.add(sym);
                    }
                }

                Map<String, HistoricalData> batchResult = new java.util.HashMap<>();
                if (!indexSymbols.isEmpty()) {
                    batchResult.putAll(marketDataService.getHistoricalDataBatch(
                            new ArrayList<>(indexSymbols), fromDate, toDate, interval, false, additionalParams, null,
                            true, forceRefresh));
                }
                if (!stockSymbols.isEmpty()) {
                    batchResult.putAll(marketDataService.getHistoricalDataBatch(
                            new ArrayList<>(stockSymbols), fromDate, toDate, interval, false, additionalParams, null,
                            false, forceRefresh));
                }

                int successCount = 0;
                int totalDataPoints = 0;
                int totalFilteredDataPoints = 0;

                for (String symbol : symbols) {
                    HistoricalData historicalData = batchResult.get(symbol);
                    if (historicalData != null && historicalData.getDataPoints() != null
                            && !historicalData.getDataPoints().isEmpty()) {
                        List<OHLCVTPoint> dataPoints = historicalData.getDataPoints();
                        int originalCount = dataPoints.size();

                        if (filterParams.isFiltered()) {
                            // Fixed SLF4J pattern in helper classes might also have been updated
                            dataPoints = HistoricalDataFilterUtil.applyFilterStrategy(dataPoints, filterParams);
                        }

                        HistoricalData filteredHistoricalData = new HistoricalData();
                        filteredHistoricalData.setTradingSymbol(symbol);
                        filteredHistoricalData.setInterval(interval.getApiValue());
                        filteredHistoricalData.setDataPoints(dataPoints);
                        filteredHistoricalData.setDataPointCount(dataPoints.size());
                        filteredHistoricalData.setExchange(historicalData.getExchange());
                        filteredHistoricalData.setCurrency(historicalData.getCurrency());
                        filteredHistoricalData.setIsin(historicalData.getIsin());
                        filteredHistoricalData.setFromDate(historicalData.getFromDate());
                        filteredHistoricalData.setToDate(historicalData.getToDate());
                        filteredHistoricalData.setRetrievalTime(historicalData.getRetrievalTime());

                        symbolsData.put(symbol, filteredHistoricalData);
                        successCount++;
                        totalDataPoints += originalCount;
                        totalFilteredDataPoints += dataPoints.size();
                    }
                }

                HistoricalDataMetadata metadata = HistoricalDataMetadata.builder()
                        .fromDate(new SimpleDateFormat("yyyy-MM-dd").format(fromDate))
                        .toDate(new SimpleDateFormat("yyyy-MM-dd").format(toDate))
                        .interval(interval.getApiValue())
                        .intervalEnum(interval.name())
                        .totalSymbols(symbols.size())
                        .successfulSymbols(successCount)
                        .totalDataPoints(totalDataPoints)
                        .filteredDataPoints(filterParams.isFiltered() ? totalFilteredDataPoints : totalDataPoints)
                        .filtered(filterParams.isFiltered())
                        .filterType(filterParams.getFilterType())
                        .filterFrequency(filterParams.isFiltered() ? filterParams.getFilterFrequency() : null)
                        .processingTimeMs(span.elapsedMillis())
                        .source(forceRefresh ? "provider" : "cache")
                        .build();

                flowLogger.complete(span, "successfulSymbols", successCount, "totalPoints", totalFilteredDataPoints);
                return HistoricalDataResponseV1.builder()
                        .data(symbolsData)
                        .metadata(metadata)
                        .build();
            } catch (Exception e) {
                // Fixed SLF4J pattern: was log.error("methodName", "msg", e)
                log.error("Error in batch historical data retrieval", e);
                flowLogger.fail(span, e);
                return HistoricalDataResponseV1.builder()
                        .error("Failed to retrieve batch historical data")
                        .message(e.getMessage())
                        .build();
            }
        }
    }

    @Override
    public Map<String, Object> getOptionChain(String underlyingSymbol, Date expiryDate, boolean forceRefresh) {
        log.debug(
                "Fetching option chain for symbol={} expiryDate={}", underlyingSymbol, expiryDate);
        // Option chain functionality not yet migrated to MarketDataService
        Map<String, Object> result = new HashMap<>();
        result.put("error", "Option chain not yet supported");
        return result;
    }

    @Override
    public Map<String, Object> getMutualFundDetails(String schemeCode, boolean forceRefresh) {
        log.debug("Fetching mutual fund details for schemeCode={}", schemeCode);
        // Mutual fund functionality not yet migrated to MarketDataService
        Map<String, Object> result = new HashMap<>();
        result.put("error", "Mutual fund details not yet supported");
        return result;
    }

    @Override
    public Map<String, Object> getMutualFundNavHistory(String schemeCode, Date from, Date to, boolean forceRefresh) {
        log.debug(
                "Fetching mutual fund NAV history for schemeCode={} from={} to={}", schemeCode, from, to);
        // Mutual fund functionality not yet migrated to MarketDataService
        Map<String, Object> result = new HashMap<>();
        result.put("error", "Mutual fund NAV history not yet supported");
        return result;
    }

    @Override
    public HistoricalDataResponseV1 processHistoricalDataRequest(
            HistoricalDataRequest request) throws Exception {
        log.info(
                "[INTERVAL_TRACE] Controller → Service: Processing historical data request symbols={} from={} to={} interval={} (enum={}, apiValue={}) filterType={} isIndexSymbol={}",
                request.getSymbols(), request.getFrom(), request.getTo(),
                request.getInterval(),
                request.getInterval().name(),
                request.getInterval().getApiValue(),
                request.getFilterType(),
                request.isIndexSymbol());

        // Resolve symbols - DON'T expand if isIndexSymbol is true
        // isIndexSymbol=true means we want the index itself, not its constituents
        // isIndexSymbol=false means expand indices to constituent stocks
        Set<String> symbolList;
        if (request.isIndexSymbol()) {
            log.info("[INTERVAL_TRACE] isIndexSymbol=true, returning index symbols as-is: {}",
                    request.getSymbols());
            symbolList = parseSymbols(request.getSymbols());
            // Pass expandIndices=false to keep index symbols as-is
            symbolList = instrumentUtils.resolveSymbols(new ArrayList<>(symbolList), false);
            log.info("[INTERVAL_TRACE] Kept {} index symbols without expansion",
                    symbolList.size());
        } else {
            log.info("[INTERVAL_TRACE] isIndexSymbol=false, expanding indices to constituent stocks");
            Set<String> parsedSymbols = parseSymbols(request.getSymbols());
            // Pass expandIndices=true to expand indices to constituent stocks
            symbolList = instrumentUtils.resolveSymbols(new ArrayList<>(parsedSymbols), true);
            log.info("[INTERVAL_TRACE] Expanded {} symbols to {} stocks",
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
                log.info("[INTERVAL_TRACE] 'to' date not provided, using current date: {}",
                        dateFormat.format(toDate));
            } else {
                toDate = dateFormat.parse(request.getTo());
                // [Inclusive toDate Fix]
                // SimpleDateFormat.parse("yyyy-MM-dd") returns midnight (00:00:00.000) of that day.
                // This excludes all data points generated during the market hours of that day.
                // We adjust toDate to the end of the day (23:59:59.999) to make the query fully inclusive of the target day.
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(toDate);
                cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
                cal.set(java.util.Calendar.MINUTE, 59);
                cal.set(java.util.Calendar.SECOND, 59);
                cal.set(java.util.Calendar.MILLISECOND, 999);
                toDate = cal.getTime();
            }
        } catch (ParseException e) {
            return HistoricalDataResponseV1.builder()
                    .error("Invalid date format")
                    .message("Use yyyy-MM-dd format for dates")
                    .build();
        }

        // Adjust query dates to the last active trading session if they fall on weekends or holidays.
        // This prevents querying empty holiday/weekend ranges and avoids Upstox API rejections.
        java.time.ZoneId kolkataZone = java.time.ZoneId.of("Asia/Kolkata");
        java.time.LocalDate localTo = toDate.toInstant().atZone(kolkataZone).toLocalDate();
        java.time.LocalDate localFrom = fromDate.toInstant().atZone(kolkataZone).toLocalDate();

        // Roll to-date back to the last active trading day if currently closed
        localTo = adjustToLastTradingDay(localTo);

        // Ensure from-date is not after the adjusted to-date (collapses 1D queries correctly)
        if (localFrom.isAfter(localTo)) {
            localFrom = localTo;
        }

        // Convert back to java.util.Date, preserving end-of-day precision for toDate
        toDate = java.util.Date.from(localTo.atTime(23, 59, 59, 999).atZone(kolkataZone).toInstant());
        fromDate = java.util.Date.from(localFrom.atStartOfDay(kolkataZone).toInstant());

        log.info("[HOLIDAY_ROLLBACK] Adjusted query range: {} to {}", fromDate, toDate);

        Map<String, Object> additionalParams = request.getAdditionalParams();
        if (additionalParams == null) {
            additionalParams = new HashMap<>();
        }
        additionalParams.put("filterType", request.getFilterType());
        additionalParams.put("filterFrequency", request.getFilterFrequency());

        log.info(
                "[INTERVAL_TRACE] Service → getHistoricalDataMultipleSymbols: Calling with interval: {} (apiValue: {})",
                request.getInterval(), request.getInterval().getApiValue());

        HistoricalDataResponseV1 response = getHistoricalDataMultipleSymbols(
                symbolList, fromDate, toDate, request.getInterval(),
                request.getInstrumentType(),
                additionalParams, request.isForceRefresh(), !request.isIndexSymbol()); // fetchIndexStocks =
                                                                                       // !isIndexSymbol

        log.info(
                "[INTERVAL_TRACE] Service → Controller: Returning response for interval: {}",
                response.getMetadata() != null ? response.getMetadata().getInterval() : "unknown");

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

        // Fallback for Index Symbols when Market is Closed
        // We only use this MongoDB fallback if we are NOT performing a force refresh.
        // If forceRefresh is true, we want to bypass this block and contact the Upstox API
        // directly to fetch the fresh, accurate End-of-Day (EOD) prices.
        if (isIndexSymbol && !marketHoursService.isMarketOpen() && !forceRefresh) {
            log.info("Market is closed. Fetching OHLC for index symbols from MongoDB fallback: {}", symbols);
            Map<String, OHLCQuote> fallbackData = new HashMap<>();
            List<com.am.common.investment.model.stockindice.StockIndicesMarketData> indexDocs = stockIndicesMarketDataService.findByIndexSymbols(symbols);
            if (indexDocs != null) {
                for (com.am.common.investment.model.stockindice.StockIndicesMarketData indexData : indexDocs) {
                    if (indexData != null && indexData.getIndexSymbol() != null && indexData.getMetadata() != null) {
                        String symbol = indexData.getIndexSymbol();
                        var meta = indexData.getMetadata();
                        fallbackData.put(symbol, OHLCQuote.builder()
                            .lastPrice(meta.getLast())
                            .previousClose(meta.getPreviousClose())
                            .ohlc(OHLCQuote.OHLC.builder()
                                .open(meta.getOpen())
                                .high(meta.getHigh())
                                .low(meta.getLow())
                                .close(meta.getLast())
                                .build())
                            .build());
                    }
                }
            }
            if (!fallbackData.isEmpty()) {
                return fallbackData;
            }
        }

        // Resolve symbols using InstrumentUtils
        // isIndexSymbol=true means keep as-is (fetchIndexStocks=false)
        // isIndexSymbol=false means expand indices (fetchIndexStocks=true)
        boolean fetchIndexStocks = !isIndexSymbol;
        Set<String> requested = new HashSet<>(symbols);
        symbols = instrumentUtils.resolveSymbols(new ArrayList<>(symbols), fetchIndexStocks);

        Map<String, OHLCQuote> ohlcData = marketDataService.getOHLC(new ArrayList<>(symbols), timeFrame, forceRefresh,
                null);

        if (ohlcData != null) {
            ohlcData = new HashMap<>(ohlcData);
            instrumentUtils.aliasQuotesUnderOriginalIsins(requested, ohlcData);
            log.info("Fetched OHLC data for keys: {}", ohlcData.keySet());
            return ohlcData;
        } else {
            log.warn("Fetched OHLC data is null");
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
        log.debug("Finding symbols not included in the passed list: {}", symbolsToCheck);

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
        log.info("Fetching historical charts for symbol={} range={}",
                symbol, range);

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
            log.error("Error fetching historical charts for {}: {}", symbol, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch chart data");
            errorResponse.put("message", e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Recursively rolls back a date to the nearest preceding active trading day (non-weekend, non-holiday).
     */
    private java.time.LocalDate adjustToLastTradingDay(java.time.LocalDate date) {
        java.time.LocalDate current = date;
        while (!isTradingDay(current)) {
            current = current.minusDays(1);
        }
        return current;
    }

    /**
     * Checks if a specific date is a valid trading day using the market calendar database (includes mid-week holidays).
     */
    private boolean isTradingDay(java.time.LocalDate date) {
        try {
            var timings = marketCalendarService.getTimings("NSE", date);
            return timings != null && timings.open();
        } catch (Exception e) {
            // Fallback: simple day-of-week check if calendar db lookup fails
            java.time.DayOfWeek dow = date.getDayOfWeek();
            return dow != java.time.DayOfWeek.SATURDAY && dow != java.time.DayOfWeek.SUNDAY;
        }
    }
}

