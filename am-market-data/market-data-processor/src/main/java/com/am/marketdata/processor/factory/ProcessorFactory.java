package com.am.marketdata.processor.factory;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.am.common.investment.service.StockFinancialPerformanceService;
import com.am.marketdata.kafka.producer.StockPortfolioProducerService;
import com.am.marketdata.processor.service.common.DataProcessor;
import com.am.marketdata.processor.service.common.DataValidator;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Factory for creating data processors dynamically
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ProcessorFactory {
    
    private final StockPortfolioProducerService stockPortfolioProducer;
    private final StockFinancialPerformanceService stockFinancialPerformanceService;
    private final MeterRegistry meterRegistry;
    
    private final Map<String, DataProcessor<?, ?>> processorCache = new HashMap<>();
    private final Map<String, DataValidator<?>> validatorCache = new HashMap<>();
    private final Map<String, Timer> timerCache = new HashMap<>();
    
    /**
     * Get or create a processor for the given data type
     * @param <T> Data type
     * @param <R> Result type
     * @param dataType Data type class
     * @param processorClass Processor class
     * @param validatorClass Validator class
     * @return Data processor
     */
    @SuppressWarnings("unchecked")
    public <T, R> DataProcessor<T, R> getProcessor(
            Class<T> dataType, 
            Class<? extends DataProcessor<T, R>> processorClass,
            Class<? extends DataValidator<T>> validatorClass) {
        
        String processorKey = dataType.getSimpleName();
        
        if (processorCache.containsKey(processorKey)) {
            return (DataProcessor<T, R>) processorCache.get(processorKey);
        }
        
        try {
            // Create timer
            Timer timer = getOrCreateTimer(processorKey);
            
            // Create validator
            DataValidator<T> validator = getOrCreateValidator(processorKey, validatorClass);
            
            // Create processor
            DataProcessor<T, R> processor = processorClass.getDeclaredConstructor(
                    StockPortfolioProducerService.class,
                    StockFinancialPerformanceService.class,
                    DataValidator.class,
                    Timer.class)
                .newInstance(stockPortfolioProducer, stockFinancialPerformanceService, validator, timer);
            
            processorCache.put(processorKey, processor);
            log.info("Created processor for {}", processorKey);
            
            return processor;
        } catch (Exception e) {
            log.error("Failed to create processor for {}: {}", processorKey, e.getMessage(), e);
            throw new RuntimeException("Failed to create processor for " + processorKey, e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> DataValidator<T> getOrCreateValidator(String key, Class<? extends DataValidator<T>> validatorClass) {
        if (validatorCache.containsKey(key)) {
            return (DataValidator<T>) validatorCache.get(key);
        }
        
        try {
            DataValidator<T> validator = validatorClass.getDeclaredConstructor().newInstance();
            validatorCache.put(key, validator);
            return validator;
        } catch (Exception e) {
            log.error("Failed to create validator for {}: {}", key, e.getMessage(), e);
            throw new RuntimeException("Failed to create validator for " + key, e);
        }
    }
    
    private Timer getOrCreateTimer(String key) {
        if (timerCache.containsKey(key)) {
            return timerCache.get(key);
        }
        
        String metricName = key.replaceAll("([a-z])([A-Z])", "$1.$2").toLowerCase() + ".processing.time";
        Timer timer = Timer.builder(metricName)
                .description("Time taken to process " + key + " data")
                .register(meterRegistry);
        
        timerCache.put(key, timer);
        return timer;
    }
}
