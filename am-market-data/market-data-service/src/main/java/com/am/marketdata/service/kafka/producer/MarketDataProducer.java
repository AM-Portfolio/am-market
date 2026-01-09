package com.am.marketdata.service.kafka.producer;

import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.common.model.events.OHLCDataIngestionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.market-data-ohlc-ingestion:market-data-ohlc-ingestion}")
    private String ohlcIngestionTopic;

    @Value("${kafka.topics.market-data-historical-ingestion:market-data-historical-ingestion}")
    private String historicalIngestionTopic;

    /**
     * Send OHLC data to Kafka for async ingestion
     *
     * @param data      The OHLC data map
     * @param timeFrame The timeframe of the data
     * @param provider  The source provider
     */
    public void sendOHLCData(Map<String, OHLCQuote> data, TimeFrame timeFrame, String provider) {
        if (data == null || data.isEmpty()) {
            return;
        }

        try {
            OHLCDataIngestionEvent event = OHLCDataIngestionEvent.builder()
                    .data(data)
                    .timeFrame(timeFrame)
                    .provider(provider)
                    .timestamp(System.currentTimeMillis())
                    .build();

            log.info("Sending OHLC ingestion event to Kafka topic {}: {} symbols from {}",
                    ohlcIngestionTopic, data.size(), provider);

            // We use the provider as the key to ensure ordering if needed,
            // or we could use random partitions for parallel processing if ordering isn't
            // critical across symbols.
            // Since the requirement is "process one by one", using a single partition or
            // same key helps,
            // but Consumer Groups allow sequential processing per partition.
            // Using a static key "OHLC_INGESTION" ensures all go to the same partition if
            // we want strict total ordering,
            // but that limits throughput.
            // Given the user said "even though they have thousands of record they will
            // process one by one",
            // let's stick to a constant key for now to guarantee strict sequentiality on a
            // single partition.

            kafkaTemplate.send(ohlcIngestionTopic, "OHLC_INGESTION", event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("Successfully sent OHLC ingestion event to Kafka");
                        } else {
                            log.error("Failed to send OHLC ingestion event to Kafka", ex);
                        }
                    });

        } catch (Exception e) {
            log.error("Error creating/sending OHLC ingestion event", e);
        }
    }

    /**
     * Send Historical data to Kafka for async ingestion
     *
     * @param data      The Historical data map
     * @param timeFrame The timeframe of the data
     * @param provider  The source provider
     */
    public void sendHistoricalData(Map<String, com.am.common.investment.model.historical.HistoricalData> data,
            TimeFrame timeFrame, String provider) {
        if (data == null || data.isEmpty()) {
            return;
        }

        // Split into smaller batches to avoid RecordTooLargeException
        // 11MB for 51 symbols suggests ~200KB-250KB per symbol.
        // A single message limit is often 1MB.
        // So a batch of 1-3 symbols is safe. Let's use 2 to be very safe across all
        // scenarios.
        int batchSize = 2;

        List<Map<String, com.am.common.investment.model.historical.HistoricalData>> batches = new ArrayList<>();
        Map<String, com.am.common.investment.model.historical.HistoricalData> currentBatch = new HashMap<>();

        for (Map.Entry<String, com.am.common.investment.model.historical.HistoricalData> entry : data.entrySet()) {
            currentBatch.put(entry.getKey(), entry.getValue());
            if (currentBatch.size() >= batchSize) {
                batches.add(new HashMap<>(currentBatch));
                currentBatch.clear();
            }
        }
        if (!currentBatch.isEmpty()) {
            batches.add(currentBatch);
        }

        log.info("Splitting {} symbols into {} batches for Kafka ingestion (Batch Size: {})",
                data.size(), batches.size(), batchSize);

        for (int i = 0; i < batches.size(); i++) {
            Map<String, com.am.common.investment.model.historical.HistoricalData> batch = batches.get(i);
            try {
                com.am.marketdata.common.model.events.HistoricalDataIngestionEvent event = com.am.marketdata.common.model.events.HistoricalDataIngestionEvent
                        .builder()
                        .data(batch)
                        .timeFrame(timeFrame)
                        .provider(provider)
                        .timestamp(System.currentTimeMillis())
                        .build();

                log.info("Sending Historical ingestion event batch {}/{} to Kafka topic {}: {} symbols",
                        i + 1, batches.size(), historicalIngestionTopic, batch.size());

                // Using constant key for sequential processing, but appending index to track
                // different messages if needed
                // Actually sticking to "HISTORICAL_INGESTION" is fine as long as they are
                // ordered.
                kafkaTemplate.send(historicalIngestionTopic, "HISTORICAL_INGESTION", event)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                log.debug("Successfully sent Historical ingestion event batch to Kafka");
                            } else {
                                log.error("Failed to send Historical ingestion event batch to Kafka", ex);
                            }
                        });

            } catch (Exception e) {
                log.error("Error creating/sending Historical ingestion event batch " + (i + 1), e);
            }
        }
    }
}
