package com.am.marketdata.upstock.mapper;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.am.common.investment.model.equity.EquityPrice;
import com.am.common.investment.model.historical.OHLCVTPoint;
import com.am.marketdata.upstock.model.OHLCResponse.OHLCData;
import com.am.marketdata.upstock.model.common.StockQuote;

@Component
public class EquityStockMapper {
    

    public List<EquityPrice> getEquityPrices(List<StockQuote> marketQuotes) {
        if (marketQuotes == null || marketQuotes.isEmpty()) {
            return List.of();
        }
        return marketQuotes.stream().map(this::getEquityPrice).collect(Collectors.toList());
    }

    public EquityPrice getEquityPrice(StockQuote stockQuote) {
        return EquityPrice.builder()
            .symbol(stockQuote.getSymbol())
            .ohlcv(OHLCVTPoint.builder().open(stockQuote.getOpenPrice()).high(stockQuote.getHighPrice()).low(stockQuote.getLowPrice()).close(stockQuote.getClosePrice()).volume(stockQuote.getVolume()).build())
            .time(ZonedDateTime.now().toInstant())
            .build();
    }

    public List<EquityPrice> getEquityPricesByOHLC(Map<String, OHLCData> ohlcResponses) {
        if (ohlcResponses == null || ohlcResponses.isEmpty()) {
            return List.of();
        }
        
        var equityPrices = new ArrayList<EquityPrice>();
        for (var entry : ohlcResponses.entrySet()) {
            equityPrices.add(getEquityPriceByOHLC(entry.getKey(), entry.getValue()));
        }
        return equityPrices;
    }

    public EquityPrice getEquityPriceByOHLC(String symbol, OHLCData ohlcData) {
        var exchange = symbol.substring(0, 6);
        var extractedSymbol = getSymbol(symbol);
        return EquityPrice.builder()
        .exchange(exchange)
        .isin(ohlcData.getISIN())
            .symbol(extractedSymbol)
            .ohlcv(OHLCVTPoint.builder().open(ohlcData.getOpen()).high(ohlcData.getHigh()).low(ohlcData.getLow()).close(ohlcData.getClose()).build())
            .time(ZonedDateTime.now().toInstant())
            .build();
    }

    public String getSymbol(String symbol) {
        if (symbol == null) return null;
        String[] parts = symbol.split("\\:");
        return parts.length > 1 ? parts[1] : null;
    }

}
