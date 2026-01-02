package com.am.marketdata.processor.service.processor;

import com.am.common.investment.model.equity.financial.profitandloss.StockProfitAndLoss;
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
public class StockProfitAndLossProcessor implements DataProcessor<StockProfitAndLoss, Void> {
    
    private final StockFinancialPerformanceService stockFinancialPerformanceService;
    private final StockPortfolioProducerService stockPortfolioProducer;

    @Override
    @SneakyThrows
    public Void process(StockProfitAndLoss data) {
        if (data == null || data.getProfitAndLoss() == null || data.getProfitAndLoss().isEmpty()) {
            log.warn("Received null or empty stock profit and loss response");
            throw new ProcessorException(getDataTypeName(),ProcessorException.ProcessorErrorType.INVALID_DATA,"Null or empty stock profit and loss data");
        }

        log.info("Processing {} stock profit and loss", data.getProfitAndLoss().size());

        try {
            // Save to the database
            stockFinancialPerformanceService.saveProfitAndLoss(data);

            log.info("Processing stock profit and loss data. Symbol: {}, Count: {}", 
                data.getSymbol(),
                data.getProfitAndLoss().size());
            // Publish to Kafka
            try {
                stockPortfolioProducer.sendStockProfitAndLossFinancialsUpdate(data.getSymbol(), data);
                log.debug("Published stock profit and loss data for symbol: {}", data.getSymbol());
            } catch (Exception e) {
                log.error("Failed to publish stock profit and loss data for symbol: {}", data.getSymbol(), e);
                // Continue processing other items
            }
            
            log.info("Successfully published stock profit and loss data to Kafka");
            return null;
        } catch (Exception e) {
            log.error("Error processing stock profit and loss data for symbol: {}", data.getSymbol(), e);
            throw new ProcessorException(getDataTypeName(),ProcessorException.ProcessorErrorType.PERSISTENCE_ERROR,"Failed to process stock profit and loss data", e);
        }
    }   
    
    @Override
    public String getDataTypeName() {
        return "stock-profit-and-loss";
    }
    
}