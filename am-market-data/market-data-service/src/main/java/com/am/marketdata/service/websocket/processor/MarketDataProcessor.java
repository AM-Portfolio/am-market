package com.am.marketdata.service.websocket.processor;

import com.am.marketdata.common.model.OHLCQuote;
import com.am.common.investment.model.equity.EquityPrice;
import com.am.marketdata.common.mapper.OHLCMapper;
import com.am.marketdata.kafka.producer.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Processes market data updates using ONLY organization-level common models.
 * 
 * IMPORTANT: This class should NEVER import provider-specific models (Upstox,
 * Zerodha, etc.).
 * Provider layer is responsible for converting provider-specific formats to
 * common models.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataProcessor {

    private final java.util.Optional<KafkaProducerService> kafkaProducerService;
    private final java.util.Optional<com.am.marketdata.service.kafka.producer.MarketDataProducer> marketDataProducer;
    private final OHLCMapper ohlcMapper;

    /**
     * Process incoming market update using organization-level common model.
     * 
     * @param marketUpdate Map of instrumentKey -> OHLCQuote (common model from
     *                     provider layer)
     * @return Map of Symbol -> OHLCQuote (same as input, for backward
     *         compatibility)
     */
    @SuppressWarnings("unchecked")
    public Map<String, OHLCQuote> processUpdate(Object marketUpdate) {
        try {
            // Accept only common model: Map<String, OHLCQuote>
            final Map<String, OHLCQuote> results;
            if (marketUpdate instanceof Map) {
                results = (Map<String, OHLCQuote>) marketUpdate;
            } else {
                log.warn("MarketDataProcessor expects Map<String, OHLCQuote> (common model). Received: {}",
                        marketUpdate != null ? marketUpdate.getClass().getSimpleName() : "null");
                return new HashMap<>();
            }

            if (results.isEmpty()) {
                log.debug("Empty market data update");
                return results;
            }

            // Publish to Kafka
            log.info("Processed {} quotes. Publishing to Kafka.", results.size());
            List<EquityPrice> equityPrices = ohlcMapper.toEquityPriceList(results);
            kafkaProducerService.ifPresent(service -> service.sendEquityPriceUpdates(equityPrices));

            // Also send to Ingestion topic for robust persistence (Streaming Injection)
            marketDataProducer.ifPresent(producer -> producer.sendOHLCData(results,
                    com.am.marketdata.common.model.TimeFrame.DAY, "PROVIDER_STREAM"));

            return results;

        } catch (Exception e) {
            log.error("Error processing market update", e);
            return new HashMap<>();
        }
    }
}
