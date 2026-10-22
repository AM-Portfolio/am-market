package com.am.marketdata.analysis.service;

import com.am.marketdata.common.model.analysis.CalendarHeatmapResponse;
import com.am.marketdata.common.model.analysis.SeasonalityResponse;
import com.am.marketdata.common.model.analysis.TechnicalAnalysisResponse;
import com.am.marketdata.redis.cache.AnalysisRedisCache;
import com.am.common.investment.model.historical.HistoricalData;
import com.am.common.investment.model.historical.OHLCVTPoint;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class AnalysisService {

    private final MarketDataService marketDataService;
    private final AnalysisRedisCache analysisRedisCache;

    // --- Seasonality Analysis ---

    public SeasonalityResponse getSeasonalityAnalysis(String symbol, TimeFrame interval) {
        SeasonalityResponse cached = analysisRedisCache.getSeasonality(symbol, interval);
        if (cached != null) {
            return cached;
        }

        LocalDate to = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));
        LocalDate from = to.minusYears(5);
        HistoricalData data = marketDataService.getHistoricalData(symbol, java.sql.Date.valueOf(from),
                java.sql.Date.valueOf(to), interval, true, null, null);
        SeasonalityResponse response = calculateSeasonality(symbol, data);
        analysisRedisCache.saveSeasonality(response, interval);
        return response;
    }

    public Map<String, SeasonalityResponse> getSeasonalityAnalysisBatch(List<String> symbols, TimeFrame interval) {
        // Simple implementation: check cache one by one, then fetch missing
        // A better implementation would be to modify AnalysisRedisCache to support
        // multiGet

        // For now, let's just fetch everything and not worry about partial cache hits
        // for simplicity in this iteration
        // OR, just do individually since we are removing the auto-batching annotation
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusYears(5);

        // Use batch fetch
        Map<String, HistoricalData> batchData = marketDataService.getHistoricalDataBatch(
                symbols,
                java.sql.Date.valueOf(from),
                java.sql.Date.valueOf(to),
                interval,
                true,
                null,
                null,
                false,
                false);

        Map<String, SeasonalityResponse> results = new HashMap<>();
        for (String symbol : symbols) {
            SeasonalityResponse res = calculateSeasonality(symbol, batchData.get(symbol));
            results.put(symbol, res);
        }

        // Batch save to cache
        analysisRedisCache.saveSeasonalityBatch(new ArrayList<>(results.values()), interval);

        return results;
    }

    private SeasonalityResponse calculateSeasonality(String symbol, HistoricalData data) {
        if (data == null || data.getDataPoints() == null || data.getDataPoints().isEmpty()) {
            return SeasonalityResponse.builder().symbol(symbol).build();
        }

        Map<DayOfWeek, List<Double>> dayOfWeekReturns = new EnumMap<>(DayOfWeek.class);
        Map<Integer, List<Double>> monthlyReturns = new HashMap<>();

        List<OHLCVTPoint> points = data.getDataPoints();
        for (int i = 1; i < points.size(); i++) {
            OHLCVTPoint prev = points.get(i - 1);
            OHLCVTPoint curr = points.get(i);

            if (prev.getClose() == null || curr.getClose() == null)
                continue;
            if (prev.getClose() == 0)
                continue;

            double ret = (curr.getClose() - prev.getClose()) / prev.getClose() * 100.0;

            // Use LocalDateTime directly
            if (curr.getTime() != null) {
                DayOfWeek day = curr.getTime().getDayOfWeek();
                if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                    dayOfWeekReturns.computeIfAbsent(day, k -> new ArrayList<>()).add(ret);
                }
                monthlyReturns.computeIfAbsent(curr.getTime().getMonthValue(), k -> new ArrayList<>()).add(ret);
            }
        }

        Map<String, Double> avgDayOfWeek = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            if (dayOfWeekReturns.containsKey(day)) {
                avgDayOfWeek.put(day.toString(),
                        round2(dayOfWeekReturns.get(day).stream().mapToDouble(d -> d).average().orElse(0.0)));
            }
        }

        Map<String, Double> avgMonth = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            if (monthlyReturns.containsKey(m)) {
                avgMonth.put(Month.of(m).toString(),
                        round2(monthlyReturns.get(m).stream().mapToDouble(d -> d).average().orElse(0.0)));
            }
        }

        return SeasonalityResponse.builder()
                .symbol(symbol)
                .dayOfWeekReturns(avgDayOfWeek)
                .monthlyReturns(avgMonth)
                .build();
    }

    // --- Technical Analysis ---

    public TechnicalAnalysisResponse getTechnicalAnalysis(String symbol, TimeFrame interval) {
        TechnicalAnalysisResponse cached = analysisRedisCache.getTechnical(symbol, interval);
        if (cached != null) {
            return cached;
        }

        LocalDate to = LocalDate.now();
        LocalDate from = to.minusYears(2);
        HistoricalData data = marketDataService.getHistoricalData(symbol, java.sql.Date.valueOf(from),
                java.sql.Date.valueOf(to), interval, true, null, null);
        TechnicalAnalysisResponse response = calculateTechnical(symbol, data);
        analysisRedisCache.saveTechnical(response, interval);
        return response;
    }

    public Map<String, TechnicalAnalysisResponse> getTechnicalAnalysisBatch(List<String> symbols, TimeFrame interval) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusYears(2);

        Map<String, HistoricalData> batchData = marketDataService.getHistoricalDataBatch(
                symbols,
                java.sql.Date.valueOf(from),
                java.sql.Date.valueOf(to),
                interval,
                true,
                null,
                null,
                false,
                false);

        Map<String, TechnicalAnalysisResponse> results = new HashMap<>();
        for (String symbol : symbols) {
            TechnicalAnalysisResponse res = calculateTechnical(symbol, batchData.get(symbol));
            results.put(symbol, res);
            // Save individually for now as we didn't implement batch save for technical
            analysisRedisCache.saveTechnical(res, interval);
        }
        return results;
    }

    private TechnicalAnalysisResponse calculateTechnical(String symbol, HistoricalData data) {
        if (data == null || data.getDataPoints() == null || data.getDataPoints().isEmpty()) {
            return TechnicalAnalysisResponse.builder().symbol(symbol).build();
        }

        List<OHLCVTPoint> points = data.getDataPoints();
        List<Double> closes = points.stream()
                .map(OHLCVTPoint::getClose)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        int n = closes.size();
        double currentPrice = round2(n > 0 ? closes.get(n - 1) : 0.0);

        Double sma20 = round2(calculateSMA(closes, 20));
        Double sma50 = round2(calculateSMA(closes, 50));
        Double sma200 = round2(calculateSMA(closes, 200));
        Double rsi14 = round2(calculateRSI(closes, 14));

        TechnicalAnalysisResponse.TechnicalAnalysisResponseBuilder builder = TechnicalAnalysisResponse.builder()
                .symbol(symbol)
                .currentPrice(currentPrice)
                .sma20(sma20)
                .sma50(sma50)
                .sma200(sma200)
                .rsi14(rsi14);

        String signal = "NEUTRAL";
        if (sma50 != null && currentPrice > sma50)
            signal = "BUY";
        else if (sma50 != null && currentPrice < sma50)
            signal = "SELL";
        builder.signal(signal);

        return builder.build();
    }

    // --- Calendar Heatmap ---

    public CalendarHeatmapResponse getCalendarHeatmap(String symbol, int year) {
        CalendarHeatmapResponse cached = analysisRedisCache.getHeatmap(symbol, year);
        if (cached != null) {
            return cached;
        }

        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);
        if (year == LocalDate.now().getYear()) {
            to = LocalDate.now();
        }

        LocalDate fetchFrom = from.minusDays(7);

        HistoricalData data = marketDataService.getHistoricalData(symbol,
                java.sql.Date.valueOf(fetchFrom),
                java.sql.Date.valueOf(to),
                TimeFrame.DAY,
                true,
                null,
                null);

        Map<String, Map<Integer, Double>> responseData = new LinkedHashMap<>();
        for (Month m : Month.values()) {
            responseData.put(m.toString(), new HashMap<>());
        }

        if (data != null && data.getDataPoints() != null) {
            List<OHLCVTPoint> points = data.getDataPoints();

            Map<LocalDate, Double> closeMap = new HashMap<>();
            for (OHLCVTPoint dp : points) {
                if (dp.getTime() != null) {
                    closeMap.put(dp.getTime().toLocalDate(), dp.getClose());
                }
            }

            LocalDate curr = from;
            while (!curr.isAfter(to)) {
                if (closeMap.containsKey(curr)) {
                    LocalDate prev = curr.minusDays(1);
                    int lookback = 0;
                    Double prevClose = null;
                    while (lookback < 7) {
                        if (closeMap.containsKey(prev)) {
                            prevClose = closeMap.get(prev);
                            break;
                        }
                        prev = prev.minusDays(1);
                        lookback++;
                    }

                    if (prevClose != null && prevClose > 0) {
                        double currentClose = closeMap.get(curr);
                        double change = (currentClose - prevClose) / prevClose * 100.0;
                        responseData.get(curr.getMonth().toString()).put(curr.getDayOfMonth(), round2(change));
                    }
                }
                curr = curr.plusDays(1);
            }
        }

        CalendarHeatmapResponse response = CalendarHeatmapResponse.builder()
                .symbol(symbol)
                .year(year)
                .data(responseData)
                .build();
        analysisRedisCache.saveHeatmap(response, year);
        return response;
    }

    // --- Helpers ---

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private Double round2(Double value) {
        if (value == null)
            return null;
        return Math.round(value * 100.0) / 100.0;
    }

    private Double calculateSMA(List<Double> prices, int period) {
        if (prices.size() < period)
            return null;
        double sum = 0;
        for (int i = prices.size() - period; i < prices.size(); i++) {
            sum += prices.get(i);
        }
        return sum / period;
    }

    private Double calculateRSI(List<Double> prices, int period) {
        if (prices.size() < period + 1)
            return null;

        double avgGain = 0;
        double avgLoss = 0;

        for (int i = 1; i <= period; i++) {
            double change = prices.get(i) - prices.get(i - 1);
            if (change > 0)
                avgGain += change;
            else
                avgLoss += Math.abs(change);
        }
        avgGain /= period;
        avgLoss /= period;

        for (int i = period + 1; i < prices.size(); i++) {
            double change = prices.get(i) - prices.get(i - 1);
            double gain = change > 0 ? change : 0;
            double loss = change < 0 ? Math.abs(change) : 0;

            avgGain = ((avgGain * (period - 1)) + gain) / period;
            avgLoss = ((avgLoss * (period - 1)) + loss) / period;
        }

        if (avgLoss == 0)
            return 100.0;
        double rs = avgGain / avgLoss;
        return 100.0 - (100.0 / (1.0 + rs));
    }
    // --- Historical Monthly Performance ---

    public com.am.marketdata.common.model.analysis.HistoricalPerformanceResponse getHistoricalPerformance(
            String symbol, int years, boolean detailed) {
        return getHistoricalPerformance(symbol, years, detailed, false);
    }

    public com.am.marketdata.common.model.analysis.HistoricalPerformanceResponse getHistoricalPerformance(
            String symbol, int years, boolean detailed, boolean bypassCache) {

        // Validate years
        if (years <= 0)
            years = 5;

        // Check Cache
        if (!bypassCache) {
            com.am.marketdata.common.model.analysis.HistoricalPerformanceResponse cached = analysisRedisCache
                    .getHistoricalPerformance(symbol, years, detailed);
            if (cached != null) {
                return cached;
            }
        }

        LocalDate to = LocalDate.now();
        LocalDate from = LocalDate.of(to.getYear() - years + 1, 1, 1); // Start from Jan 1st of the starting year
        LocalDate fetchFrom = from.minusDays(15); // Fetch extra days for previous close

        HistoricalData data = marketDataService.getHistoricalData(symbol,
                java.sql.Date.valueOf(fetchFrom),
                java.sql.Date.valueOf(to),
                TimeFrame.DAY,
                true,
                null,
                null);

        if (data == null || data.getDataPoints() == null || data.getDataPoints().isEmpty()) {
            return com.am.marketdata.common.model.analysis.HistoricalPerformanceResponse.builder()
                    .symbol(symbol)
                    .startYear(from.getYear())
                    .endYear(to.getYear())
                    .build();
        }

        List<OHLCVTPoint> points = data.getDataPoints();
        // Sort points by date just in case
        points.sort(Comparator.comparing(OHLCVTPoint::getTime));

        Map<Integer, com.am.marketdata.common.model.analysis.YearlyPerformance> yearlyMap = new TreeMap<>(
                Comparator.reverseOrder());

        // Map Date -> Close
        NavigableMap<LocalDate, Double> closeMap = new TreeMap<>();
        for (OHLCVTPoint p : points) {
            if (p.getTime() != null && p.getClose() != null) {
                closeMap.put(p.getTime().toLocalDate(), p.getClose());
            }
        }

        double overallStartPrice = -1;
        double overallEndPrice = -1;

        // Iterate through each year
        for (int y = to.getYear(); y >= from.getYear(); y--) {
            int currentYear = y;
            Map<String, Double> monthlyReturns = new LinkedHashMap<>();
            Map<String, Map<Integer, Double>> dailyReturns = detailed ? new LinkedHashMap<>() : null;

            double yearStartPrice = -1;
            double yearEndPrice = -1;

            // Iterate through months
            for (Month m : Month.values()) {
                // Skip future months
                if (currentYear == to.getYear() && m.getValue() > to.getMonthValue())
                    continue;

                LocalDate monthStart = LocalDate.of(currentYear, m, 1);
                LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

                // Adjust monthEnd if it's the current month and today is before end
                if (currentYear == to.getYear() && m == to.getMonth() && to.isBefore(monthEnd)) {
                    monthEnd = to;
                }

                // Find last available price for this month
                Map.Entry<LocalDate, Double> lastEntry = closeMap.floorEntry(monthEnd);
                // Find last available price BEFORE this month (for return calculation)
                Map.Entry<LocalDate, Double> prevEntry = closeMap.floorEntry(monthStart.minusDays(1));

                if (lastEntry != null && prevEntry != null
                        && lastEntry.getKey().getMonth() == m // Ensure last entry is actually IN this month
                        && lastEntry.getKey().getYear() == currentYear) {

                    double startPrice = prevEntry.getValue();
                    double endPrice = lastEntry.getValue();

                    if (startPrice > 0) {
                        double ret = (endPrice - startPrice) / startPrice * 100.0;
                        monthlyReturns.put(m.toString(), round2(ret));
                    }

                    if (yearStartPrice == -1 && startPrice > 0)
                        yearStartPrice = startPrice; // First month processed (Jan or partial)
                    yearEndPrice = endPrice; // Keep updating
                }

                // Detailed Daily Returns
                if (detailed) {
                    Map<Integer, Double> days = new LinkedHashMap<>();
                    LocalDate d = monthStart;
                    while (!d.isAfter(monthEnd)) {
                        Double todayClose = closeMap.get(d);
                        Double yesterdayClose = closeMap.get(d.minusDays(1)); // This might rely on map having exact key
                        // Better: use floorEntry for yesterday? No, return is strict day-to-day
                        // Actually, for calendar heatmap, it compares to previous trading day
                        if (todayClose != null) {
                            Map.Entry<LocalDate, Double> prevDayEntry = closeMap.floorEntry(d.minusDays(1));
                            if (prevDayEntry != null) {
                                double pClose = prevDayEntry.getValue();
                                double change = (todayClose - pClose) / pClose * 100.0;
                                days.put(d.getDayOfMonth(), round2(change));
                            }
                        }
                        d = d.plusDays(1);
                    }
                    if (!days.isEmpty()) {
                        if (dailyReturns == null)
                            dailyReturns = new LinkedHashMap<>();
                        dailyReturns.put(m.toString(), days);
                    }
                }
            }

            // Calculate Yearly Return
            // This is roughly (YearClose - YearOpen) / YearOpen
            // But YearOpen is essentially Close of Dec 31st Previous Year
            Double yearlyRet = null;

            // Re-eval year start price: it should be the close of the year BEFORE
            Map.Entry<LocalDate, Double> yearPrevEntry = closeMap
                    .floorEntry(LocalDate.of(currentYear, 1, 1).minusDays(1));
            // And year end price is close of last avail day in year
            Map.Entry<LocalDate, Double> yearLastEntry = closeMap.floorEntry(LocalDate.of(currentYear, 12, 31));

            if (yearPrevEntry != null && yearLastEntry != null && yearLastEntry.getKey().getYear() == currentYear) {
                double yStart = yearPrevEntry.getValue();
                double yEnd = yearLastEntry.getValue();
                if (yStart > 0) {
                    yearlyRet = round2((yEnd - yStart) / yStart * 100.0);
                }

                // Capture for overall
                if (currentYear == to.getYear())
                    overallEndPrice = yEnd;
                if (currentYear == from.getYear())
                    overallStartPrice = yStart;
            }

            if (!monthlyReturns.isEmpty()) {
                yearlyMap.put(currentYear, com.am.marketdata.common.model.analysis.YearlyPerformance.builder()
                        .year(currentYear)
                        .yearlyReturn(yearlyRet)
                        .monthlyReturns(monthlyReturns)
                        .dailyReturns(dailyReturns)
                        .build());
            }
        }

        Double overallReturn = null;
        if (overallStartPrice > 0 && overallEndPrice > 0) {
            overallReturn = round2((overallEndPrice - overallStartPrice) / overallStartPrice * 100.0);
        }

        com.am.marketdata.common.model.analysis.HistoricalPerformanceResponse response = com.am.marketdata.common.model.analysis.HistoricalPerformanceResponse
                .builder()
                .symbol(symbol)
                .startYear(from.getYear())
                .endYear(to.getYear())
                .overallReturn(overallReturn)
                .yearlyPerformance(new ArrayList<>(yearlyMap.values()))
                .build();

        analysisRedisCache.saveHistoricalPerformance(response, years, detailed);
        return response;
    }
    // --- Heatmap Analysis ---

    public Map<String, Double> getHeatmap(String symbol, String timeframeStr) {
        return getHeatmap(symbol, timeframeStr, false);
    }

    public Map<String, Double> getHeatmap(String symbol, String timeframeStr, boolean bypassCache) {
        String tfNorm = timeframeStr == null ? "1D" : timeframeStr.trim().toUpperCase();

        // Check Cache — ignore degenerate INDICES maps (almost all 0.0) so UI recovers
        // without waiting for the long analysis TTL to expire.
        if (!bypassCache) {
            Map<String, Double> cached = analysisRedisCache.getIndexHeatmap(symbol, tfNorm);
            if (cached != null && !isDegenerateIndicesHeatmap(tfNorm, cached)) {
                return cached;
            }
            if (cached != null) {
                log.warn("Discarding degenerate cached heatmap for {} {}", symbol, tfNorm);
                analysisRedisCache.deleteIndexHeatmap(symbol, tfNorm);
            }
        }

        // 1. Get Constituents
        List<String> symbols = marketDataService.getIndexConstituents(symbol);
        boolean isConstituentIndex = symbols != null && !symbols.isEmpty();
        if (symbols == null || symbols.isEmpty()) {
            // Fallback: if no constituents, maybe it's a single stock?
            // But the feature is "Index Heatmap".
            // If the user requests a stock ticker like "RELIANCE", getIndexConstituents
            // returns empty.
            // In that case, we return map with single entry?
            // Let's assume valid index for now.
            if (!"INDICES".equalsIgnoreCase(symbol)) {
                symbols = Collections.singletonList(symbol);
            } else {
                return new HashMap<>();
            }
        }

        // 2. Parse Timeframe & Determine Dates
        LocalDate to = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));
        LocalDate from;

        // Custom logic for duration
        String tfUpper = tfNorm;
        if (tfUpper.equals("1D")) {
            from = to.minusDays(5); // Look back enough to find prev close
        } else if (tfUpper.equals("1W") || tfUpper.equals("5D")) {
            from = to.minusWeeks(1);
        } else if (tfUpper.equals("1M")) {
            from = to.minusMonths(1);
        } else if (tfUpper.equals("3M")) {
            from = to.minusMonths(3);
        } else if (tfUpper.equals("6M")) {
            from = to.minusMonths(6);
        } else if (tfUpper.equals("1Y")) {
            from = to.minusYears(1);
        } else if (tfUpper.equals("3Y")) {
            from = to.minusYears(3);
        } else if (tfUpper.equals("5Y")) {
            from = to.minusYears(5);
        } else {
            // Default 1D
            from = to.minusDays(5);
        }

        // Optimize interval: For > 1Y, use WEEKLY to save data size
        TimeFrame interval = TimeFrame.DAY;
        if (tfUpper.equals("3Y") || tfUpper.equals("5Y")) {
            interval = TimeFrame.WEEK;
        }

        boolean isIndexBatch = "INDICES".equalsIgnoreCase(symbol);

        // 3. Fetch Data & Calculate Returns
        Map<String, Double> heatmap = new HashMap<>();
        LocalDate targetStartDate = CalculateTargetDate(to, tfUpper);

        if (tfUpper.equals("1D")) {
            // For 1D timeframe: Use live session OHLC quotes directly via marketDataService.getOHLC.
            // Historical candle lookups (InfluxDB) often do not have today's unclosed candle or may fail
            // during active sessions, which previously resulted in empty data points and 0.00% across the board.
            // Live OHLC quotes supply the exact session lastPrice and validated previousClose baseline,
            // producing the true real-time percentage move: ((lastPrice - previousClose) / previousClose) * 100.
            Map<String, OHLCQuote> dayQuotes = marketDataService.getOHLC(symbols, TimeFrame.DAY, bypassCache, null);
            if (dayQuotes != null && !dayQuotes.isEmpty()) {
                for (String sym : symbols) {
                    OHLCQuote quote = dayQuotes.get(sym);
                    if (quote == null && sym.contains(":")) {
                        quote = dayQuotes.get(sym.substring(sym.indexOf(":") + 1));
                    }
                    if (quote == null) {
                        quote = dayQuotes.get("NSE:" + sym);
                    }
                    if (quote != null && isValidPrice(quote.getLastPrice()) && isValidPrice(quote.getPreviousClose())) {
                        double change = (quote.getLastPrice() - quote.getPreviousClose()) / quote.getPreviousClose() * 100.0;
                        heatmap.put(sym, round2(change));
                    }
                }
            }
        }

        // Recover only missing members so a partial live response does not force valid symbols
        // through a slower history path or discard their current-session values.
        Set<String> availableSymbols = new HashSet<>(heatmap.keySet());
        List<String> symbolsNeedingHistory = symbols.stream()
                .filter(candidate -> !availableSymbols.contains(candidate)).toList();
        if (!symbolsNeedingHistory.isEmpty()) {
            long historyStartedAt = System.nanoTime();
            Map<String, HistoricalData> startData;
            Map<String, HistoricalData> endData;
            int batchCalls;
            int lookbackDays = interval == TimeFrame.WEEK ? 42 : 10;
            LocalDate startWindowFrom = targetStartDate.minusDays(lookbackDays);
            LocalDate startWindowTo = targetStartDate.plusDays(1);
            LocalDate endWindowFrom = to.minusDays(lookbackDays);
            LocalDate endWindowTo = to.plusDays(1);

            if ("1D".equals(tfUpper)) {
                // Preserve the existing 1D recovery behavior exactly.
                Map<String, HistoricalData> batchData = fetchHeatmapHistory(
                        symbolsNeedingHistory, from.minusDays(10), to.plusDays(1), interval,
                        isIndexBatch, bypassCache);
                startData = batchData;
                endData = batchData;
                batchCalls = 1;
            } else if (!startWindowFrom.isAfter(endWindowTo) && !endWindowFrom.isAfter(startWindowTo)) {
                // Nearby boundaries (notably 1W) can be served by one compact query.
                Map<String, HistoricalData> batchData = fetchHeatmapHistory(
                        symbolsNeedingHistory,
                        startWindowFrom.isBefore(endWindowFrom) ? startWindowFrom : endWindowFrom,
                        startWindowTo.isAfter(endWindowTo) ? startWindowTo : endWindowTo,
                        interval, isIndexBatch, bypassCache);
                startData = batchData;
                endData = batchData;
                batchCalls = 1;
            } else {
                // Long windows only need two verified boundaries: the last candle on
                // or before the baseline date and the latest candle on or before today.
                // Fetching the intervening months/years adds payload without changing
                // either value used by the existing return calculation.
                startData = fetchHeatmapHistory(symbolsNeedingHistory, startWindowFrom,
                        startWindowTo, interval, isIndexBatch, bypassCache);
                endData = fetchHeatmapHistory(symbolsNeedingHistory, endWindowFrom,
                        endWindowTo, interval, isIndexBatch, bypassCache);
                batchCalls = 2;
            }

            int resolved = 0;
            int omitted = 0;
            int candleCount = countCandles(startData);
            if (endData != startData) {
                candleCount += countCandles(endData);
            }
            for (String sym : symbolsNeedingHistory) {
                HistoricalData baselineHistory = findHeatmapHistory(startData, sym);
                HistoricalData endingHistory = findHeatmapHistory(endData, sym);
                Double startPrice = null;
                if (tfUpper.equals("1D")) {
                    List<OHLCVTPoint> points = sortedValidPoints(baselineHistory);
                    if (points.size() >= 2) {
                        startPrice = points.get(points.size() - 2).getClose();
                    }
                } else {
                    startPrice = findClosestPrice(sortedValidPoints(baselineHistory), targetStartDate);
                }
                Double endPrice = latestCloseOnOrBefore(sortedValidPoints(endingHistory), to);

                if (isValidPrice(endPrice) && isValidPrice(startPrice)) {
                    double change = (endPrice - startPrice) / startPrice * 100.0;
                    heatmap.put(sym, round2(change));
                    resolved++;
                } else {
                    omitted++;
                }
            }
            log.info("Heatmap history boundary resolution symbol={} timeframe={} requested={} resolved={} omitted={} candles={} batchCalls={} baselineWindow={}..{} endWindow={}..{} durationMs={}",
                    symbol, tfNorm, symbolsNeedingHistory.size(), resolved, omitted, candleCount, batchCalls,
                    startWindowFrom, startWindowTo, endWindowFrom, endWindowTo,
                    (System.nanoTime() - historyStartedAt) / 1_000_000);
        }

        // Sort by change desc?
        // Map is unordered, but UI might want sorted.
        // Let's return LinkedHashMap sorted
        Map<String, Double> sortedHeatmap = heatmap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new));

        // Validate the roster itself before using it as the coverage denominator. Then
        // tolerate a small number of unavailable members so one missing candle does not
        // force every Heatmap visit to repeat the same expensive batch query.
        int minRequired = com.am.marketdata.common.util.IndexMembershipUtils.minimumMembershipSize(symbol);
        boolean validRoster = !isConstituentIndex || symbols.size() >= minRequired;
        int threshold = isConstituentIndex
                ? Math.max(minRequired, (int) Math.ceil(symbols.size() * 0.85))
                : 1;
        boolean incompleteCoverage = isConstituentIndex && (!validRoster || sortedHeatmap.size() < threshold);
        boolean degenerate = isDegenerateIndicesHeatmap(tfNorm, sortedHeatmap);
        if (incompleteCoverage) {
            // Keep a prior validated cache entry; a temporary source gap must not
            // erase good data and cause every following request to recompute it.
            log.warn("Skipping incomplete heatmap cache save {} {} (validRoster={}, values={}/{}, required={})",
                    symbol, tfNorm, validRoster, sortedHeatmap.size(), symbols.size(), threshold);
        } else if (degenerate) {
            log.warn("Skipping cache save for degenerate heatmap {} {} (zeros={}/{}, count={}/{})",
                    symbol, tfNorm,
                    sortedHeatmap.values().stream().filter(v -> v != null && Math.abs(v) < 1e-9).count(),
                    sortedHeatmap.size(), sortedHeatmap.size(), symbols.size());
            analysisRedisCache.deleteIndexHeatmap(symbol, tfNorm);
        } else {
            analysisRedisCache.saveIndexHeatmap(symbol, tfNorm, sortedHeatmap);
        }
        return sortedHeatmap;
    }

    /**
     * A multi-member 1D Heatmap that is almost entirely zero usually means
     * incomplete history or a bad scrape day, not a flat market.
     */
    private boolean isDegenerateIndicesHeatmap(String timeframe,
            Map<String, Double> heatmap) {
        if (heatmap == null || heatmap.isEmpty()) {
            return false;
        }
        if (timeframe == null || !"1D".equalsIgnoreCase(timeframe.trim())) {
            return false;
        }
        long zeroCount = heatmap.values().stream()
                .filter(v -> v == null || Math.abs(v) < 1e-9)
                .count();
        // >70% exact zeros with at least 5 symbols → treat as bad cache / bad compute
        return heatmap.size() >= 5 && zeroCount * 10 >= heatmap.size() * 7;
    }

    private boolean isValidPrice(Double price) {
        return price != null && Double.isFinite(price) && price > 0.0;
    }

    private LocalDate CalculateTargetDate(LocalDate to, String tf) {
        if (tf.equals("1D"))
            return to.minusDays(1);
        if (tf.equals("5D") || tf.equals("1W"))
            return to.minusWeeks(1);
        if (tf.equals("1M"))
            return to.minusMonths(1);
        if (tf.equals("3M"))
            return to.minusMonths(3);
        if (tf.equals("6M"))
            return to.minusMonths(6);
        if (tf.equals("1Y"))
            return to.minusYears(1);
        if (tf.equals("3Y"))
            return to.minusYears(3);
        if (tf.equals("5Y"))
            return to.minusYears(5);
        return to.minusDays(1);
    }

    private Double findClosestPrice(List<OHLCVTPoint> points, LocalDate targetDate) {
        // A timeframe return must never use a future candle as its baseline. For
        // weekends and exchange holidays, the preceding completed session is the
        // authoritative base; if it is absent, omit the member instead of creating
        // an artificial return.
        return points.stream()
                .filter(point -> point != null && point.getTime() != null
                        && !point.getTime().toLocalDate().isAfter(targetDate)
                        && isValidPrice(point.getClose()))
                .max(Comparator.comparing(point -> point.getTime().toLocalDate()))
                .map(OHLCVTPoint::getClose)
                .orElse(null);
    }

    private Map<String, HistoricalData> fetchHeatmapHistory(List<String> symbols, LocalDate from, LocalDate to,
            TimeFrame interval, boolean isIndexBatch, boolean bypassCache) {
        return marketDataService.getHistoricalDataBatch(
                symbols,
                java.sql.Date.valueOf(from),
                java.sql.Date.valueOf(to),
                interval,
                true,
                null,
                null,
                isIndexBatch,
                bypassCache,
                // HTTP requests remain cache/database-only; scheduler bypass keeps its
                // current provider-backed repair behavior.
                bypassCache);
    }

    private HistoricalData findHeatmapHistory(Map<String, HistoricalData> data, String symbol) {
        if (data == null) {
            return null;
        }
        HistoricalData history = data.get(symbol);
        if (history == null && symbol.contains(":")) {
            history = data.get(symbol.substring(symbol.indexOf(":") + 1));
        }
        if (history == null) {
            history = data.get("NSE_EQ:" + symbol);
        }
        if (history == null) {
            history = data.get("NSE:" + symbol);
        }
        return history;
    }

    private List<OHLCVTPoint> sortedValidPoints(HistoricalData history) {
        if (history == null || history.getDataPoints() == null) {
            return Collections.emptyList();
        }
        return history.getDataPoints().stream()
                .filter(point -> point != null && point.getTime() != null && isValidPrice(point.getClose()))
                .sorted(Comparator.comparing(OHLCVTPoint::getTime))
                .toList();
    }

    private Double latestCloseOnOrBefore(List<OHLCVTPoint> points, LocalDate targetDate) {
        return points.stream()
                .filter(point -> !point.getTime().toLocalDate().isAfter(targetDate))
                .max(Comparator.comparing(point -> point.getTime().toLocalDate()))
                .map(OHLCVTPoint::getClose)
                .orElse(null);
    }

    private int countCandles(Map<String, HistoricalData> data) {
        if (data == null) {
            return 0;
        }
        return data.values().stream()
                .filter(Objects::nonNull)
                .filter(history -> history.getDataPoints() != null)
                .mapToInt(history -> history.getDataPoints().size())
                .sum();
    }
    // --- Indices Historical Performance ---

    public com.am.marketdata.common.model.analysis.IndicesHistoricalPerformanceResponse getIndicesHistoricalPerformance(
            int years) {
        return getIndicesHistoricalPerformance(years, false);
    }

    public com.am.marketdata.common.model.analysis.IndicesHistoricalPerformanceResponse getIndicesHistoricalPerformance(
            int years, boolean bypassCache) {

        // Check Cache
        if (!bypassCache) {
            com.am.marketdata.common.model.analysis.IndicesHistoricalPerformanceResponse cached = analysisRedisCache
                    .getIndicesHistoricalPerformance(years);
            if (cached != null) {
                return cached;
            }
        }

        // 1. Get List of Indices
        List<String> indices = getAllTrackedIndices();

        // 2. Data Structure to hold aggregates: Year -> Month (1-12) -> List of
        // Performances
        Map<Integer, Map<Integer, List<com.am.marketdata.common.model.analysis.IndexPerformance>>> aggregateMap = new TreeMap<>(
                Collections.reverseOrder());

        // 3. Iterate and Fetch
        for (String index : indices) {
            // Re-use existing method
            try {
                com.am.marketdata.common.model.analysis.HistoricalPerformanceResponse response = getHistoricalPerformance(
                        index, years, false, bypassCache);

                if (response != null && response.getYearlyPerformance() != null) {
                    for (com.am.marketdata.common.model.analysis.YearlyPerformance yp : response
                            .getYearlyPerformance()) {
                        int year = yp.getYear();
                        Map<String, Double> monthly = yp.getMonthlyReturns();

                        if (monthly != null) {
                            for (Map.Entry<String, Double> entry : monthly.entrySet()) {
                                String monthStr = entry.getKey(); // "JANUARY"
                                Double ret = entry.getValue();

                                try {
                                    Month m = Month.valueOf(monthStr);
                                    int monthVal = m.getValue();

                                    aggregateMap.computeIfAbsent(year, k -> new TreeMap<>())
                                            .computeIfAbsent(monthVal, k -> new ArrayList<>())
                                            .add(com.am.marketdata.common.model.analysis.IndexPerformance.builder()
                                                    .symbol(index)
                                                    .returnPercentage(ret)
                                                    .build());
                                } catch (IllegalArgumentException e) {
                                    log.warn("Invalid month string: {}", monthStr);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error fetching historical performance for index: {}", index, e);
            }
        }

        // 4. Transform to Response
        List<com.am.marketdata.common.model.analysis.MonthlyIndicesPerformance> performanceList = new ArrayList<>();

        for (Map.Entry<Integer, Map<Integer, List<com.am.marketdata.common.model.analysis.IndexPerformance>>> yearEntry : aggregateMap
                .entrySet()) {
            int year = yearEntry.getKey();

            for (Map.Entry<Integer, List<com.am.marketdata.common.model.analysis.IndexPerformance>> monthEntry : yearEntry
                    .getValue().entrySet()) {
                int month = monthEntry.getKey();
                List<com.am.marketdata.common.model.analysis.IndexPerformance> indicesPerf = monthEntry.getValue();

                if (indicesPerf.isEmpty())
                    continue;

                // Sort by return desc
                indicesPerf.sort((a, b) -> Double.compare(b.getReturnPercentage(), a.getReturnPercentage()));

                com.am.marketdata.common.model.analysis.IndexPerformance top = indicesPerf.get(0);
                com.am.marketdata.common.model.analysis.IndexPerformance worst = indicesPerf
                        .get(indicesPerf.size() - 1);

                performanceList.add(com.am.marketdata.common.model.analysis.MonthlyIndicesPerformance.builder()
                        .year(year)
                        .month(month)
                        .monthName(Month.of(month).toString())
                        .topPerformer(top)
                        .worstPerformer(worst)
                        .allIndices(indicesPerf)
                        .build());
            }
        }

        // Sort final list by Date Descending
        performanceList.sort((a, b) -> {
            if (a.getYear() != b.getYear()) {
                return b.getYear() - a.getYear();
            }
            return b.getMonth() - a.getMonth();
        });

        LocalDate to = LocalDate.now();
        int endYear = to.getYear();
        int startYear = endYear - years + 1;

        com.am.marketdata.common.model.analysis.IndicesHistoricalPerformanceResponse response = com.am.marketdata.common.model.analysis.IndicesHistoricalPerformanceResponse
                .builder()
                .startYear(startYear)
                .endYear(endYear)
                .monthlyPerformance(performanceList)
                .build();

        analysisRedisCache.saveIndicesHistoricalPerformance(response, years);
        return response;
    }

    /**
     * Top/worst constituent stock per month for a given index (Mode B Constituents tab).
     */
    public com.am.marketdata.common.model.analysis.IndicesHistoricalPerformanceResponse getConstituentsHistoricalPerformance(
            String indexSymbol, int years) {
        return getConstituentsHistoricalPerformance(indexSymbol, years, false);
    }

    public com.am.marketdata.common.model.analysis.IndicesHistoricalPerformanceResponse getConstituentsHistoricalPerformance(
            String indexSymbol, int years, boolean bypassCache) {
        String index = indexSymbol == null ? "" : indexSymbol.trim();
        if (index.isEmpty()) {
            return com.am.marketdata.common.model.analysis.IndicesHistoricalPerformanceResponse.builder()
                    .startYear(LocalDate.now().getYear() - years + 1)
                    .endYear(LocalDate.now().getYear())
                    .monthlyPerformance(Collections.emptyList())
                    .build();
        }

        if (!bypassCache) {
            com.am.marketdata.common.model.analysis.IndicesHistoricalPerformanceResponse cached = analysisRedisCache
                    .getConstituentsHistoricalPerformance(index, years);
            if (cached != null) {
                return cached;
            }
        }

        List<String> symbols = marketDataService.getIndexConstituents(index);
        if (symbols == null || symbols.isEmpty()) {
            symbols = Collections.singletonList(index);
        }

        Map<Integer, Map<Integer, List<com.am.marketdata.common.model.analysis.IndexPerformance>>> aggregateMap = new TreeMap<>(
                Collections.reverseOrder());

        for (String stock : symbols) {
            try {
                com.am.marketdata.common.model.analysis.HistoricalPerformanceResponse response = getHistoricalPerformance(
                        stock, years, false, bypassCache);

                if (response != null && response.getYearlyPerformance() != null) {
                    for (com.am.marketdata.common.model.analysis.YearlyPerformance yp : response
                            .getYearlyPerformance()) {
                        int year = yp.getYear();
                        Map<String, Double> monthly = yp.getMonthlyReturns();

                        if (monthly != null) {
                            for (Map.Entry<String, Double> entry : monthly.entrySet()) {
                                String monthStr = entry.getKey();
                                Double ret = entry.getValue();
                                if (ret == null)
                                    continue;

                                try {
                                    Month m = Month.valueOf(monthStr);
                                    int monthVal = m.getValue();

                                    aggregateMap.computeIfAbsent(year, k -> new TreeMap<>())
                                            .computeIfAbsent(monthVal, k -> new ArrayList<>())
                                            .add(com.am.marketdata.common.model.analysis.IndexPerformance.builder()
                                                    .symbol(stock)
                                                    .returnPercentage(ret)
                                                    .build());
                                } catch (IllegalArgumentException e) {
                                    log.warn("Invalid month string: {}", monthStr);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error fetching historical performance for constituent: {}", stock, e);
            }
        }

        List<com.am.marketdata.common.model.analysis.MonthlyIndicesPerformance> performanceList = new ArrayList<>();

        for (Map.Entry<Integer, Map<Integer, List<com.am.marketdata.common.model.analysis.IndexPerformance>>> yearEntry : aggregateMap
                .entrySet()) {
            int year = yearEntry.getKey();

            for (Map.Entry<Integer, List<com.am.marketdata.common.model.analysis.IndexPerformance>> monthEntry : yearEntry
                    .getValue().entrySet()) {
                int month = monthEntry.getKey();
                List<com.am.marketdata.common.model.analysis.IndexPerformance> stocksPerf = monthEntry.getValue();

                if (stocksPerf.isEmpty())
                    continue;

                stocksPerf.sort((a, b) -> Double.compare(b.getReturnPercentage(), a.getReturnPercentage()));

                com.am.marketdata.common.model.analysis.IndexPerformance top = stocksPerf.get(0);
                com.am.marketdata.common.model.analysis.IndexPerformance worst = stocksPerf
                        .get(stocksPerf.size() - 1);

                performanceList.add(com.am.marketdata.common.model.analysis.MonthlyIndicesPerformance.builder()
                        .year(year)
                        .month(month)
                        .monthName(Month.of(month).toString())
                        .topPerformer(top)
                        .worstPerformer(worst)
                        .allIndices(stocksPerf)
                        .build());
            }
        }

        performanceList.sort((a, b) -> {
            if (a.getYear() != b.getYear()) {
                return b.getYear() - a.getYear();
            }
            return b.getMonth() - a.getMonth();
        });

        LocalDate to = LocalDate.now();
        int endYear = to.getYear();
        int startYear = endYear - years + 1;

        com.am.marketdata.common.model.analysis.IndicesHistoricalPerformanceResponse response = com.am.marketdata.common.model.analysis.IndicesHistoricalPerformanceResponse
                .builder()
                .startYear(startYear)
                .endYear(endYear)
                .monthlyPerformance(performanceList)
                .build();

        analysisRedisCache.saveConstituentsHistoricalPerformance(response, index, years);
        return response;
    }

    private List<String> getAllTrackedIndices() {
        return Arrays.asList(
                "NIFTY 50",
                "NIFTY BANK",
                "NIFTY IT",
                "SENSEX",
                "NIFTY SMALLCAP 100",
                "NIFTY METAL",
                "NIFTY MIDCAP 100",
                "NIFTY INFRA",
                "NIFTY PHARMA",
                "NIFTY ENERGY",
                "NIFTY FMCG",
                "NIFTY AUTO",
                "NIFTY REALTY",
                "NIFTY PSU BANK", // Added common ones
                "NIFTY FIN SERVICE");
    }
}
