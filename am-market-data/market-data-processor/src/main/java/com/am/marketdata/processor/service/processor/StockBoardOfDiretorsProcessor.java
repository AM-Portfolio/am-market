package com.am.marketdata.processor.service.processor;

import com.am.common.investment.model.board.BoardOfDirectors;
import com.am.common.investment.service.StockFinancialPerformanceService;
import com.am.marketdata.kafka.producer.StockPortfolioProducerService;
import com.am.marketdata.processor.exception.ProcessorException;
import com.am.marketdata.processor.service.common.DataProcessor;

import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Processor for NSE Stock Indices data
 * Transforms raw NSE stock indices data into domain model and publishes to Kafka
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockBoardOfDiretorsProcessor implements DataProcessor<BoardOfDirectors, Void> {
    
    private final StockPortfolioProducerService stockPortfolioProducer;
    private final StockFinancialPerformanceService stockFinancialPerformanceService;

    @Override
    @SneakyThrows
    public Void process(BoardOfDirectors data) {
        if (data == null || data.getDirectors() == null || data.getDirectors().isEmpty()) {
            log.warn("Received null or empty stock board of directors response");
            throw new ProcessorException(getDataTypeName(),ProcessorException.ProcessorErrorType.INVALID_DATA,"Null or empty stock board of directors data");
        }

        log.info("Processing {} stock board of directors", data.getDirectors().size());

        try {
            // Save to the database
            stockFinancialPerformanceService.saveBoardOfDirectors(data);

            log.info("Processing stock board of directors data. Symbol: {}, Count: {}", 
                data.getSymbol(),
                data.getDirectors().size());
            // Publish to Kafka
            try {
                stockPortfolioProducer.sendBoardOfDirectorsUpdate(data.getSymbol(), data);
                log.debug("Published stock board of directors data for symbol: {}", data.getSymbol());
            } catch (Exception e) {
                log.error("Failed to publish stock board of directors data for symbol: {}", data.getSymbol(), e);
                // Continue processing other items
            }
            
            log.info("Successfully published stock board of directors data to Kafka");
            return null;
        } catch (Exception e) {
            log.error("Error processing stock board of directors data for symbol: {}", data.getSymbol(), e);
            throw new ProcessorException(getDataTypeName(),ProcessorException.ProcessorErrorType.PERSISTENCE_ERROR,"Failed to process stock board of directors data", e);
        }
    }
    
    @Override
    public String getDataTypeName() {
        return "stock-board-of-directors";
    }
    
}