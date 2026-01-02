package com.am.marketdata.processor.factory;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.am.marketdata.processor.service.common.AbstractMarketDataOperation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Registry for market data operations
 * This provides a centralized way to access all market data operations
 * without creating individual beans for each one
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MarketDataOperationRegistry {
    
    private final ApplicationContext applicationContext;
    private final Map<Class<?>, AbstractMarketDataOperation<?, ?>> operationCache = new HashMap<>();
    
    /**
     * Get an operation for the given data type
     * @param <T> Data type
     * @param <R> Result type
     * @param dataType Data type class
     * @return Market data operation
     */
    @SuppressWarnings("unchecked")
    public <T, R> AbstractMarketDataOperation<T, R> getOperation(Class<T> dataType) {
        if (operationCache.containsKey(dataType)) {
            return (AbstractMarketDataOperation<T, R>) operationCache.get(dataType);
        }
        
        // Find all operations in the application context
        @SuppressWarnings("rawtypes")
        Map<String, AbstractMarketDataOperation> operations = 
                applicationContext.getBeansOfType(AbstractMarketDataOperation.class);
        
        // Find the operation that handles the given data type
        for (AbstractMarketDataOperation<?, ?> operation : operations.values()) {
            try {
                // Use reflection to check if this operation handles the given data type
                Class<?> operationType = getOperationDataType(operation.getClass());
                
                if (operationType != null && dataType.isAssignableFrom(operationType)) {
                    operationCache.put(dataType, operation);
                    log.info("Found operation for {}: {}", dataType.getSimpleName(), operation.getClass().getSimpleName());
                    return (AbstractMarketDataOperation<T, R>) operation;
                }
            } catch (Exception e) {
                log.warn("Error checking operation type for {}: {}", 
                        operation.getClass().getSimpleName(), e.getMessage());
            }
        }
        
        log.error("No operation found for data type: {}", dataType.getSimpleName());
        throw new IllegalArgumentException("No operation found for data type: " + dataType.getSimpleName());
    }
    
    /**
     * Get the data type that an operation handles
     * @param operationClass Operation class
     * @return Data type class
     */
    private Class<?> getOperationDataType(Class<?> operationClass) {
        // Get the generic type parameters of AbstractMarketDataOperation
        java.lang.reflect.Type genericSuperclass = operationClass.getGenericSuperclass();
        if (!(genericSuperclass instanceof java.lang.reflect.ParameterizedType)) {
            return null;
        }
        
        java.lang.reflect.ParameterizedType parameterizedType = 
                (java.lang.reflect.ParameterizedType) genericSuperclass;
        java.lang.reflect.Type[] typeArguments = parameterizedType.getActualTypeArguments();
        
        if (typeArguments.length > 0 && typeArguments[0] instanceof Class) {
            return (Class<?>) typeArguments[0];
        }
        
        return null;
    }
}
