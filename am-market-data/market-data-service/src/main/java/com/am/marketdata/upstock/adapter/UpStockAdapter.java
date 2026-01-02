package com.am.marketdata.upstock.adapter;

import com.am.common.investment.model.equity.EquityPrice;
import com.am.marketdata.upstock.client.UpStockClient;
import com.am.marketdata.upstock.mapper.EquityStockMapper;
import com.am.marketdata.upstock.model.MarketQuoteResponse;
import com.am.marketdata.upstock.model.OHLCResponse;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UpStockAdapter {
    private static final Logger log = LoggerFactory.getLogger(UpStockAdapter.class);
    private final UpStockClient upStockClient;
    private final EquityStockMapper equityStockMapper;

    @Value("${upstox.interval}")
    private String interval;


    public List<EquityPrice> getStocks(List<String> symbols) {
        log.info("Fetching market quotes for {} symbols", symbols.size());
        
        MarketQuoteResponse response = upStockClient.getMarketQuotes(symbols);
        log.info("Successfully received market quotes. Processing response...");
        if (response == null || response.getData() == null) {
            log.warn("Received null response or null data from Upstox API");
            return List.of();
        }
        
        var stockQuotes = response.getData().values().stream()
            .collect(Collectors.toList());

        return equityStockMapper.getEquityPrices(stockQuotes);
    }

    public List<EquityPrice> getStocksOHLC(List<String> symbols) {
        log.info("Fetching market quotes for {} symbols", symbols.size());
        
        OHLCResponse response = upStockClient.getOHLCData(symbols, interval);
        log.info("Successfully received market quotes. Processing response...");
        if (response == null || response.getData() == null) {
            log.warn("Received null response or null data from Upstox API");
            return List.of();
        }

        return equityStockMapper.getEquityPricesByOHLC(response.getData());
    }
}