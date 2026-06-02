package com.am.marketdata.provider.upstox.adapter;

import com.am.common.investment.model.equity.EquityPrice;
import com.am.marketdata.provider.upstox.client.UpStockClient;
import com.am.marketdata.provider.upstox.mapper.EquityStockMapper;
import com.am.marketdata.provider.upstox.model.MarketQuoteResponse;
import com.am.marketdata.provider.upstox.model.OHLCResponse;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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


    // Upstox API has a hard limit of 500 instrument keys per request.
    private static final int BATCH_SIZE = 500;
    
    // Delay between batch requests to prevent triggering HTTP 429 Rate Limits from Upstox.
    private static final int BATCH_DELAY_MS = 150;

    public List<EquityPrice> getStocks(List<String> symbols) {
        log.info("Fetching market quotes for {} symbols in batches of {}", symbols.size(), BATCH_SIZE);
        List<EquityPrice> allPrices = new ArrayList<>();
        
        for (int i = 0; i < symbols.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, symbols.size());
            List<String> batch = symbols.subList(i, end);
            log.info("Fetching market quotes batch: {} to {}", i, end);
            
            try {
                MarketQuoteResponse response = upStockClient.getMarketQuotes(batch);
                if (response != null && response.getData() != null) {
                    var stockQuotes = response.getData().values().stream()
                        .collect(Collectors.toList());
                    allPrices.addAll(equityStockMapper.getEquityPrices(stockQuotes));
                } else {
                    log.warn("Received null response or null data from Upstox API for batch {}-{}", i, end);
                }
                
                // Add a delay between batches to respect rate limits, but not after the final batch
                if (end < symbols.size()) {
                    Thread.sleep(BATCH_DELAY_MS);
                }
            } catch (InterruptedException ie) {
                log.error("Batching sleep interrupted", ie);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Error fetching market quotes batch {}-{}: {}", i, end, e.getMessage(), e);
            }
        }
        
        return allPrices;
    }

    public List<EquityPrice> getStocksOHLC(List<String> symbols) {
        log.info("Fetching OHLC quotes for {} symbols in batches of {}", symbols.size(), BATCH_SIZE);
        List<EquityPrice> allPrices = new ArrayList<>();
        
        for (int i = 0; i < symbols.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, symbols.size());
            List<String> batch = symbols.subList(i, end);
            log.info("Fetching OHLC batch: {} to {}", i, end);
            
            try {
                OHLCResponse response = upStockClient.getOHLCData(batch, interval);
                if (response != null && response.getData() != null) {
                    allPrices.addAll(equityStockMapper.getEquityPricesByOHLC(response.getData()));
                } else {
                    log.warn("Received null response or null data from Upstox API for batch {}-{}", i, end);
                }
                
                // Add a delay between batches to respect rate limits, but not after the final batch
                if (end < symbols.size()) {
                    Thread.sleep(BATCH_DELAY_MS);
                }
            } catch (InterruptedException ie) {
                log.error("Batching sleep interrupted", ie);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Error fetching OHLC batch {}-{}: {}", i, end, e.getMessage(), e);
            }
        }

        return allPrices;
    }
}