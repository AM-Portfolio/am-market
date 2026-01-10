package com.am.marketdata.analysis.service;

import com.am.marketdata.common.model.analysis.CalendarHeatmapResponse;
import com.am.marketdata.common.model.analysis.SeasonalityResponse;
import com.am.marketdata.common.model.analysis.TechnicalAnalysisResponse;
import com.am.marketdata.redis.cache.AnalysisRedisCache;
import com.am.common.investment.model.historical.HistoricalData;
import com.am.common.investment.model.historical.OHLCVTPoint;
import com.am.marketdata.common.model.TimeFrame;
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

        LocalDate to = LocalDate.now();
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
                dayOfWeekReturns.computeIfAbsent(curr.getTime().getDayOfWeek(), k -> new ArrayList<>()).add(ret);
                monthlyReturns.computeIfAbsent(curr.getTime().getMonthValue(), k -> new ArrayList<>()).add(ret);
            }
        }

        Map<String, Double> avgDayOfWeek = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            if (dayOfWeekReturns.containsKey(day)) {
                avgDayOfWeek.put(day.toString(),
                        dayOfWeekReturns.get(day).stream().mapToDouble(d -> d).average().orElse(0.0));
            }
        }

        Map<String, Double> avgMonth = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            if (monthlyReturns.containsKey(m)) {
                avgMonth.put(Month.of(m).toString(),
                        monthlyReturns.get(m).stream().mapToDouble(d -> d).average().orElse(0.0));
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
        double currentPrice = n > 0 ? closes.get(n - 1) : 0.0;

        Double sma20 = calculateSMA(closes, 20);
        Double sma50 = calculateSMA(closes, 50);
        Double sma200 = calculateSMA(closes, 200);
        Double rsi14 = calculateRSI(closes, 14);

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
                        responseData.get(curr.getMonth().toString()).put(curr.getDayOfMonth(), change);
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
}
