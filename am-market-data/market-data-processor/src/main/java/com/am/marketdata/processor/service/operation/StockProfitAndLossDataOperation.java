package com.am.marketdata.processor.service.operation;

import com.am.common.investment.model.equity.financial.profitandloss.StockProfitAndLoss;
import com.am.marketdata.common.model.tradeB.financials.profitloss.ProfitLossStatementResponse;
import com.am.marketdata.external.api.client.TradeBrainClient;
import com.am.marketdata.external.api.model.ApiResponse;
import com.am.marketdata.processor.exception.DataValidationException;
import com.am.marketdata.processor.service.common.AbstractMarketDataOperation;
import com.am.marketdata.processor.service.common.DataFetcher;
import com.am.marketdata.processor.service.common.DataProcessor;
import com.am.marketdata.processor.service.common.DataValidator;
import com.am.marketdata.processor.service.mapper.StockProfitLossFinanceMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * Operation for fetching and processing NSE stock indices data
 */
@Slf4j
@Component
public class StockProfitAndLossDataOperation extends AbstractMarketDataOperation<StockProfitAndLoss, Void> {
    
    private final TradeBrainClient tradeBrainClient;
    
    private StockProfitAndLoss lastFetchedData;
    private final StockProfitLossFinanceMapper profitAndLossMapper;
    
    @Value("${market.data.max.retries:3}")
    private int maxRetries;
    
    @Value("${market.data.retry.delay.ms:1000}")
    private long retryDelayMs;
    
    public StockProfitAndLossDataOperation(
            DataFetcher dataFetcher,
            DataValidator<StockProfitAndLoss> stockProfitAndLossValidator,
            DataProcessor<StockProfitAndLoss, Void> stockProfitAndLossProcessor,
            MeterRegistry meterRegistry,
            @Qualifier("asyncExecutor") Executor executor,
            TradeBrainClient tradeBrainClient,
            StockProfitLossFinanceMapper profitAndLossMapper) {
        super(dataFetcher, stockProfitAndLossValidator, stockProfitAndLossProcessor, meterRegistry, executor);
        this.tradeBrainClient = tradeBrainClient;
        this.profitAndLossMapper = profitAndLossMapper;
    }

    public StockProfitAndLossDataOperation withSymbol(String symbol) {
        return (StockProfitAndLossDataOperation) super.withIndexSymbol(symbol);
    }
    
    @Override
    protected String getDataTypeName() {
        return "stock-board-of-directors";
    }
    
    @Override
    protected void handleSuccess(Void result) {
        log.info("Successfully processed stock board of directors");
    }
    
    @Override
    @SneakyThrows
    protected StockProfitAndLoss fetchData() {
        try {
            ApiResponse response = tradeBrainClient.getProfitAndLoss(getIndexSymbol());
            ProfitLossStatementResponse profitAndLoss = profitAndLossMapper.parse(response.getData());
            if (profitAndLoss == null) {
                return null;
            }
            lastFetchedData = profitAndLossMapper.toStockProfitAndLoss(getIndexSymbol(), profitAndLoss);
            return lastFetchedData;
        } catch (Exception e) {
            throw new DataValidationException(getIndexSymbol(), getDataTypeName(), maxRetries, "Failed to fetch stock profit and loss data", e);
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
    public StockProfitAndLoss getLastFetchedData() {
        return lastFetchedData;
    }
}