package com.am.marketdata.kafka.consumer;

import com.am.common.investment.model.historical.HistoricalData;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.events.HistoricalDataIngestionEvent;
import com.am.marketdata.common.model.events.OHLCDataIngestionEvent;
import com.am.marketdata.common.service.MarketDataIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class MarketDataConsumer {

    private final MarketDataIngestionService ingestionService;

    /**
     * Consumes equity price updates from Kafka
     * 
     * @param event The equity price update event
     */
    @KafkaListener(topics = "${kafka.topics.stock-price}", groupId = "${kafka.consumer.group-id:market-data-service-group}")
    public void consumeEquityPriceUpdates(com.am.common.investment.model.events.EquityPriceUpdateEvent event) {
        try {
            if (event == null || event.getEquityPrices() == null || event.getEquityPrices().isEmpty()) {
                return;
            }

            log.debug("Received {} equity price updates from Kafka", event.getEquityPrices().size());

            Map<String, OHLCQuote> quotes = new java.util.HashMap<>();
            for (com.am.common.investment.model.equity.EquityPrice price : event.getEquityPrices()) {
                if (price.getSymbol() != null) {
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

            ingestionService.saveOHLCData(quotes);

        } catch (Exception e) {
            log.error("Error processing equity price update event", e);
        }
    }

    /**
     * Consumes OHLC data ingestion events from Kafka
     *
     * @param event The OHLC data ingestion event
     */
    @KafkaListener(topics = "${kafka.topics.market-data-ohlc-ingestion:market-data-ohlc-ingestion}", groupId = "${kafka.consumer.group-id:market-data-service-group}")
    public void consumeOHLCIngestion(OHLCDataIngestionEvent event) {
        try {
            if (event == null || event.getData() == null || event.getData().isEmpty()) {
                return;
            }

            log.info("Received OHLC ingestion event: {} symbols from {}", event.getData().size(), event.getProvider());

            // Persist data sequentially
            ingestionService.saveOHLCData(event.getData()).join();

            log.debug("Successfully processed OHLC ingestion event");

        } catch (Exception e) {
            log.error("Error processing OHLC ingestion event", e);
        }
    }

    /**
     * Consumes Historical data ingestion events from Kafka
     *
     * @param event The Historical data ingestion event
     */
    @KafkaListener(topics = "${kafka.topics.market-data-historical-ingestion:market-data-historical-ingestion}", groupId = "${kafka.consumer.group-id:market-data-service-group}")
    public void consumeHistoricalIngestion(HistoricalDataIngestionEvent event) {
        try {
            if (event == null || event.getData() == null || event.getData().isEmpty()) {
                return;
            }

            log.info("MarketDataConsumer: Processing Kafka message for topic: market-data-historical-ingestion");
            log.info("Received Historical ingestion event: {} symbols from {}", event.getData().size(),
                    event.getProvider());

            // Persist data sequentially
            for (Map.Entry<String, HistoricalData> entry : event.getData().entrySet()) {
                ingestionService.saveHistoricalData(entry.getKey(), event.getTimeFrame(), entry.getValue()).join();
            }

            log.debug("Successfully processed Historical ingestion event");

        } catch (Exception e) {
            log.error("Error processing Historical ingestion event", e);
        }
    }
}
