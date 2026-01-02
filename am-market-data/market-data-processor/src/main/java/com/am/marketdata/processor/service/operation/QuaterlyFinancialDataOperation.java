package com.am.marketdata.processor.service.operation;

import com.am.common.investment.model.equity.financial.resultstatement.QuaterlyResult;
import com.am.marketdata.common.model.tradeB.financials.results.QuaterlyFinancialStatementResponse;
import com.am.marketdata.external.api.client.TradeBrainClient;
import com.am.marketdata.external.api.model.ApiResponse;
import com.am.marketdata.processor.exception.DataValidationException;
import com.am.marketdata.processor.service.common.AbstractMarketDataOperation;
import com.am.marketdata.processor.service.common.DataFetcher;
import com.am.marketdata.processor.service.common.DataProcessor;
import com.am.marketdata.processor.service.common.DataValidator;
import com.am.marketdata.processor.service.mapper.StockQuaterlyResultFinanceMapper;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Operation for fetching and processing NSE stock indices data
 */
@Slf4j
@Component
public class QuaterlyFinancialDataOperation extends AbstractMarketDataOperation<QuaterlyResult, Void> {
    
    private final TradeBrainClient tradeBrainClient;
    
    private QuaterlyResult lastFetchedData;
    private final StockQuaterlyResultFinanceMapper stockQuaterlyResultFinanceMapper;
    
    @Value("${market.data.max.retries:3}")
    private int maxRetries;
    
    @Value("${market.data.retry.delay.ms:1000}")
    private long retryDelayMs;
    
    public QuaterlyFinancialDataOperation(
            DataFetcher dataFetcher,
            DataValidator<QuaterlyResult> stockQuaterlyResultFinanceValidator,
            DataProcessor<QuaterlyResult, Void> stockQuaterlyResultFinanceProcessor,
            MeterRegistry meterRegistry,
            @Qualifier("asyncExecutor") Executor executor,
            TradeBrainClient tradeBrainClient,
            StockQuaterlyResultFinanceMapper stockQuaterlyResultFinanceMapper) {
        super(dataFetcher, stockQuaterlyResultFinanceValidator, stockQuaterlyResultFinanceProcessor, meterRegistry, executor);
        this.tradeBrainClient = tradeBrainClient;
        this.stockQuaterlyResultFinanceMapper = stockQuaterlyResultFinanceMapper;
    }

    public QuaterlyFinancialDataOperation withSymbol(String symbol) {
        return (QuaterlyFinancialDataOperation) super.withIndexSymbol(symbol);
    }
    
    @Override
    protected String getDataTypeName() {
        return "stock-quaterly-financials";
    }
    
    @Override
    protected void handleSuccess(Void result) {
        log.info("Successfully processed stock quaterly financials");
    }
    
    @Override
    @SneakyThrows
    protected QuaterlyResult fetchData() {
        log.info("Fetching stock quaterly financials for symbol: {}", getIndexSymbol());
        try {
            ApiResponse response = tradeBrainClient.getQuaterlyFinancials(getIndexSymbol());
            log.info("Quaterly Financials Response: {}", response);
            QuaterlyFinancialStatementResponse financials = stockQuaterlyResultFinanceMapper.parseQuaterlyFinancials(response.getData());
            if (financials == null) {
                return null;
            }
            lastFetchedData = stockQuaterlyResultFinanceMapper.toQuarterlyFinancialMetrics(getIndexSymbol(), financials);
            return lastFetchedData;
        } catch (Exception e) {
            throw new DataValidationException(getIndexSymbol(), getDataTypeName(), maxRetries, "Failed to fetch stock quaterly financials data", e);
        }
    }
    
    /**
     * Get the current index symbol
     * 
     * @return The index symbol
     */
    public String getIndexSymbol() {
        return super.getIndexSymbol();
    }

    /**
     * Get the last fetched data from the API
     * 
     * @return The last fetched data, or null if no data has been fetched yet
     */
    public QuaterlyResult getLastFetchedData() {
        return lastFetchedData;
    }
}