package com.am.marketdata.processor.factory;

import org.springframework.stereotype.Component;

import com.am.marketdata.processor.service.common.AbstractMarketDataOperation;
import com.am.marketdata.processor.service.common.DataProcessor;
import com.am.marketdata.processor.service.common.DataValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Factory for creating data operations dynamically
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OperationFactory {
    
    private final ProcessorFactory processorFactory;
    
    /**
     * Get or create an operation for the given data type
     * @param <T> Data type
     * @param <R> Result type
     * @param dataType Data type class
     * @param operationClass Operation class
     * @param processorClass Processor class
     * @param validatorClass Validator class
     * @return Market data operation
     */
    public <T, R> AbstractMarketDataOperation<T, R> getOperation(
            Class<T> dataType, 
            Class<? extends AbstractMarketDataOperation<T, R>> operationClass,
            Class<? extends DataProcessor<T, R>> processorClass,
            Class<? extends DataValidator<T>> validatorClass) {
        
        try {
            // Get processor
            DataProcessor<T, R> processor = processorFactory.getProcessor(
                    dataType, processorClass, validatorClass);
            
            // Create operation
            AbstractMarketDataOperation<T, R> operation = operationClass.getDeclaredConstructor(
                    DataProcessor.class)
                .newInstance(processor);
            
            log.info("Created operation for {}: {}", 
                    dataType.getSimpleName(), operationClass.getSimpleName());
            
            return operation;
        } catch (Exception e) {
            log.error("Failed to create operation for {}: {}", 
                    dataType.getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("Failed to create operation for " + dataType.getSimpleName(), e);
        }
    }
}
