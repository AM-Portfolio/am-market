package com.am.marketdata.api.service;

import com.am.marketdata.common.model.MarketDataUpdate;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.service.MarketDataPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to generate simulated ticking market data updates for after-hours testing.
 * Reads real-time baseline quotes strictly from local cache/database if available before falling back to predefined offsets.
 * Strictly decoupled from external provider HTTP/SDK networks to ensure fast, offline simulation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataMockService {

    private final MarketDataPersistenceService persistenceService;

    private final Map<String, MarketDataUpdate.QuoteChange> mockQuotes = new ConcurrentHashMap<>();
    private final Random random = new Random();

    /**
     * Initializes base quotes using local database/cache snapshot if available.
     * Bypasses external provider calls.
     *
     * @param symbols Symbols to initialize
     */
    public void initializeMockQuotes(Set<String> symbols) {
        mockQuotes.clear();
        try {
            // Retrieve OHLC quote baseline strictly from local cache/database (forceRefresh = false)
            Map<String, OHLCQuote> ohlcMap = persistenceService.getOHLCData(
                    new ArrayList<>(symbols),
                    TimeFrame.DAY,
                    false
            );
            if (ohlcMap != null) {
                for (Map.Entry<String, OHLCQuote> entry : ohlcMap.entrySet()) {
                    String symbol = entry.getKey();
                    OHLCQuote quote = entry.getValue();
                    if (quote != null && quote.getLastPrice() > 0) {
                        double lastPrice = quote.getLastPrice();
                        double prevClose = quote.getPreviousClose() > 0 ? quote.getPreviousClose() : lastPrice;
                        double open = quote.getOhlc() != null ? quote.getOhlc().getOpen() : prevClose;
                        double high = quote.getOhlc() != null ? quote.getOhlc().getHigh() : lastPrice;
                        double low = quote.getOhlc() != null ? quote.getOhlc().getLow() : lastPrice;

                        MarketDataUpdate.QuoteChange quoteChange = MarketDataUpdate.QuoteChange.builder()
                                .lastPrice(lastPrice)
                                .open(open)
                                .high(high)
                                .low(low)
                                .close(prevClose)
                                .previousClose(prevClose)
                                .change(lastPrice - prevClose)
                                .changePercent(((lastPrice - prevClose) / prevClose) * 100)
                                .build();
                        mockQuotes.put(symbol, quoteChange);
                    }
                }
                log.info("Initialized mock quotes for {}/{} symbols from local cache/db snapshot", mockQuotes.size(), symbols.size());
            }
        } catch (Exception e) {
            log.error("Failed to initialize mock quotes from snapshot, using defaults", e);
        }
    }

    /**
     * Generates standard ticking update with random fluctuations (±0.5% per tick)
     *
     * @param symbols Set of symbols to mock
     * @return MarketDataUpdate object
     */
    public MarketDataUpdate generateMockUpdate(Set<String> symbols) {
        Map<String, MarketDataUpdate.QuoteChange> updates = new HashMap<>();
        long timestamp = System.currentTimeMillis();

        for (String symbol : symbols) {
            MarketDataUpdate.QuoteChange lastQuote = mockQuotes.get(symbol);
            double lastPrice;
            double prevClose;
            double open;
            double high;
            double low;

            if (lastQuote != null) {
                lastPrice = lastQuote.getLastPrice();
                prevClose = lastQuote.getPreviousClose();
                open = lastQuote.getOpen();
                high = lastQuote.getHigh();
                low = lastQuote.getLow();
            } else {
                prevClose = getBasePriceForSymbol(symbol);
                lastPrice = prevClose * (1 + (random.nextDouble() - 0.5) * 0.01);
                open = prevClose;
                high = Math.max(open, lastPrice);
                low = Math.min(open, lastPrice);
            }

            // Apply random walk fluctuation (±0.5% per tick for visible mock streaming)
            double pctChange = (random.nextDouble() - 0.5) * 0.01;
            lastPrice = lastPrice * (1 + pctChange);
            lastPrice = BigDecimal.valueOf(lastPrice).setScale(2, RoundingMode.HALF_UP).doubleValue();

            // Recalculate change metrics
            if (prevClose <= 0) {
                prevClose = lastPrice;
            }
            double change = lastPrice - prevClose;
            double changePercent = (change / prevClose) * 100;
            change = BigDecimal.valueOf(change).setScale(2, RoundingMode.HALF_UP).doubleValue();
            changePercent = BigDecimal.valueOf(changePercent).setScale(2, RoundingMode.HALF_UP).doubleValue();

            // Update session High and Low
            high = Math.max(high, lastPrice);
            low = Math.min(low, lastPrice);

            MarketDataUpdate.QuoteChange newQuote = MarketDataUpdate.QuoteChange.builder()
                    .lastPrice(lastPrice)
                    .open(open)
                    .high(high)
                    .low(low)
                    .close(prevClose)
                    .previousClose(prevClose)
                    .change(change)
                    .changePercent(changePercent)
                    .build();

            mockQuotes.put(symbol, newQuote);
            updates.put(symbol, newQuote);
        }

        return MarketDataUpdate.builder()
                .timestamp(timestamp)
                .quotes(updates)
                .build();
    }

    private double getBasePriceForSymbol(String symbol) {
        if (symbol == null) return 100.00;

        // 1. Try to fetch from local Cache/Database strictly
        try {
            Map<String, OHLCQuote> ohlcMap = persistenceService.getOHLCData(
                    Collections.singletonList(symbol),
                    TimeFrame.DAY,
                    false
            );
            if (ohlcMap != null && ohlcMap.containsKey(symbol)) {
                OHLCQuote quote = ohlcMap.get(symbol);
                if (quote != null && quote.getLastPrice() > 0) {
                    log.info("Loaded baseline price for {} from local cache/db: {}", symbol, quote.getLastPrice());
                    return quote.getLastPrice();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve cached baseline price for symbol {}: {}", symbol, e.getMessage());
        }

        // 2. Fall back to hardcoded switch-case values if no cached data exists
        String upper = symbol.toUpperCase();
        if (upper.contains("NIFTY 50")) return 22400.00;
        if (upper.contains("NIFTY BANK") || upper.contains("BANKNIFTY")) return 48000.00;
        if (upper.contains("NIFTY IT")) return 35000.00;
        if (upper.contains("SENSEX")) return 73500.00;
        if (upper.contains("RELIANCE")) return 2900.00;
        if (upper.contains("TCS")) return 3800.00;
        if (upper.contains("INFY")) return 1450.00;
        if (upper.contains("HDFCBANK")) return 1500.00;
        if (upper.contains("ICICIBANK")) return 1100.00;
        if (upper.contains("SBIN")) return 800.00;
        return 200.00 + random.nextDouble() * 800.00;
    }
}
