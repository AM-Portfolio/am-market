package com.am.marketdata.processor.service.operation;

import com.am.common.investment.model.equity.financial.balancesheet.StockBalanceSheet;
import com.am.marketdata.common.model.tradeB.financials.balancesheet.BalanceSheetResponse;
import com.am.marketdata.external.api.client.TradeBrainClient;
import com.am.marketdata.external.api.model.ApiResponse;
import com.am.marketdata.processor.exception.DataValidationException;
import com.am.marketdata.processor.service.common.AbstractMarketDataOperation;
import com.am.marketdata.processor.service.common.DataFetcher;
import com.am.marketdata.processor.service.common.DataProcessor;
import com.am.marketdata.processor.service.common.DataValidator;
import com.am.marketdata.processor.service.mapper.StockBalanceSheetFinanceMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
public class StockBalanceSheetDataOperation extends AbstractMarketDataOperation<StockBalanceSheet, Void> {
    
    private final TradeBrainClient tradeBrainClient;
    
    private StockBalanceSheet lastFetchedData;
    private final StockBalanceSheetFinanceMapper balanceSheetMapper;
    
    @Value("${market.data.max.retries:3}")
    private int maxRetries;
    
    @Value("${market.data.retry.delay.ms:1000}")
    private long retryDelayMs;
    
    public StockBalanceSheetDataOperation(
            DataFetcher dataFetcher,
            DataValidator<StockBalanceSheet> stockBalanceSheetValidator,
            DataProcessor<StockBalanceSheet, Void> stockBalanceSheetProcessor,
            MeterRegistry meterRegistry,
            @Qualifier("asyncExecutor") Executor executor,
            TradeBrainClient tradeBrainClient,
            StockBalanceSheetFinanceMapper balanceSheetMapper) {
        super(dataFetcher, stockBalanceSheetValidator, stockBalanceSheetProcessor, meterRegistry, executor);
        this.tradeBrainClient = tradeBrainClient;
        this.balanceSheetMapper = balanceSheetMapper;
    }

    public StockBalanceSheetDataOperation withSymbol(String symbol) {
        return (StockBalanceSheetDataOperation) super.withIndexSymbol(symbol);
    }
    
    @Override
    protected String getDataTypeName() {
        return "stock-balance-sheet";
    }
    
    @Override
    protected void handleSuccess(Void result) {
        log.info("Successfully processed stock balance sheet");
    }
    
    @Override
    @SneakyThrows
    protected StockBalanceSheet fetchData() {
        try {
            ApiResponse response = tradeBrainClient.getBalanceSheet(getIndexSymbol());
            BalanceSheetResponse balanceSheetResponse = balanceSheetMapper.parse(response.getData());
            if (balanceSheetResponse == null) {
                return null;
            }
            lastFetchedData = balanceSheetMapper.toBalanceSheet(getIndexSymbol(), balanceSheetResponse);
            return lastFetchedData;
        } catch (Exception e) {
            throw new DataValidationException(getIndexSymbol(), getDataTypeName(), maxRetries, "Failed to fetch stock balance sheet data", e);
        }
    }
    
    public String getIndexSymbol() {
        return super.getIndexSymbol();
    }

    public StockBalanceSheet getLastFetchedData() {
        return lastFetchedData;
    }
}