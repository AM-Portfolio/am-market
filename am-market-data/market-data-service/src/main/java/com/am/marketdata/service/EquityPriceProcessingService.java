package com.am.marketdata.service;

import com.am.common.investment.model.equity.EquityPrice;
import com.am.common.investment.service.EquityService;
import com.am.marketdata.kafka.producer.KafkaProducerService;
import com.am.marketdata.upstock.adapter.UpStockAdapter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EquityPriceProcessingService {
    private final UpStockAdapter upStockAdapter;
    private final EquityService equityService;
    private final KafkaProducerService kafkaProducerService;
    private final MeterRegistry meterRegistry;

    private static final int BATCH_SIZE = 50;
    private static final String NSE_PREFIX = "NSE_EQ|";

    @Transactional
    public boolean processEquityPrices(List<String> isins) {
        if (isins.isEmpty()) {
            log.warn("No stocks found to process");
            return false;
        }

        Timer.Sample processingTimer = Timer.start(meterRegistry);
        
        try {
            // Format ISINs with NSE prefix
            Set<String> formattedIsins = formatIsins(isins);
            
            // Process in batches
            List<List<String>> batches = partition(formattedIsins.stream().toList(), BATCH_SIZE);
            log.info("Processing {} stocks in {} batches", isins.size(), batches.size());

            List<EquityPrice> allUpdatedStocks = new ArrayList<>();
            boolean hasErrors = false;

            // Track metrics for batch processing
            meterRegistry.counter("equity.price.batch.total").increment(batches.size());

            for (List<String> batch : batches) {
                try {
                    Timer.Sample batchTimer = Timer.start(meterRegistry);
                    var equityPrices = upStockAdapter.getStocksOHLC(batch);
                    
                    if (!equityPrices.isEmpty()) {
                        equityService.saveAllPrices(equityPrices);
                        allUpdatedStocks.addAll(equityPrices);
                        meterRegistry.counter("equity.price.batch.success").increment();
                    } else {
                        log.warn("Received empty response for batch. Skipping Kafka event.");
                        meterRegistry.counter("equity.price.batch.empty").increment();
                        hasErrors = true;
                    }
                    
                    batchTimer.stop(meterRegistry.timer("equity.price.batch.processing.time"));
                } catch (Exception e) {
                    log.error("Error processing batch: {}", e.getMessage(), e);
                    meterRegistry.counter("equity.price.batch.error").increment();
                    hasErrors = true;
                }
            }

            // Send Kafka events if we have data and no errors occurred
            if (!allUpdatedStocks.isEmpty() && !hasErrors) {
                log.info("Sending Kafka events for {} updated stocks", allUpdatedStocks.size());
                kafkaProducerService.sendEquityPriceUpdates(allUpdatedStocks);
                meterRegistry.counter("equity.price.kafka.events.sent").increment();
                processingTimer.stop(meterRegistry.timer("equity.price.total.processing.time"));
                return true;
            } else {
                log.warn("Skipping Kafka events due to errors or no data");
                meterRegistry.counter("equity.price.kafka.events.skipped").increment();
            }

        } catch (Exception e) {
            log.error("Error in equity price processing: {}", e.getMessage(), e);
            meterRegistry.counter("equity.price.processing.error").increment();
        }
        
        processingTimer.stop(meterRegistry.timer("equity.price.total.processing.time"));
        return false;
    }

    private Set<String> formatIsins(List<String> isins) {
        return isins.stream()
            .map(isin -> NSE_PREFIX + isin)
            .collect(Collectors.toSet());
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        return list.stream()
            .collect(Collectors.groupingBy(item -> list.indexOf(item) / size))
            .values()
            .stream()
            .toList();
    }
    
    /**
     * Get the latest equity prices for the specified ISINs
     * This method fetches the latest prices directly from the data source without persisting them
     * 
     * @param isins List of ISINs to get prices for
     * @return List of equity prices with current market data
     */
    public List<EquityPrice> getLatestEquityPrices(List<String> isins) {
        if (isins == null || isins.isEmpty()) {
            log.warn("No ISINs provided to fetch prices for");
            return List.of();
        }
        
        Timer.Sample fetchTimer = Timer.start(meterRegistry);
        log.info("Fetching latest prices for {} ISINs", isins.size());
        
        try {
            // Format ISINs with NSE prefix
            Set<String> formattedIsins = formatIsins(isins);
            
            // Process in batches for better performance
            List<List<String>> batches = partition(formattedIsins.stream().toList(), BATCH_SIZE);
            log.debug("Processing {} ISINs in {} batches", isins.size(), batches.size());
            
            // Use a thread-safe collection to store results from all batches
            List<EquityPrice> allPrices = new ArrayList<>();
            
            // Process each batch and collect results
            for (List<String> batch : batches) {
                try {
                    var equityPrices = upStockAdapter.getStocksOHLC(batch);
                    if (!equityPrices.isEmpty()) {
                        allPrices.addAll(equityPrices);
                        meterRegistry.counter("equity.price.fetch.success").increment();
                    } else {
                        log.warn("Received empty response for batch");
                        meterRegistry.counter("equity.price.fetch.empty").increment();
                    }
                } catch (Exception e) {
                    log.error("Error fetching prices for batch: {}", e.getMessage(), e);
                    meterRegistry.counter("equity.price.fetch.error").increment();
                }
            }
            
            log.info("Successfully fetched {} equity prices", allPrices.size());
            fetchTimer.stop(meterRegistry.timer("equity.price.fetch.time"));
            return allPrices;
            
        } catch (Exception e) {
            log.error("Error fetching latest equity prices: {}", e.getMessage(), e);
            meterRegistry.counter("equity.price.fetch.error").increment();
            fetchTimer.stop(meterRegistry.timer("equity.price.fetch.time"));
            return List.of();
        }
    }
}
