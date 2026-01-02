package com.am.marketdata.redis.service;

import static com.am.marketdata.common.constants.TimeIntervalConstants.*;

import com.am.marketdata.redis.model.OHLCV;
import com.am.marketdata.redis.util.BarCalculatorUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for calculating OHLCV bars from raw price data
 */
@Service
public class BarCalculator {

    /**
     * Calculate OHLCV bars for a given interval
     * 
     * @param prices   List of price points
     * @param interval The interval (5m, 15m, 30m, 1h, 4h, 1d)
     * @param date     The date for which to calculate bars
     * @return List of calculated OHLCV bars
     */
    public List<OHLCV> calculateBars(List<OHLCV> prices, String interval, LocalDate date) {
        if (prices == null || prices.isEmpty()) {
            return Collections.emptyList();
        }

        BarCalculatorUtil.validateInterval(interval);

        // Sort prices by timestamp
        prices.sort(Comparator.comparing(OHLCV::getTime));

        if (interval.equals(INTERVAL_1_DAY)) {
            return calculateDailyBar(prices, date);
        } else {
            return calculateIntradayBars(prices, interval, date);
        }
    }

    /**
     * Calculate intraday bars (5m, 15m, 30m, 1h, 4h)
     */
    private List<OHLCV> calculateIntradayBars(List<OHLCV> prices, String interval, LocalDate date) {
        int minutes = BarCalculatorUtil.INTERVAL_MINUTES.get(interval);
        Map<LocalDateTime, List<OHLCV>> groupedPrices = BarCalculatorUtil.groupPricesByInterval(prices, minutes, date);

        List<OHLCV> bars = new ArrayList<>();

        for (Map.Entry<LocalDateTime, List<OHLCV>> entry : groupedPrices.entrySet()) {
            LocalDateTime barTime = entry.getKey();
            List<OHLCV> barPrices = entry.getValue();

            if (!barPrices.isEmpty()) {
                bars.add(BarCalculatorUtil.createBar(barPrices, barTime));
            }
        }

        return bars;
    }

    /**
     * Calculate a single daily bar
     */
    private List<OHLCV> calculateDailyBar(List<OHLCV> prices, LocalDate date) {
        LocalDateTime barTime = date.atStartOfDay();
        return Collections.singletonList(BarCalculatorUtil.createBar(prices, barTime));
    }
}
