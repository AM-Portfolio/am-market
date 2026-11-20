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
import com.am.marketdata.analysis.dto.TopMoversResponse;
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
    private final com.am.marketdata.redis.cache.AnalysisRedisCache analysisRedisCache;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.am.marketdata.service.calendar.MarketCalendarService marketCalendarService;

    // Default broad index for "entire market" analytics
    private static final String DEFAULT_MARKET_INDEX = "NIFTY 50";

    /**
     * Get Historical Charts (Batch or Single)
     */
    public com.am.marketdata.api.model.HistoricalDataResponseV1 getHistoricalCharts(
            String symbols, String range, Boolean requestedIndexSymbol) {

        String canonicalSymbols = canonicalizeChartSymbols(symbols);
        boolean isIndexSymbol = resolveChartSymbolType(canonicalSymbols, requestedIndexSymbol);
        String rangeKey = range == null || range.isBlank() ? "1Y" : range.trim().toUpperCase(Locale.ROOT);

        String chartCacheKey = "analysis:charts:" + (canonicalSymbols != null ? canonicalSymbols.trim().toUpperCase(Locale.ROOT) : "")
                + ":" + rangeKey + ":" + isIndexSymbol;
        if (analysisRedisCache != null) {
            try {
                String cachedJson = analysisRedisCache.getChartRawJson(chartCacheKey);
                if (cachedJson != null && !cachedJson.isBlank()) {
                    com.am.marketdata.api.model.HistoricalDataResponseV1 cached = analysisRedisCache.getRedisObjectMapper().readValue(
                            cachedJson, com.am.marketdata.api.model.HistoricalDataResponseV1.class);
                    ensureRequestedSymbolAliases(cached, symbols, canonicalSymbols);
                    return cached;
                }
            } catch (Exception e) {
                log.warn("getHistoricalCharts", "Redis cache read failed for " + chartCacheKey + ": " + e.getMessage());
            }
        }

        String interval = "1D";
        java.time.LocalDateTime to = java.time.LocalDateTime.now();
        java.time.LocalDateTime from = to.minusDays(1);

        // Detect if today is a weekend, holiday, or before market open. If so, roll back to the previous active trading day.
        java.time.ZonedDateTime istNow = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        java.time.LocalDate candidateDate = istNow.toLocalDate();
        java.time.LocalTime time = istNow.toLocalTime();
        boolean isBeforeMarketOpen = time.isBefore(java.time.LocalTime.of(9, 15));
        if (isBeforeMarketOpen) {
            candidateDate = candidateDate.minusDays(1);
        }
        java.time.LocalDate targetDate = adjustToLastTradingDay(candidateDate);

        // Determine Interval and From Time based on Range
        switch (rangeKey) {
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
                // Target exactly the active trading day from 00:00:00 to 23:59:59.
                // InfluxDB will query only ~75 five-minute candles for the single day,
                // avoiding the multi-second penalty of scanning 6+ calendar days.
                from = targetDate.atStartOfDay();
                to = targetDate.atTime(23, 59, 59);
                break;
            case "1W":
                // [Upstox Compatible Interval Mapping]
                // Upstox API accepts intervals: '1minute', '30minute', 'day', 'week', 'month'.
                // '1H' is not supported by Upstox v2/v3 and causes 0 data points.
                // Using '30m' provides rich 30-minute historical candle progression for the 1-week window across both Stocks & Indices.
                interval = "30m";
                from = to.minusWeeks(1).toLocalDate().atStartOfDay();
                break;
            case "1M":
                interval = "1D";
                from = to.minusMonths(1).toLocalDate().atStartOfDay();
                break;
            case "3M":
                interval = "1D";
                from = to.minusMonths(3).toLocalDate().atStartOfDay();
                break;
            case "6M":
                interval = "1D";
                from = to.minusMonths(6).toLocalDate().atStartOfDay();
                break;
            case "5Y":
                interval = "1W";
                from = to.minusYears(5).toLocalDate().atStartOfDay();
                break;
            default: // Default 1Y
                interval = "1D";
                from = to.minusYears(1).toLocalDate().atStartOfDay();
        }

        // Construct Request
        com.am.marketdata.api.dto.HistoricalDataRequest request = com.am.marketdata.api.dto.HistoricalDataRequest
                .builder()
                .symbols(canonicalSymbols)
                .from(from.toLocalDate().toString())
                .to(to.toLocalDate().toString())
                .interval(com.am.marketdata.common.model.TimeFrame.fromApiValue(interval)) // Convert string to
                                                                                           // TimeFrame
                .filterType("price")
                .indexSymbol(isIndexSymbol)
                .build();

        // Fetch Data
        try {
            boolean intradayChart = Set.of("10M", "15M", "30M", "1H", "4H", "1D").contains(rangeKey);
            // Intraday charts consume the WebSocket data already persisted by the
            // stream pipeline. Allow the existing historical-data cache to serve warm requests,
            // while keeping provider fallback disabled for routine live chart reads.
            com.am.marketdata.api.model.HistoricalDataResponseV1 response = marketDataFetchService
                    .processHistoricalDataRequest(request, true, !intradayChart);

            if (response.getError() != null) {
                return response;
            }

            boolean hasNoCandles = response.getData() == null || response.getData().isEmpty()
                    || response.getData().values().stream().allMatch(d -> d == null || d.getDataPoints() == null || d.getDataPoints().isEmpty());

            // If 1D chart returned 0 candles (e.g. cold start after a mid-session pod restart/deployment),
            // allow a single bounded provider fallback for targetDate so the chart self-heals immediately.
            if ("1D".equalsIgnoreCase(rangeKey) && hasNoCandles) {
                log.info("getHistoricalCharts", "1D chart had no cached candles for targetDate " + targetDate + "; attempting bounded provider fallback");
                try {
                    response = marketDataFetchService.processHistoricalDataRequest(request, true, true);
                    hasNoCandles = response.getData() == null || response.getData().isEmpty()
                            || response.getData().values().stream().allMatch(d -> d == null || d.getDataPoints() == null || d.getDataPoints().isEmpty());
                } catch (Exception ex) {
                    log.warn("getHistoricalCharts", "Provider fallback failed for targetDate " + targetDate + ": " + ex.getMessage());
                }
            }

            if ("1D".equalsIgnoreCase(rangeKey) && hasNoCandles) {
                // Rare edge case: targetDate had no trading candles (e.g. exchange holiday).
                // Fall back to querying previous 4 days so the chart is never blank.
                log.info("getHistoricalCharts", "1D targetDate " + targetDate + " had no candles (holiday/closed); falling back to lookback window");
                com.am.marketdata.api.dto.HistoricalDataRequest fallbackRequest = com.am.marketdata.api.dto.HistoricalDataRequest
                        .builder()
                        .symbols(canonicalSymbols)
                        .from(targetDate.minusDays(4).toString())
                        .to(targetDate.plusDays(1).toString())
                        .interval(com.am.marketdata.common.model.TimeFrame.fromApiValue(interval))
                        .filterType("price")
                        .indexSymbol(isIndexSymbol)
                        .build();
                response = marketDataFetchService.processHistoricalDataRequest(fallbackRequest, true, true);
            }

            // Filter data points by time range for each symbol
            if (response.getData() != null) {
                // Bound the filter to the starting bell of the active market session (9:15 AM IST).
                // By using 9:15 AM targetDate instead of "now - 24 hours" (which points to yesterday afternoon),
                // we preserve the complete intraday chart progression rather than stripping out all daily candles.
                java.time.LocalDateTime filterFrom = "1D".equalsIgnoreCase(rangeKey)
                        ? targetDate.atTime(9, 15)
                        : from;
                java.time.ZoneId kolkataZone = java.time.ZoneId.of("Asia/Kolkata");
                long minTime = filterFrom.atZone(kolkataZone).toInstant().toEpochMilli();
                long maxTime = "1D".equalsIgnoreCase(rangeKey)
                        ? targetDate.atTime(15, 30).atZone(kolkataZone).toInstant().toEpochMilli()
                        : Long.MAX_VALUE;

                final java.time.LocalDateTime queryFrom = from;
                response.getData().forEach((s, symbolData) -> {
                    if (symbolData != null && symbolData.getDataPoints() != null) {
                        boolean isGlobal = isGlobalIndexSymbol(s);
                        java.time.ZoneId pointZone = isGlobal ? java.time.ZoneOffset.UTC : kolkataZone;
                        long symMinTime = isGlobal
                                ? queryFrom.atZone(kolkataZone).toInstant().toEpochMilli()
                                : minTime;
                        long symMaxTime = isGlobal
                                ? Long.MAX_VALUE
                                : maxTime;

                        List<com.am.common.investment.model.historical.OHLCVTPoint> filteredPoints = symbolData
                                .getDataPoints().stream()
                                .filter(p -> {
                                    try {
                                        // A malformed provider candle must never reach the
                                        // chart. Returning it would make the UI draw a false
                                        // spike/flat line and could poison the next cache write.
                                        if (p == null || p.getTime() == null
                                                || !isValidCandle(p)) {
                                            return false;
                                        }
                                        // For global instruments, candle timestamps are UTC.
                                        // For Indian stocks/indices, candle timestamps are IST (Asia/Kolkata).
                                        long timestamp = p.getTime().atZone(pointZone)
                                                .toInstant()
                                                .toEpochMilli();
                                        return timestamp >= symMinTime && timestamp <= symMaxTime;
                                    } catch (Exception e) {
                                        return true;
                                    }
                                })
                                .filter(p -> p.getTime() != null)
                                .sorted(Comparator.comparing(
                                        com.am.common.investment.model.historical.OHLCVTPoint::getTime))
                                .collect(Collectors.toMap(
                                        com.am.common.investment.model.historical.OHLCVTPoint::getTime,
                                        p -> p,
                                        // Keep the last point at a duplicate timestamp; provider ticks
                                        // arriving later carry the most recent candle values.
                                        (first, last) -> last,
                                        LinkedHashMap::new))
                                .values().stream()
                                .collect(Collectors.toList());

                        if ("1D".equalsIgnoreCase(range) && filteredPoints.isEmpty() && !symbolData.getDataPoints().isEmpty()) {
                            if (!isGlobal) {
                                // Edge case: Target date had no trading session (e.g. market holiday).
                                // Find the most recent active session date available in data points.
                                java.time.LocalDate latestTradingDate = symbolData.getDataPoints().stream()
                                        .filter(this::isValidCandle)
                                        .map(pt -> pt.getTime().atZone(pointZone).toLocalDate())
                                        .max(java.util.Comparator.naturalOrder())
                                        .orElse(null);
                                if (latestTradingDate != null) {
                                    long fallbackMin = latestTradingDate.atTime(9, 15).atZone(kolkataZone).toInstant().toEpochMilli();
                                    long fallbackMax = latestTradingDate.atTime(15, 30).atZone(kolkataZone).toInstant().toEpochMilli();
                                    filteredPoints = symbolData.getDataPoints().stream()
                                            .filter(p -> {
                                                if (p == null || p.getTime() == null || !isValidCandle(p)) return false;
                                                long ts = p.getTime().atZone(pointZone).toInstant().toEpochMilli();
                                                return ts >= fallbackMin && ts <= fallbackMax;
                                            })
                                            .sorted(Comparator.comparing(com.am.common.investment.model.historical.OHLCVTPoint::getTime))
                                            .collect(Collectors.toMap(
                                                    com.am.common.investment.model.historical.OHLCVTPoint::getTime,
                                                    p -> p,
                                                    (first, last) -> last,
                                                    LinkedHashMap::new))
                                            .values().stream()
                                            .collect(Collectors.toList());
                                }
                            } else {
                                // For global indices, retain all available candles without Indian session bounding
                                filteredPoints = symbolData.getDataPoints().stream()
                                        .filter(this::isValidCandle)
                                        .sorted(Comparator.comparing(com.am.common.investment.model.historical.OHLCVTPoint::getTime))
                                        .collect(Collectors.toMap(
                                                com.am.common.investment.model.historical.OHLCVTPoint::getTime,
                                                p -> p,
                                                (first, last) -> last,
                                                LinkedHashMap::new))
                                        .values().stream()
                                        .collect(Collectors.toList());
                            }
                        }

                        if (!filteredPoints.isEmpty()) {
                            symbolData.setDataPoints(filteredPoints);
                            // The response count must describe the serialized points,
                            // not the provider's requested/available count. This prevents
                            // clients from interpreting a partial series as complete.
                            symbolData.setDataPointCount(filteredPoints.size());
                        } else {
                            symbolData.setDataPointCount(0);
                        }
                    }
                });
            }

            // Cache non-empty valid result in Redis with dynamic market-aware TTL
            if (analysisRedisCache != null && response != null && response.getData() != null && !response.getData().isEmpty()) {
                boolean hasPoints = response.getData().values().stream()
                        .anyMatch(sd -> sd != null && sd.getDataPoints() != null && !sd.getDataPoints().isEmpty());
                if (hasPoints) {
                    try {
                        long ttlSeconds = calculateChartTtlSeconds(rangeKey);
                        String json = analysisRedisCache.getRedisObjectMapper().writeValueAsString(response);
                        analysisRedisCache.saveChartRawJson(chartCacheKey, json, ttlSeconds);
                    } catch (Exception e) {
                        log.warn("getHistoricalCharts", "Redis cache write error for " + chartCacheKey + ": " + e.getMessage());
                    }
                }
            }
            ensureRequestedSymbolAliases(response, symbols, canonicalSymbols);
            return response;
        } catch (Exception e) {
            log.error("getHistoricalCharts", "Error fetching historical charts: " + e.getMessage());
            return com.am.marketdata.api.model.HistoricalDataResponseV1.builder()
                    .error("Failed to fetch chart data")
                    .message(e.getMessage())
                    .build();
        }
    }

    private boolean isValidCandle(com.am.common.investment.model.historical.OHLCVTPoint point) {
        double open = point.getOpen();
        double high = point.getHigh();
        double low = point.getLow();
        double close = point.getClose();
        return Double.isFinite(open) && Double.isFinite(high)
                && Double.isFinite(low) && Double.isFinite(close)
                && open > 0.0 && high > 0.0 && low > 0.0 && close > 0.0
                && low <= open && low <= close
                && high >= open && high >= close;
    }

    private long calculateChartTtlSeconds(String rangeKey) {
        boolean isIntraday = Set.of("10M", "15M", "30M", "1H", "4H", "1D").contains(rangeKey);
        if (!isIntraday) {
            return 300L;
        }
        java.time.ZonedDateTime nowIst = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        java.time.DayOfWeek day = nowIst.getDayOfWeek();
        boolean isWeekday = day != java.time.DayOfWeek.SATURDAY && day != java.time.DayOfWeek.SUNDAY;
        java.time.LocalTime time = nowIst.toLocalTime();
        boolean isMarketHours = isWeekday && !time.isBefore(java.time.LocalTime.of(9, 15))
                && !time.isAfter(java.time.LocalTime.of(15, 35));
        return isMarketHours ? 15L : 300L;
    }

    private boolean isGlobalIndexSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return false;
        }
        String up = symbol.toUpperCase(Locale.ROOT).trim();
        return up.startsWith("GLOBAL_INDEX|") || up.startsWith("GLOBAL_INDEX:") || up.startsWith("GLOBAL_")
                || Set.of("DJI", "^DJI", "DOW", "DOW FUTURES", "SPX", "^GSPC", "S&P 500", "IXIC", "NASDAQ",
                        "FTSE", "^FTSE", "GDAXI", "^GDAXI", "DAX", "FCHI", "^FCHI", "CAC", "N225", "^N225", "NIKKEI",
                        "HSI", "^HSI", "HANG SENG", "SGX NIFTY").contains(up);
    }

    static String canonicalizeChartSymbols(String symbols) {
        if (symbols == null || symbols.isBlank()) {
            return "";
        }
        return java.util.Arrays.stream(symbols.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(MarketAnalyticsService::canonicalizeIndexSymbol)
                .collect(Collectors.joining(","));
    }

    static String canonicalizeIndexSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return "";
        }
        String clean = symbol.trim().toUpperCase(Locale.ROOT);
        for (String prefix : List.of("GLOBAL_INDEX|", "GLOBAL_INDEX:", "NSE_INDEX|", "NSE_INDEX:",
                "BSE_INDEX|", "BSE_INDEX:", "NSE_EQ|", "BSE_EQ|", "NSE_EQ:", "BSE_EQ:",
                "NSE:", "BSE:")) {
            if (clean.startsWith(prefix)) {
                clean = clean.substring(prefix.length()).trim();
                break;
            }
        }
        return switch (clean) {
            case "NIFTY", "NIFTY50" -> "NIFTY 50";
            case "BANKNIFTY", "NIFTYBANK" -> "NIFTY BANK";
            case "FINNIFTY", "NIFTY FIN SERVICE", "NIFTY FINANCIAL SERVICES", "NIFTY FINANCIAL" -> "NIFTY FIN SERVICES";
            case "MIDCPNIFTY", "NIFTY MID SELECT", "NIFTY MIDCAP SELECT", "MIDCAPNIFTY", "NIFTY SELECT" -> "NIFTY MIDCAP 50";
            case "NIFTYNXT50", "NIFTY NEXT50", "NIFTY JR", "NIFTYJUNIOR" -> "NIFTY NEXT 50";
            case "NIFTY100" -> "NIFTY 100";
            case "NIFTY200" -> "NIFTY 200";
            case "NIFTY500" -> "NIFTY 500";
            case "NIFTYMIDCAP100", "NIFTY MID 100" -> "NIFTY MIDCAP 100";
            case "NIFTYSMALLCAP100", "NIFTY SMLCAP 100", "NIFTY SML 100" -> "NIFTY SMALLCAP 100";
            case "NIFTYAUTO" -> "NIFTY AUTO";
            case "NIFTYIT" -> "NIFTY IT";
            case "NIFTYPHARMA", "NIFTY PHARMACEUTICALS" -> "NIFTY PHARMA";
            case "NIFTYMETAL" -> "NIFTY METAL";
            case "NIFTYFMCG" -> "NIFTY FMCG";
            case "NIFTYENERGY" -> "NIFTY ENERGY";
            case "NIFTYREALTY" -> "NIFTY REALTY";
            case "NIFTYPSUBANK" -> "NIFTY PSU BANK";
            case "NIFTYPVTBANK", "NIFTY PRIVATE BANK" -> "NIFTY PVT BANK";
            case "BSESENSEX", "BSE SENSEX", "SENSEX 30" -> "SENSEX";
            case "BSEBANKEX", "BSE BANKEX" -> "BANKEX";
            case "INDIAVIX", "VIX" -> "INDIA VIX";
            default -> clean;
        };
    }

    private void ensureRequestedSymbolAliases(com.am.marketdata.api.model.HistoricalDataResponseV1 response,
            String requestedSymbols, String canonicalSymbols) {
        if (response == null || response.getData() == null || requestedSymbols == null || canonicalSymbols == null
                || requestedSymbols.equalsIgnoreCase(canonicalSymbols)) {
            return;
        }
        List<String> reqList = new ArrayList<>(splitChartSymbols(requestedSymbols));
        List<String> canList = new ArrayList<>(splitChartSymbols(canonicalSymbols));
        for (int i = 0; i < reqList.size() && i < canList.size(); i++) {
            String req = reqList.get(i);
            String can = canList.get(i);
            if (!req.equalsIgnoreCase(can) && response.getData().containsKey(can)
                    && !response.getData().containsKey(req)) {
                response.getData().put(req, response.getData().get(can));
            }
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

        // Fast path: known index patterns or global ticker symbols resolve immediately without DB round-trips
        boolean allMatchIndexPattern = requestedSymbols.stream().allMatch(s -> {
            String up = s.toUpperCase(Locale.ROOT);
            return up.startsWith("NIFTY") || up.startsWith("BANKNIFTY") || up.startsWith("FINNIFTY")
                    || up.startsWith("MIDCPNIFTY") || up.startsWith("SENSEX") || up.startsWith("BANKEX") || up.startsWith("BSE")
                    || up.startsWith("INDIA VIX") || up.startsWith("INDIAVIX")
                    || Set.of("DJI", "SPX", "IXIC", "FTSE", "GDAXI", "FCHI", "N225", "HSI").contains(up);
        });
        if (allMatchIndexPattern) {
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
     * @param indexSymbol   Index to use for filtering (e.g., "NIFTY 50", "NIFTY 500")
     * @param timeFrame     Time frame for price data (e.g., 1D, 1W, 1M)
     * @param expandIndices Whether to expand index symbols to constituent stocks
     * @return List of enriched stock data sorted by percentage change
     */
    public List<StockMoverDTO> getMovers(int limit, String type, String indexSymbol,
            com.am.marketdata.common.model.TimeFrame timeFrame, boolean expandIndices) {
        return getMovers(limit, type, indexSymbol, timeFrame, expandIndices, false);
    }

    public List<StockMoverDTO> getMovers(int limit, String type, String indexSymbol,
            com.am.marketdata.common.model.TimeFrame timeFrame, boolean expandIndices, boolean forceRefresh) {
        // Normalize symbol (e.g. "NSE:NIFTY 50" -> "NIFTY 50") to match MongoDB stock_indices_market_data _id
        String targetIndex = analysisRedisCache != null ? analysisRedisCache.normalizeIndexSymbol(indexSymbol != null && !indexSymbol.isEmpty() ? indexSymbol : DEFAULT_MARKET_INDEX) : (indexSymbol != null && !indexSymbol.isEmpty() ? indexSymbol.trim().toUpperCase(Locale.ROOT) : DEFAULT_MARKET_INDEX);
        String tfStr = timeFrame != null ? timeFrame.getApiValue() : "1D";

        // 1. Check Redis response cache first (bypass if forceRefresh is true)
        if (!forceRefresh && analysisRedisCache != null) {
            String cachedJson = analysisRedisCache.getMoversRawJson(targetIndex, tfStr, limit, type, expandIndices);
            if (cachedJson != null) {
                try {
                    return analysisRedisCache.getRedisObjectMapper().readValue(cachedJson,
                            new com.fasterxml.jackson.core.type.TypeReference<List<StockMoverDTO>>() {});
                } catch (Exception e) {
                    log.warn("getMovers", "Failed to deserialize cached movers for " + targetIndex + ": " + e.getMessage());
                }
            }
        }

        // 2. Cache miss: Fetch and enrich constituent quotes from MongoDB / InfluxDB
        List<EnrichedStockData> enrichedData = fetchEnrichedData(targetIndex, timeFrame, expandIndices, true);

        if (enrichedData.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. Sort by percentage change (gainers descending, losers ascending)
        boolean descending = "gainers".equalsIgnoreCase(type);
        List<EnrichedStockData> filteredData = enrichedData.stream()
                .filter(data -> data.getChange() != null && Double.isFinite(data.getChange())
                        && (descending ? data.getChange() > 0.0 : data.getChange() < 0.0))
                .collect(Collectors.toList());
        List<EnrichedStockData> sortedData = stockDataEnricher.sortByPercentChange(filteredData, descending);

        // Convert to response format and limit results
        List<StockMoverDTO> result = sortedData.stream()
                .limit(limit)
                .map(this::enrichedDataToMap)
                .collect(Collectors.toList());

        // 4. Cache non-empty valid result in Redis with dynamic market-aware TTL
        if (analysisRedisCache != null && result != null && !result.isEmpty()) {
            try {
                String json = analysisRedisCache.getRedisObjectMapper().writeValueAsString(result);
                analysisRedisCache.saveMoversRawJson(targetIndex, tfStr, limit, type, expandIndices, json);
            } catch (Exception e) {
                log.warn("getMovers", "Failed to serialize movers cache: " + e.getMessage());
            }
        }

        return result;
    }

    /**
     * Get Top Gainers AND Losers (Unified)
     */
    public TopMoversResponse getMoversUnified(int limit, String indexSymbol,
            com.am.marketdata.common.model.TimeFrame timeFrame, boolean expandIndices) {
        return getMoversUnified(limit, indexSymbol, timeFrame, expandIndices, false);
    }

    public TopMoversResponse getMoversUnified(int limit, String indexSymbol,
            com.am.marketdata.common.model.TimeFrame timeFrame, boolean expandIndices, boolean forceRefresh) {
        // Normalize symbol to match MongoDB stock_indices_market_data _id
        String targetIndex = analysisRedisCache != null ? analysisRedisCache.normalizeIndexSymbol(indexSymbol != null && !indexSymbol.isEmpty() ? indexSymbol : DEFAULT_MARKET_INDEX) : (indexSymbol != null && !indexSymbol.isEmpty() ? indexSymbol.trim().toUpperCase(Locale.ROOT) : DEFAULT_MARKET_INDEX);
        String tfStr = timeFrame != null ? timeFrame.getApiValue() : "1D";

        // 1. Check Redis response cache first (bypass if forceRefresh is true)
        if (!forceRefresh && analysisRedisCache != null) {
            String cachedJson = analysisRedisCache.getMoversRawJson(targetIndex, tfStr, limit, "all", expandIndices);
            if (cachedJson != null) {
                try {
                    return analysisRedisCache.getRedisObjectMapper().readValue(cachedJson, TopMoversResponse.class);
                } catch (Exception e) {
                    log.warn("getMoversUnified", "Failed to deserialize cached unified movers for " + targetIndex + ": " + e.getMessage());
                }
            }
        }

        /*
         * 2. Read prices once, then sort the same snapshot in both directions.
         * This avoids two slow provider calls and mismatched gainers/losers.
         */
        List<EnrichedStockData> enrichedData = fetchEnrichedData(targetIndex, timeFrame, expandIndices, true);

        // Flat stocks are not movers. Keeping zero-change rows in both sorted lists
        // made the same unchanged stocks appear as both gainers and losers.
        List<EnrichedStockData> gainersData = enrichedData.stream()
                .filter(this::hasConsistentReturn)
                .filter(data -> data.getPercentChange() > 0.0)
                .collect(Collectors.toList());
        List<EnrichedStockData> losersData = enrichedData.stream()
                .filter(this::hasConsistentReturn)
                .filter(data -> data.getPercentChange() < 0.0)
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

        TopMoversResponse response = TopMoversResponse.builder()
                .gainers(gainers)
                .losers(losers)
                .build();

        // 3. Guard against caching degenerate/empty results before storing in Redis
        if (analysisRedisCache != null && ((gainers != null && !gainers.isEmpty()) || (losers != null && !losers.isEmpty()))) {
            try {
                String json = analysisRedisCache.getRedisObjectMapper().writeValueAsString(response);
                analysisRedisCache.saveMoversRawJson(targetIndex, tfStr, limit, "all", expandIndices, json);
            } catch (Exception e) {
                log.warn("getMoversUnified", "Failed to serialize unified movers cache: " + e.getMessage());
            }
        }

        return response;
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
            if (expectedMembers == 0 || expectedMembers < minimumMembers || enrichedData.size() < minimumMembers) {
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
        double change = data.getLastPrice() - data.getPreviousClose();
        double percentChange = (change / data.getPreviousClose()) * 100.0;
        return StockMoverDTO.builder()
                .symbol(data.getSymbol())
                .lastPrice(data.getLastPrice())
                .change(change)
                .pChange(percentChange)
                .percentChange(percentChange)
                .previousClose(data.getPreviousClose())
                .build();
    }

    private boolean hasConsistentReturn(EnrichedStockData data) {
        if (data == null || !data.hasValidPrice()
                || data.getLastPrice() == null || data.getPreviousClose() == null
                || !Double.isFinite(data.getLastPrice())
                || !Double.isFinite(data.getPreviousClose())
                || data.getLastPrice() <= 0.0 || data.getPreviousClose() <= 0.0) {
            return false;
        }
        double expectedChange = data.getLastPrice() - data.getPreviousClose();
        double expectedPercent = (expectedChange / data.getPreviousClose()) * 100.0;
        // Provider percentages are commonly rounded to two decimals. The
        // canonical values calculated from the prices are authoritative; do
        // not reject a valid quote because its rounded provider fields differ
        // by a few floating-point units.
        return Double.isFinite(expectedChange) && Double.isFinite(expectedPercent);
    }

    /**
     * Rolls back a date to the latest open trading session using exchange calendar rules (weekends and holidays).
     */
    private java.time.LocalDate adjustToLastTradingDay(java.time.LocalDate date) {
        java.time.LocalDate current = date;
        for (int i = 0; i < 15; i++) {
            if (isTradingDay(current)) {
                return current;
            }
            current = current.minusDays(1);
        }
        return current;
    }

    /**
     * Checks if a date is an active exchange trading day according to MarketCalendarService.
     */
    private boolean isTradingDay(java.time.LocalDate date) {
        if (marketCalendarService != null) {
            try {
                com.am.marketdata.service.calendar.MarketCalendarService.SessionTiming timings = marketCalendarService.getTimings("NSE", date);
                if (timings != null) {
                    return timings.open();
                }
            } catch (Exception e) {
                log.warn("isTradingDay", "Market calendar lookup failed for " + date + ": " + e.getMessage());
            }
        }
        java.time.DayOfWeek dow = date.getDayOfWeek();
        return dow != java.time.DayOfWeek.SATURDAY && dow != java.time.DayOfWeek.SUNDAY;
    }
}
