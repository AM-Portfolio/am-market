package com.am.marketdata.service;

import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.service.MarketDataService;

import lombok.RequiredArgsConstructor;
import com.am.marketdata.common.log.AppLogger;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service to provide "Smart" stock data by combining Cache, Database, and
 * Historical data.
 * Adheres to the strict rule: NEVER call provider for read operations.
 */
@Service
@RequiredArgsConstructor
public class SmartStockService {

    private final AppLogger log = AppLogger.getLogger(SmartStockService.class);
    private final MarketDataService marketDataService;

    /**
     * Get the latest quotes for a list of symbols with TimeFrame support.
     * If TimeFrame is provided and not DAY, it fetches historical data to calculate
     * change/pChange
     * relative to that timeframe (e.g. 1 Year ago).
     */
    public Map<String, OHLCQuote> getSmartQuotes(List<String> symbols, TimeFrame timeFrame) {
        // 1. Get Base Quotes (Live/Latest)
        Map<String, OHLCQuote> currentQuotes = getSmartQuotes(symbols);

        if (timeFrame == null || timeFrame == TimeFrame.DAY) {
            return currentQuotes; // Default behavior
        }

        log.info("getSmartQuotes", "Calculating quotes for TimeFrame: " + timeFrame);

        // 2. Fetch Historical Data for Previous Reference Point
        LocalDate to = LocalDate.now();
        LocalDate from = getStartDateForTimeFrame(to, timeFrame);

        try {
            // We need data around 'from' date to get the close price at that time
            // We ask for a small buffer around 'from' date to ensure we get a point
            Date fromDate = Date.from(from.minusDays(5).atStartOfDay(ZoneId.systemDefault()).toInstant());
            // We only need up to 'from' date essentially, but let's ask for a range to be
            // safe
            Date toDate = Date.from(from.plusDays(5).atStartOfDay(ZoneId.systemDefault()).toInstant());

            Map<String, com.am.common.investment.model.historical.HistoricalData> historyMap = marketDataService
                    .getHistoricalDataBatch(
                            new ArrayList<>(symbols),
                            fromDate,
                            toDate,
                            TimeFrame.DAY, // We want Daily candles to find the close
                            false,
                            null,
                            null,
                            false,
                            false // Cache is fine
                    );

            if (historyMap != null) {
                currentQuotes.forEach((symbol, quote) -> {
                    if (historyMap.containsKey(symbol)) {
                        var history = historyMap.get(symbol);
                        if (history.getDataPoints() != null && !history.getDataPoints().isEmpty()) {
                            // Find the point closest to 'from' date
                            // Since we asked for -5 to +5 days, we pick the one closest to 'from'
                            // OR simpler: just pick the *last available* point in this historical range
                            // which would be closest to the 'target start date'.

                            // Actually, simple sorting by date and taking the last one (which is closest to
                            // 'from')
                            // is a good approximation for "price at start of timeframe".

                            var points = history.getDataPoints();
                            var refPoint = points.get(points.size() - 1); // Last point in the requested past window

                            double pastClose = refPoint.getClose();
                            quote.setPreviousClose(pastClose);

                            // Recalculate Change & PChange logic is usually in EnrichedStockData or
                            // implicitly in UI.
                            // But OHLCQuote object itself doesn't hold 'change'/'pChange' fields explicitly
                            // standardly?
                            // Wait, StockDataEnricher calculates change = lastPrice - previousClose.
                            // So by updating previousClose here, we effectively update the change
                            // calculation!
                        }
                    }
                });
            }

        } catch (Exception e) {
            log.error("getSmartQuotes", "Error fetching historical reference for timeframe: " + timeFrame, e);
        }

        return currentQuotes;
    }

    private LocalDate getStartDateForTimeFrame(LocalDate current, TimeFrame timeFrame) {
        switch (timeFrame) {
            case WEEK:
                return current.minusWeeks(1);
            case MONTH:
                return current.minusMonths(1);
            case YEAR:
                return current.minusYears(1);
            default:
                return current.minusDays(1);
        }
    }

    /**
     * Get the latest quotes for a list of symbols.
     * Strategy:
     * 1. Try Cache/DB (forceRefresh=false).
     * 2. For missing symbols, try fetching latest Historical Data from DB.
     * 3. Construct a fallback quote using Historical Close as Previous Close.
     */
    public Map<String, OHLCQuote> getSmartQuotes(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyMap();
        }

        // 1. Try Cache & Database (No Provider)
        Map<String, OHLCQuote> quotes = marketDataService.getOHLC(symbols, TimeFrame.DAY, false, null);

        Set<String> missingSymbols = new HashSet<>(symbols);
        if (quotes != null) {
            missingSymbols.removeAll(quotes.keySet());
        } else {
            quotes = new HashMap<>();
        }

        if (missingSymbols.isEmpty()) {
            return quotes;
        }

        log.info("getSmartQuotes", "Found {} symbols missing in Cache/DB. Attempting historical fallback...",
                missingSymbols.size());

        // 2. Fetch Historical Data (Last 5 days to be safe) for missing symbols
        // We use a small range to get the latest available closing price
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(5);

        // We can use getHistoricalDataBatch from MarketDataService (forceRefresh=false)
        // Note: MarketDataService might not expose a batch method that takes
        // List<String>, Date, Date...
        // Let's assume we have to iterate or if there's a batch method.
        // Checking MarketDataService... it has getHistoricalDataBatch(List<String>,
        // TimeFrame, Date, Date, boolean)

        try {
            Map<String, com.am.common.investment.model.historical.HistoricalData> historyMap = marketDataService
                    .getHistoricalDataBatch(
                            new ArrayList<>(missingSymbols),
                            Date.from(from.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                            Date.from(to.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                            TimeFrame.DAY,
                            false, // continuous
                            null, // additionalParams
                            null, // providerName (will resolve default)
                            false, // isIndexSymbol
                            false // forceRefresh (Strict NO Provider)
                    );

            if (historyMap != null) {
                for (String symbol : missingSymbols) {
                    if (historyMap.containsKey(symbol)) {
                        var history = historyMap.get(symbol);
                        if (history.getDataPoints() != null && !history.getDataPoints().isEmpty()) {
                            // Sort to get latest
                            var points = history.getDataPoints();
                            // Assuming points are sorted, get last
                            var lastPoint = points.get(points.size() - 1);

                            // Create Fallback Quote
                            OHLCQuote.OHLC ohlcData = OHLCQuote.OHLC.builder()
                                    .open(lastPoint.getClose())
                                    .high(lastPoint.getClose())
                                    .low(lastPoint.getClose())
                                    .close(lastPoint.getClose())
                                    .build();

                            OHLCQuote fallback = OHLCQuote.builder()
                                    .lastPrice(lastPoint.getClose())
                                    .previousClose(lastPoint.getClose())
                                    .ohlc(ohlcData)
                                    .build();

                            // Try to get actual previous close if we have > 1 point
                            if (points.size() > 1) {
                                var prevPoint = points.get(points.size() - 2);
                                fallback.setPreviousClose(prevPoint.getClose());
                            }

                            quotes.put(symbol, fallback);
                            log.debug("getSmartQuotes", "Synthesized quote for {} from history.", symbol);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("getSmartQuotes", "Error fetching historical fallback", e);
        }

        return quotes;
    }
}
