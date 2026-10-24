package com.am.common.investment.service;

import com.am.common.investment.model.equity.EquityPrice;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EquityService {
    // Price methods
    void savePrice(EquityPrice price);
    
    void saveAllPrices(List<EquityPrice> prices);

    Optional<EquityPrice> getLatestPriceByKey(String key);
    List<EquityPrice> getPriceHistoryByKey(String key, Instant startTime, Instant endTime);
    List<EquityPrice> getPriceHistoryByKey(String key, Instant startTime, Instant endTime, String interval);
    default List<EquityPrice> getPriceHistoryBySymbols(List<String> symbols, Instant startTime, Instant endTime, String interval) {
        if (symbols == null || symbols.isEmpty()) return List.of();
        return symbols.stream()
                .filter(java.util.Objects::nonNull)
                .flatMap(symbol -> getPriceHistoryByKey(symbol, startTime, endTime, interval).stream())
                .toList();
    }
    List<EquityPrice> getPricesByExchange(String exchange);
    List<EquityPrice> getPricesByTradingSymbols(List<String> tradingSymbols);
    List<EquityPrice> getPricesByIsin(List<String> isins);
}
