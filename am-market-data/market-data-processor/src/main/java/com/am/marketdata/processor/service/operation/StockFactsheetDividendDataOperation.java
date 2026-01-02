package com.am.marketdata.processor.service.operation;

import com.am.common.investment.model.equity.financial.factsheetdividend.StockFactSheetDividend;
import com.am.common.investment.model.equity.financial.profitandloss.StockProfitAndLoss;
import com.am.marketdata.common.model.tradeB.financials.dividend.FactSheetDividendResponse;
import com.am.marketdata.common.model.tradeB.financials.profitloss.ProfitLossStatementResponse;
import com.am.marketdata.external.api.client.TradeBrainClient;
import com.am.marketdata.external.api.model.ApiResponse;
import com.am.marketdata.processor.exception.DataValidationException;
import com.am.marketdata.processor.service.common.AbstractMarketDataOperation;
import com.am.marketdata.processor.service.common.DataFetcher;
import com.am.marketdata.processor.service.common.DataProcessor;
import com.am.marketdata.processor.service.common.DataValidator;
import com.am.marketdata.processor.service.mapper.StockBoardOfDirectorsMapper;
import com.am.marketdata.processor.service.mapper.StockFactSheetFinanceMapper;
import com.am.marketdata.processor.service.mapper.StockProfitLossFinanceMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Operation for fetching and processing NSE stock indices data
 */
@Slf4j
@Component
public class StockFactsheetDividendDataOperation extends AbstractMarketDataOperation<StockFactSheetDividend, Void> {
    
    private final TradeBrainClient tradeBrainClient;

    private StockFactSheetDividend lastFetchedData;
    private final StockFactSheetFinanceMapper factSheetFinanceMapper;
    
    @Value("${market.data.max.retries:3}")
    private int maxRetries;
    
    @Value("${market.data.retry.delay.ms:1000}")
    private long retryDelayMs;
    
    public StockFactsheetDividendDataOperation(
            DataFetcher dataFetcher,
            DataValidator<StockFactSheetDividend> stockFactSheetDividendValidator,
            DataProcessor<StockFactSheetDividend, Void> stockFactSheetDividendProcessor,
            MeterRegistry meterRegistry,
            @Qualifier("asyncExecutor") Executor executor,
            TradeBrainClient tradeBrainClient,
            StockFactSheetFinanceMapper factSheetFinanceMapper) {
        super(dataFetcher, stockFactSheetDividendValidator, stockFactSheetDividendProcessor, meterRegistry, executor);
        this.tradeBrainClient = tradeBrainClient;
        this.factSheetFinanceMapper = factSheetFinanceMapper;
    }

    public StockFactsheetDividendDataOperation withSymbol(String symbol) {
        return (StockFactsheetDividendDataOperation) super.withIndexSymbol(symbol);
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
    protected StockFactSheetDividend fetchData() {
        try {
            ApiResponse response = tradeBrainClient.getDividends(getIndexSymbol());
            FactSheetDividendResponse factSheetDividend = factSheetFinanceMapper.parse(response.getData());
            if (factSheetDividend == null) {
                return null;
            }
            lastFetchedData = factSheetFinanceMapper.toFactSheetDividend(getIndexSymbol(), factSheetDividend);
            return lastFetchedData;
        } catch (Exception e) {
            throw new DataValidationException(getIndexSymbol(), getDataTypeName(), maxRetries, "Failed to fetch stock factsheet dividend data", e);
        }
    }
    
    public String getIndexSymbol() {
        return super.getIndexSymbol();
    }

    public StockFactSheetDividend getLastFetchedData() {
        return lastFetchedData;
    }
}