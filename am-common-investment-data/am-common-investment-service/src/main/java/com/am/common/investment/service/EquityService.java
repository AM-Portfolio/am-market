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
    List<EquityPrice> getPricesByExchange(String exchange);
    List<EquityPrice> getPricesByTradingSymbols(List<String> tradingSymbols);
    List<EquityPrice> getPricesByIsin(List<String> isins);
}
