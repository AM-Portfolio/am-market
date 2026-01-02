package com.am.marketdata.processor.service.processor;

import com.am.common.investment.model.equity.financial.cashflow.StockCashFlow;
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
public class StockCashFlowProcessor implements DataProcessor<StockCashFlow, Void> {
    
    private final StockPortfolioProducerService stockPortfolioProducer;
    private final StockFinancialPerformanceService stockFinancialPerformanceService;

    @Override
    @SneakyThrows
    public Void process(StockCashFlow data) {
        if (data == null || data.getCashFlow() == null || data.getCashFlow().isEmpty()) {
            log.warn("Received null or empty stock cash flow response");
            throw new ProcessorException(getDataTypeName(),ProcessorException.ProcessorErrorType.INVALID_DATA,"Null or empty stock cash flow data");
        }

        log.info("Processing {} stock cash flow", data.getCashFlow().size());

        try {
            // Save to the database
            stockFinancialPerformanceService.saveCashFlow(data);

            log.info("Processing stock cash flow data. Symbol: {}, Count: {}", 
                data.getSymbol(),
                data.getCashFlow().size());
            // Publish to Kafka
            try {
                stockPortfolioProducer.sendCashFlowFinancialsUpdate(data.getSymbol(), data);
                log.debug("Published stock cash flow data for symbol: {}", data.getSymbol());
            } catch (Exception e) {
                log.error("Failed to publish stock cash flow data for symbol: {}", data.getSymbol(), e);
                // Continue processing other items
            }
            
            log.info("Successfully published stock cash flow data to Kafka");
            return null;
        } catch (Exception e) {
            log.error("Error processing stock cash flow data for symbol: {}", data.getSymbol(), e);
            throw new ProcessorException(getDataTypeName(),ProcessorException.ProcessorErrorType.PERSISTENCE_ERROR,"Failed to process stock cash flow data", e);
        }
    }
    
    @Override
    public String getDataTypeName() {
        return "stock-cash-flow";
    }
    
}