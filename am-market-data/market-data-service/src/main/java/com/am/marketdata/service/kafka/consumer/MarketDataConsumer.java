package com.am.marketdata.service.kafka.consumer;

import com.am.common.investment.model.equity.EquityPrice;
import com.am.common.investment.model.events.EquityPriceUpdateEvent;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.mapper.OHLCMapper;
import com.am.marketdata.service.MarketDataPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataConsumer {

    private final MarketDataPersistenceService persistenceService;
    // private final OHLCMapper ohlcMapper; // unused

    /**
     * Consumes equity price updates from Kafka
     * 
     * @param event The equity price update event
     */
    @KafkaListener(topics = "${kafka.topics.stock-price}", groupId = "${kafka.consumer.group-id:market-data-service-group}")
    public void consumeEquityPriceUpdates(EquityPriceUpdateEvent event) {
        try {
            if (event == null || event.getEquityPrices() == null || event.getEquityPrices().isEmpty()) {
                return;
            }

            log.debug("Received {} equity price updates from Kafka", event.getEquityPrices().size());

            // Convert back to OHLCQuote for PersistenceService if needed OR
            // PersistenceService might have a method for EquityPrice or we convert.
            // MarketDataPersistenceService.saveOHLCData takes Map<String, OHLCQuote>.
            // So we need to map List<EquityPrice> -> Map<String, OHLCQuote>.

            // Or better, let's see if PersistenceService has a method for EquityPrice
            // persistence.
            // It has: equityService.saveAllPrices(equityPrices).
            // But it also does CACHING using OHLCQuote.
            // So mapping back to OHLCQuote is safer to reuse the unified Logic.

            Map<String, OHLCQuote> quotes = new HashMap<>();
            for (EquityPrice price : event.getEquityPrices()) {
                if (price.getSymbol() != null) {
                    // Reverse mapping manually or via mapper if exists
                    OHLCQuote quote = new OHLCQuote();
                    quote.setLastPrice(price.getLastPrice());
                    if (price.getOhlcv() != null) {
                        OHLCQuote.OHLC ohlc = new OHLCQuote.OHLC();
                        ohlc.setOpen(price.getOhlcv().getOpen());
                        ohlc.setHigh(price.getOhlcv().getHigh());
                        ohlc.setLow(price.getOhlcv().getLow());
                        ohlc.setClose(price.getOhlcv().getClose());
                        quote.setOhlc(ohlc);
                    }
                    quotes.put(price.getSymbol(), quote);
                }
            }

            persistenceService.saveOHLCData(quotes);

        } catch (Exception e) {
            log.error("Error processing equity price update event", e);
        }
    }
}
