package com.am.marketdata.scheduler.service;

import com.am.common.investment.model.historical.HistoricalData;
import com.am.common.investment.model.historical.OHLCVTPoint;
import com.am.marketdata.common.log.AppLogger;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.service.MarketDataService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fetches previous-close prices across multiple timeframes for a given symbol.
 *
 * <p>Called by {@code PreviousCloseScheduler} after the existing 1D OHLC fetch,
 * to build the full 7-timeframe payload published to the Kafka topic
 * {@code am-previous-close-snapshot}.</p>
 *
 * <p><b>Cache strategy</b>: By 8:00 AM, the 7:15 AM historical sync has already
 * populated Redis and MongoDB with daily candle data for all tracked symbols.
 * {@code getHistoricalData()} on the provider uses the cache → DB → provider
 * fallback chain, so the vast majority of calls are served from cache without
 * hitting the Upstox API.</p>
 */
@Service
public class PreviousCloseMultiTimeframeService {

    private static final String IST = "Asia/Kolkata";

    /** Days of look-back buffer to absorb weekends and market holidays. */
    private static final int LOOKBACK_BUFFER_DAYS = 10;

    /**
     * Ordered map: timeframe label → calendar days to look back from today.
     * "1D" is intentionally absent — provided by the caller from the OHLC response.
     */
    private static final Map<String, Integer> TIMEFRAME_DAYS = new LinkedHashMap<>();

    static {
        TIMEFRAME_DAYS.put("1W", 7);
        TIMEFRAME_DAYS.put("1M", 30);
        TIMEFRAME_DAYS.put("3M", 90);
        TIMEFRAME_DAYS.put("6M", 180);
        TIMEFRAME_DAYS.put("1Y", 365);
        TIMEFRAME_DAYS.put("5Y", 1825);
    }

    private final AppLogger log = AppLogger.getLogger();
    private final MarketDataService marketDataService;

    public PreviousCloseMultiTimeframeService(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    /**
     * Builds the full previous-close map for all 7 timeframes.
     *
     * @param symbol      trading symbol (e.g. {@code "RELIANCE"}, {@code "NIFTY 50"})
     * @param oneDayClose the 1D previous-close already fetched by the OHLC call
     * @return ordered map {@code {"1D": x, "1W": y, ..., "5Y": z}};
     *         individual values are {@code null} when data is unavailable
     */
    public Map<String, Double> buildPreviousCloseMap(String symbol, double oneDayClose) {
        Map<String, Double> result = new LinkedHashMap<>();
        result.put("1D", oneDayClose);

        LocalDate today = LocalDate.now(ZoneId.of(IST));

        for (Map.Entry<String, Integer> entry : TIMEFRAME_DAYS.entrySet()) {
            String label = entry.getKey();
            int daysBack = entry.getValue();
            result.put(label, fetchCloseForDate(symbol, today, daysBack, label));
        }
        return result;
    }

    /**
     * Returns the closing price of the trading candle nearest to
     * {@code today - daysBack}, or {@code null} if unavailable.
     *
     * <p>A ±{@value #LOOKBACK_BUFFER_DAYS}-day window is used around the target
     * date to account for weekends and market holidays. The last data point in the
     * ascending-ordered result (i.e. the most recent date ≤ target date) is chosen.</p>
     */
    private Double fetchCloseForDate(String symbol, LocalDate today, int daysBack, String label) {
        try {
            LocalDate targetDate = today.minusDays(daysBack);
            Date from = toDate(targetDate.minusDays(LOOKBACK_BUFFER_DAYS));
            Date to   = toDate(targetDate.plusDays(1)); // inclusive of target date

            HistoricalData data = marketDataService.getHistoricalData(
                    symbol, from, to, TimeFrame.DAY, false, null, "upstox");

            if (data == null || data.getDataPoints() == null || data.getDataPoints().isEmpty()) {
                log.warn("PreviousCloseMultiTimeframeService",
                        "No historical data for symbol=" + symbol + " timeframe=" + label);
                return null;
            }

            // Data points are in ascending date order — last point is closest to targetDate
            List<OHLCVTPoint> points = data.getDataPoints();
            OHLCVTPoint closest = points.get(points.size() - 1);
            return closest.getClose();

        } catch (Exception e) {
            log.warn("PreviousCloseMultiTimeframeService",
                    "Failed to fetch " + label + " previousClose for symbol=" + symbol
                            + ": " + e.getMessage());
            return null;
        }
    }

    private Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.of(IST)).toInstant());
    }
}
