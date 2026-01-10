package com.am.marketdata.service.websocket.processor;

import com.am.marketdata.common.model.OHLCQuote;

import com.am.common.investment.model.equity.EquityPrice;
import com.am.marketdata.common.mapper.OHLCMapper;
import com.am.marketdata.kafka.producer.KafkaProducerService;
import com.am.marketdata.provider.upstox.model.feed.FeedItem;
import com.am.marketdata.provider.upstox.model.feed.OHLCInterval;
import com.am.marketdata.provider.upstox.model.feed.UpstoxFeedResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataProcessor {

    private final ObjectMapper objectMapper;
    private final java.util.Optional<KafkaProducerService> kafkaProducerService;
    private final java.util.Optional<com.am.marketdata.service.kafka.producer.MarketDataProducer> marketDataProducer;
    private final OHLCMapper ohlcMapper;

    /**
     * Process incoming market update object and convert to OHLCQuote items.
     * 
     * @param marketUpdate The raw update object (expected JSON string or
     *                     UpstoxFeedResponse)
     * @return Map of Symbol -> OHLCQuote
     */
    public Map<String, OHLCQuote> processUpdate(Object marketUpdate) {
        Map<String, OHLCQuote> results = new HashMap<>();

        try {
            UpstoxFeedResponse response = null;

            // 1. Deserialize input
            if (marketUpdate instanceof String) {
                response = objectMapper.readValue((String) marketUpdate, UpstoxFeedResponse.class);
            } else if (marketUpdate instanceof UpstoxFeedResponse) {
                response = (UpstoxFeedResponse) marketUpdate;
            } else {
                log.warn("Received unknown update type or V3 object. Expecting JSON string or UpstoxFeedResponse.");
                return results;
            }

            if (response == null || response.getFeeds() == null) {
                log.warn("Invalid or empty response object");
                return results;
            }

            // 2. Map to OHLCQuote
            for (Map.Entry<String, FeedItem> entry : response.getFeeds().entrySet()) {
                String instrumentKey = entry.getKey(); // e.g., "NSE_FO|61755"
                FeedItem feedItem = entry.getValue();

                if (feedItem.getFullFeed() != null && feedItem.getFullFeed().getMarketFF() != null) {
                    var marketFF = feedItem.getFullFeed().getMarketFF();

                    OHLCQuote quote = new OHLCQuote();
                    // Set LTP
                    quote.setLastPrice(marketFF.getLtpc().getLtp());

                    // Set OHLC from "marketOHLC" -> "1d" interval usually
                    if (marketFF.getMarketOHLC() != null && marketFF.getMarketOHLC().getOhlc() != null) {
                        for (OHLCInterval interval : marketFF.getMarketOHLC().getOhlc()) {
                            // prioritizing "1d" or "day" for the main OHLC
                            if ("1d".equalsIgnoreCase(interval.getInterval())
                                    || "I1".equalsIgnoreCase(interval.getInterval())) {
                                OHLCQuote.OHLC ohlcData = new OHLCQuote.OHLC();
                                ohlcData.setOpen(interval.getOpen());
                                ohlcData.setHigh(interval.getHigh());
                                ohlcData.setLow(interval.getLow());
                                ohlcData.setClose(interval.getClose());
                                quote.setOhlc(ohlcData);

                                if ("1d".equalsIgnoreCase(interval.getInterval())) {
                                    break;
                                }
                            }
                        }
                    }

                    if (quote.getLastPrice() != 0) {
                        results.put(instrumentKey, quote);
                    }
                }
            }

            // 3. Publish to Kafka
            if (!results.isEmpty()) {
                log.info("Processed {} quotes. Publishing to Kafka.", results.size());
                List<EquityPrice> equityPrices = ohlcMapper.toEquityPriceList(results);
                kafkaProducerService.ifPresent(service -> service.sendEquityPriceUpdates(equityPrices));

                // Also send to Ingestion topic for robust persistence (Streaming Injection)
                marketDataProducer.ifPresent(producer -> producer.sendOHLCData(results,
                        com.am.marketdata.common.model.TimeFrame.DAY, "UPSTOX_STREAM"));
            }

        } catch (Exception e) {
            log.error("Error processing update", e);
        }

        return results;
    }
}
