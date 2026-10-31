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
import com.am.marketdata.common.util.MarketDepthFreshness;
import com.am.marketdata.service.MarketDataService;
import com.am.marketdata.service.MarketHoursService;
import com.am.marketdata.service.calendar.MarketCalendarService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

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

    @Value("${market.data.depth.max.age.seconds:60}")
    private long marketDepthMaxAgeSeconds = 60;

    @Autowired
    public MarketDataFetchServiceImpl(
            java.util.Optional<FlowLogger> flowLogger,
            MarketDataService marketDataService,
            StockIndicesMarketDataService stockIndicesMarketDataService,
            InstrumentUtils instrumentUtils,
            MarketHoursService marketHoursService,
            MarketCalendarService marketCalendarService) {
        this.flowLogger = flowLogger != null ? flowLogger.orElse(null) : null;
        this.marketDataService = marketDataService;
        this.stockIndicesMarketDataService = stockIndicesMarketDataService;
        this.instrumentUtils = instrumentUtils;
        this.marketHoursService = marketHoursService;
        this.marketCalendarService = marketCalendarService;
    }

    public MarketDataFetchServiceImpl(
            FlowLogger flowLogger,
            MarketDataService marketDataService,
            StockIndicesMarketDataService stockIndicesMarketDataService,
            InstrumentUtils instrumentUtils,
            MarketHoursService marketHoursService,
            MarketCalendarService marketCalendarService) {
        this(java.util.Optional.ofNullable(flowLogger), marketDataService, stockIndicesMarketDataService, instrumentUtils, marketHoursService, marketCalendarService);
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

        try (FlowSpan span = flowLogger != null ? flowLogger.start("market.fetch.quotes",
                "symbolsCount", tradingSymbols.size(), "isIndex", isIndexSymbol, "timeFrame", timeFrame.getApiValue(),
                "forceRefresh", forceRefresh) : null) {

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

                // KEY DUP FIX: Ensure the final map contains ONLY the exact symbol keys requested by the caller.
                // If caller requested "NSE:INFY", return key "NSE:INFY" and filter out the unrequested base key "INFY".
                // This prevents returning 10 entries for 5 requested symbols.
                Map<String, OHLCQuote> sanitizedQuotes = new HashMap<>();
                for (String requestedSym : tradingSymbols) {
                    if (ohlcData.containsKey(requestedSym)) {
                        OHLCQuote quote = ohlcData.get(requestedSym);
                        if (quote != null && quote.getLastPrice() > 0.0) {
                            sanitizedQuotes.put(requestedSym, quote);
                        } else {
                            log.warn("Skipping unusable quote symbol={} lastPrice={}", requestedSym,
                                    quote != null ? quote.getLastPrice() : null);
                        }
                    } else {
                        // Fallback lookup: match clean symbol if requested symbol had exchange prefix or vice versa
                        String cleanReq = requestedSym.contains(":") ? requestedSym.substring(requestedSym.indexOf(":") + 1).trim() : requestedSym;
                        if (ohlcData.containsKey(cleanReq)) {
                            OHLCQuote quote = ohlcData.get(cleanReq);
                            if (quote != null && quote.getLastPrice() > 0.0) {
                                sanitizedQuotes.put(requestedSym, quote);
                            } else {
                                log.warn("Skipping unusable quote symbol={} lastPrice={}", requestedSym,
                                        quote != null ? quote.getLastPrice() : null);
                            }
                        }
                    }
                }
                ohlcData = sanitizedQuotes;
                removeStaleMarketDepth(ohlcData);
            } else {
                ohlcData = new HashMap<>();
            }

            final Map<String, OHLCQuote> returnedQuotes = ohlcData;
            Map<String, Object> response = new HashMap<>();
            response.put("quotes", returnedQuotes);
            response.put("count", returnedQuotes.size());
            response.put("cached", !forceRefresh);
            response.put("timestamp", System.currentTimeMillis());
            response.put("timeFrame", timeFrame.getApiValue());
            response.put("source", forceRefresh ? "provider" : "cache");
            response.put("unavailableSymbols", tradingSymbols.stream()
                    .filter(symbol -> !returnedQuotes.containsKey(symbol))
                    .toList());

            if (flowLogger != null && span != null) {
                flowLogger.complete(span, "resultCount", returnedQuotes.size());
            }
            return response;
        }
    }

    private void removeStaleMarketDepth(Map<String, OHLCQuote> quotes) {
        quotes.forEach((symbol, quote) -> {
            if (quote == null || quote.getMarketDepth() == null) return;
            long ageMillis = quote.getQuoteTimestamp() == null
                    ? Long.MAX_VALUE
                    : System.currentTimeMillis() - quote.getQuoteTimestamp().toEpochMilli();
            boolean freshTimestamp = MarketDepthFreshness.isFresh(quote.getQuoteTimestamp(),
                    java.util.concurrent.TimeUnit.SECONDS.toMillis(marketDepthMaxAgeSeconds));
            // A closed session may still return a fresh exchange snapshot. Keep it when its
            // provider timestamp is recent, and omit it once that snapshot ages out.
            if (!freshTimestamp || marketDepthMaxAgeSeconds <= 0) {
                quote.setMarketDepth(null);
                log.debug("Omitted stale market depth symbol={} ageMillis={}", symbol, ageMillis);
            }
        });
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
    public Map<String, Object> getLiveLTP(Set<String> symbols, String exchange, String timeframe,
            boolean indexSymbol, boolean forceRefresh) {
        TimeFrame tf;
        try {
            tf = TimeFrame.fromApiValue(timeframe);
        } catch (Exception e) {
            log.warn("Invalid timeframe: {} defaulting to 1D", timeframe);
            tf = TimeFrame.DAY;
        }

        // Retrieve full OHLCQuote objects via getQuotes rather than getLivePrices.
        // getQuotes populates the validated session baseline (previousClose), day OHLC,
        // and lastPrice from the provider/cache snapshot, ensuring /live-ltp returns
        // accurate non-zero daily change and changePercent for the active trading session.
        // The timeframe parameter describes the caller's chart selection. Day change
        // always needs the current session quote and previous-session close.
        Map<String, Object> quotesResponse = getQuotes(symbols, indexSymbol, TimeFrame.DAY, forceRefresh);

        Map<String, Map<String, Object>> result = new HashMap<>();
        Object quotesObj = quotesResponse.get("quotes");
        if (quotesObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, OHLCQuote> quotesMap = (Map<String, OHLCQuote>) quotesObj;

            for (Map.Entry<String, OHLCQuote> entry : quotesMap.entrySet()) {
                String fullKey = entry.getKey();
                OHLCQuote quote = entry.getValue();
                if (quote == null) {
                    continue;
                }

                Double currentPrice = quote.getLastPrice();
                if (currentPrice == null || currentPrice <= 0.0) {
                    log.warn("Skipping unusable live LTP symbol={} lastPrice={}", fullKey, currentPrice);
                    continue;
                }

                // Calculate daily change against validated session previous close:
                double previousClose = quote.getPreviousClose();
                boolean hasValidPreviousClose = Double.isFinite(previousClose) && previousClose > 0.0;
                double change = 0.0;
                double changePercent = 0.0;
                if (hasValidPreviousClose) {
                    change = Math.round((currentPrice - previousClose) * 100.0) / 100.0;
                    changePercent = Math.round((change / previousClose) * 10000.0) / 100.0;
                }

                String parsedExchange = exchange;
                String symbol = fullKey;
                if (fullKey.contains(":")) {
                    String[] parts = fullKey.split(":", 2);
                    parsedExchange = parts[0];
                    symbol = parts[1];
                }

                Map<String, Object> ltpData = new HashMap<>();
                ltpData.put("symbol", symbol);
                ltpData.put("exchange", parsedExchange);
                ltpData.put("lastPrice", currentPrice);
                ltpData.put("previousClose", previousClose);
                ltpData.put("change", change);
                ltpData.put("changePercent", changePercent);
                ltpData.put("timeframe", tf.getApiValue());

                String resultKey = (parsedExchange != null && !parsedExchange.isBlank())
                        ? parsedExchange + ":" + symbol
                        : symbol;
                result.put(resultKey, ltpData);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("count", result.size());
        response.put("timeframe", tf.getApiValue());
        response.put("data", result);
        response.put("timestamp", new Date());
        return response;
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

        try (FlowSpan span = flowLogger != null ? flowLogger.start("market.fetch.historical.batch",
                "symbolsCount", symbols.size(), "interval", interval.getApiValue(), "fetchIndexStocks",
                fetchIndexStocks, "forceRefresh", forceRefresh) : null) {
            try {
                Map<String, HistoricalData> symbolsData = new HashMap<>();

                Set<String> resolvedSymbols = instrumentUtils.resolveSymbols(new ArrayList<>(symbols), fetchIndexStocks);
                // Fixed SLF4J pattern: removed redundant methodName param
                log.info("Resolved {} symbols from {} input symbols", resolvedSymbols.size(), symbols.size());

                HistoricalDataFilterUtil.FilterParams filterParams = HistoricalDataFilterUtil
                        .extractFilterParams(additionalParams);

                // Symbol resolution can return plain names such as "NIFTY 50" without an
                // index prefix. Preserve the caller's semantic type so the correct Influx
                // measurement is selected instead of inferring it from the resolved string.
                boolean indexRequest = additionalParams != null
                        && Boolean.TRUE.equals(additionalParams.get("isIndexSymbol"));
                boolean indexInstrument = "INDEX".equalsIgnoreCase(instrumentType);
                boolean routeAsIndex = indexRequest || indexInstrument;
                log.info("Historical routing: indexRequest={} indexInstrument={} symbols={}",
                        indexRequest, indexInstrument, symbols.size());
                Set<String> resolvedIndexNames = routeAsIndex
                        ? Collections.emptySet()
                        : findKnownIndexNames(resolvedSymbols);

                // Keep a map of resolved symbols to original symbols to map results back correctly
                Map<String, String> resolvedToOriginal = new HashMap<>();
                Set<String> indexSymbols = new java.util.HashSet<>();
                Set<String> stockSymbols = new java.util.HashSet<>();
                
                for (String originalSymbol : symbols) {
                    Set<String> resolved = instrumentUtils.resolveSymbols(List.of(originalSymbol), fetchIndexStocks);
                    for (String sym : resolved) {
                        resolvedToOriginal.put(sym, originalSymbol);
                        if (routeAsIndex || isExplicitIndexSymbol(sym)
                                || resolvedIndexNames.contains(normalizeIndexName(sym))) {
                            indexSymbols.add(sym);
                        } else {
                            stockSymbols.add(sym);
                        }
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

                for (Map.Entry<String, HistoricalData> entry : batchResult.entrySet()) {
                    String resolvedSymbol = entry.getKey();
                    HistoricalData historicalData = entry.getValue();
                    String originalSymbol = resolvedToOriginal.get(resolvedSymbol);
                    
                    if (originalSymbol != null && historicalData != null && historicalData.getDataPoints() != null
                            && !historicalData.getDataPoints().isEmpty()) {
                        List<OHLCVTPoint> dataPoints = historicalData.getDataPoints();
                        int originalCount = dataPoints.size();

                        if (filterParams.isFiltered()) {
                            dataPoints = HistoricalDataFilterUtil.applyFilterStrategy(dataPoints, filterParams);
                        }

                        HistoricalData filteredHistoricalData = new HistoricalData();
                        filteredHistoricalData.setTradingSymbol(originalSymbol);
                        filteredHistoricalData.setInterval(interval.getApiValue());
                        filteredHistoricalData.setDataPoints(dataPoints);
                        filteredHistoricalData.setDataPointCount(dataPoints.size());
                        filteredHistoricalData.setExchange(historicalData.getExchange());
                        filteredHistoricalData.setCurrency(historicalData.getCurrency());
                        filteredHistoricalData.setIsin(historicalData.getIsin());
                        filteredHistoricalData.setFromDate(historicalData.getFromDate());
                        filteredHistoricalData.setToDate(historicalData.getToDate());
                        filteredHistoricalData.setRetrievalTime(historicalData.getRetrievalTime());

                        symbolsData.put(originalSymbol, filteredHistoricalData);
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
                        .processingTimeMs(span != null ? span.elapsedMillis() : 0)
                        .source(forceRefresh ? "provider" : "cache")
                        .build();

                if (flowLogger != null && span != null) {
                    flowLogger.complete(span, "successfulSymbols", successCount, "totalPoints", totalFilteredDataPoints);
                }
                return HistoricalDataResponseV1.builder()
                        .data(symbolsData)
                        .metadata(metadata)
                        .build();
            } catch (Exception e) {
                // Fixed SLF4J pattern: was log.error("methodName", "msg", e)
                log.error("Error in batch historical data retrieval", e);
                if (flowLogger != null && span != null) {
                    flowLogger.fail(span, e);
                }
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
                "Fetching option chain for symbol={} expiryDate={} forceRefresh={}",
                underlyingSymbol, expiryDate, forceRefresh);
        return marketDataService.getOptionChain(underlyingSymbol, expiryDate, null, forceRefresh);
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
            if (request.getFrom() == null || request.getFrom().trim().isEmpty()) {
                return HistoricalDataResponseV1.builder()
                        .error("Missing required parameter: from")
                        .message("The 'from' date parameter (YYYY-MM-DD) is required.")
                        .build();
            }
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

        Map<String, Object> additionalParams = request.getAdditionalParams() == null
                ? new HashMap<>()
                : new HashMap<>(request.getAdditionalParams());
        // Carry the request-level semantic type into the batch method. Without this,
        // plain index names returned by InstrumentUtils are mistaken for equities.
        additionalParams.put("isIndexSymbol", request.isIndexSymbol());
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

    private boolean isExplicitIndexSymbol(String symbol) {
        if (symbol == null) {
            return false;
        }
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("GLOBAL_INDEX|")
                || normalized.startsWith("GLOBAL_INDEX:")
                || normalized.startsWith("NSE_INDEX|")
                || normalized.startsWith("NSE_INDEX:")
                || normalized.startsWith("BSE_INDEX|")
                || normalized.startsWith("BSE_INDEX:");
    }

    private Set<String> findKnownIndexNames(Set<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> lookupNames = symbols.stream()
                .map(this::normalizeIndexName)
                .filter(name -> !name.isBlank())
                .collect(Collectors.toSet());
        try {
            List<StockIndicesMarketData> documents = stockIndicesMarketDataService
                    .findByIndexSymbols(lookupNames);
            if (documents == null || documents.isEmpty()) {
                return Collections.emptySet();
            }
            return documents.stream()
                    .map(StockIndicesMarketData::getIndexSymbol)
                    .filter(Objects::nonNull)
                    .map(this::normalizeIndexName)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            // Prefix and caller-level routing remain authoritative. A metadata outage must
            // not turn a valid historical request into an API failure.
            log.warn("Unable to resolve plain symbols as indexes: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    private String normalizeIndexName(String symbol) {
        if (symbol == null) {
            return "";
        }
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        for (String prefix : List.of("GLOBAL_INDEX|", "GLOBAL_INDEX:", "NSE_INDEX|", "NSE_INDEX:",
                "BSE_INDEX|", "BSE_INDEX:", "NSE_EQ|", "NSE_EQ:", "BSE_EQ|", "BSE_EQ:",
                "NSE:", "BSE:")) {
            if (normalized.startsWith(prefix)) {
                return normalized.substring(prefix.length()).trim();
            }
        }
        return normalized;
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

            // EXCHANGE-AWARE KEY SANITIZATION: Ensure the final map contains ONLY the exact symbol keys requested by the caller.
            // Preserves NSE vs BSE distinctions (e.g., NSE_EQ:RELIANCE vs BSE_EQ:RELIANCE) without collisions or unrequested bare keys.
            Map<String, OHLCQuote> sanitizedOHLC = new HashMap<>();
            for (String requestedSym : requested) {
                if (ohlcData.containsKey(requestedSym)) {
                    sanitizedOHLC.put(requestedSym, ohlcData.get(requestedSym));
                } else {
                    String cleanReq = requestedSym.contains(":") ? requestedSym.substring(requestedSym.indexOf(":") + 1).trim() : requestedSym;
                    if (ohlcData.containsKey(cleanReq)) {
                        sanitizedOHLC.put(requestedSym, ohlcData.get(cleanReq));
                    }
                }
            }
            if (!sanitizedOHLC.isEmpty()) {
                ohlcData = sanitizedOHLC;
            }

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

