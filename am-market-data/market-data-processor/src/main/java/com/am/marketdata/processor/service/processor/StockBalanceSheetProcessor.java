package com.am.marketdata.processor.service.processor;

import com.am.common.investment.model.equity.financial.balancesheet.StockBalanceSheet;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class StockBalanceSheetProcessor implements DataProcessor<StockBalanceSheet, Void> {
    
    private final StockPortfolioProducerService stockPortfolioProducer;
    private final StockFinancialPerformanceService stockFinancialPerformanceService;

    @Override
    @SneakyThrows
    public Void process(StockBalanceSheet data) {
        if (data == null || data.getBalanceSheet() == null || data.getBalanceSheet().isEmpty()) {
            log.warn("Received null or empty stock balance sheet response");
            throw new ProcessorException(getDataTypeName(),ProcessorException.ProcessorErrorType.INVALID_DATA,"Null or empty stock balance sheet data");
        }

        log.info("Processing {} stock balance sheet", data.getBalanceSheet().size());

        try {
            // Save to the database
            stockFinancialPerformanceService.saveBalanceSheet(data);

            log.info("Processing stock balance sheet data. Symbol: {}, Count: {}", 
                data.getSymbol(),
                data.getBalanceSheet().size());
            // Publish to Kafka
            try {
                stockPortfolioProducer.sendBalanceSheetFinancialsUpdate(data.getSymbol(), data);
                log.debug("Published stock balance sheet data for symbol: {}", data.getSymbol());
            } catch (Exception e) {
                log.error("Failed to publish stock balance sheet data for symbol: {}", data.getSymbol(), e);
                // Continue processing other items
            }
            
            log.info("Successfully published stock balance sheet data to Kafka");
            return null;
        } catch (Exception e) {
            log.error("Error processing stock balance sheet data for symbol: {}", data.getSymbol(), e);
            throw new ProcessorException(getDataTypeName(),ProcessorException.ProcessorErrorType.PERSISTENCE_ERROR,"Failed to process stock balance sheet data", e);
        }
    }
    
    @Override
    public String getDataTypeName() {
        return "stock-balance-sheet";
    }
    
}