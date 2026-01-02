package com.am.marketdata.mapper;

import com.am.common.investment.model.equity.EquityPrice;
import com.am.common.investment.model.historical.OHLCVTPoint;
import com.zerodhatech.models.LTPQuote;
import com.zerodhatech.models.OHLCQuote;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mapper for converting OHLC data to EquityPrice objects
 */
@Slf4j
@Component
public class MarketDataGenericMapper {

    /**
     * Convert LTP data to EquityPrice objects
     *
     * @param ohlcData      Map of trading symbol to OHLC quote
     * @param instrumentMap Map of trading symbol to Instrument
     * @return List of EquityPrice objects
     */
    public List<EquityPrice> mapLTPquoteToEquityPrices(Map<String, LTPQuote> ltpData) {
        List<EquityPrice> equityPrices = new ArrayList<>();

        if (ltpData == null || ltpData.isEmpty()) {
            log.warn("No OHLC data to map");
            return equityPrices;
        }

        for (Map.Entry<String, LTPQuote> entry : ltpData.entrySet()) {
            String key = entry.getKey();
            LTPQuote quote = entry.getValue();

            try {
                // Parse the key to extract exchange and symbol
                String exchange = "NSE"; // Default exchange
                String symbol = key;

                // If key contains a colon, split it to get exchange and symbol
                if (key.contains(":")) {
                    String[] parts = key.split(":", 2);
                    exchange = parts[0];
                    symbol = parts[1];
                }

                // Create and populate EquityPrice object
                EquityPrice price = new EquityPrice();
                price.setSymbol(symbol);
                price.setLastPrice(quote.lastPrice);
                price.setExchange(exchange);
                equityPrices.add(price);
            } catch (Exception e) {
                log.error("Error mapping LTP data for symbol {}: {}", key, e.getMessage(), e);
            }
        }

        log.info("Mapped {} LTP quotes to equity prices", equityPrices.size());
        return equityPrices;
    }

    /**
     * Convert LTP data to EquityPrice objects
     *
     * @param ohlcData      Map of trading symbol to OHLC quote
     * @param instrumentMap Map of trading symbol to Instrument
     * @return List of EquityPrice objects
     */
    public List<EquityPrice> mapOHLCquoteToEquityPrices(Map<String, OHLCQuote> ohlcData) {
        List<EquityPrice> equityPrices = new ArrayList<>();

        if (ohlcData == null || ohlcData.isEmpty()) {
            log.warn("No OHLC data to map");
            return equityPrices;
        }

        for (Map.Entry<String, OHLCQuote> entry : ohlcData.entrySet()) {
            String key = entry.getKey();
            OHLCQuote quote = entry.getValue();

            try {
                // Parse the key to extract exchange and symbol
                String exchange = "NSE"; // Default exchange
                String symbol = key;

                // If key contains a colon, split it to get exchange and symbol
                if (key.contains(":")) {
                    String[] parts = key.split(":", 2);
                    exchange = parts[0];
                    symbol = parts[1];
                }

                // Create and populate EquityPrice object
                EquityPrice price = new EquityPrice();
                price.setSymbol(symbol);
                price.setOhlcv(OHLCVTPoint.builder().open(quote.ohlc.open).high(quote.ohlc.high).low(quote.ohlc.low)
                        .close(quote.ohlc.close).build());
                price.setExchange(exchange);
                equityPrices.add(price);
            } catch (Exception e) {
                log.error("Error mapping OHLC data for symbol {}: {}", key, e.getMessage(), e);
            }
        }

        log.info("Mapped {} OHLC quotes to equity prices", equityPrices.size());
        return equityPrices;
    }

}
