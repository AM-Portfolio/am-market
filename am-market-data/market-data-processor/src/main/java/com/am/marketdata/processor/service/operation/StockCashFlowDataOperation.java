package com.am.marketdata.processor.service.operation;

import com.am.common.investment.model.equity.financial.cashflow.StockCashFlow;
import com.am.marketdata.common.model.tradeB.financials.cashflow.CashFlowResponse;
import com.am.marketdata.external.api.client.TradeBrainClient;
import com.am.marketdata.external.api.model.ApiResponse;
import com.am.marketdata.processor.exception.DataValidationException;
import com.am.marketdata.processor.service.common.AbstractMarketDataOperation;
import com.am.marketdata.processor.service.common.DataFetcher;
import com.am.marketdata.processor.service.common.DataProcessor;
import com.am.marketdata.processor.service.common.DataValidator;
import com.am.marketdata.processor.service.mapper.StockCashFlowFinanceMapper;
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
public class StockCashFlowDataOperation extends AbstractMarketDataOperation<StockCashFlow, Void> {
    
    private final TradeBrainClient tradeBrainClient;
    
    private StockCashFlow lastFetchedData;
    private final StockCashFlowFinanceMapper cashFlowMapper;
    
    @Value("${market.data.max.retries:3}")
    private int maxRetries;
    
    @Value("${market.data.retry.delay.ms:1000}")
    private long retryDelayMs;
    
    public StockCashFlowDataOperation(
            DataFetcher dataFetcher,
            DataValidator<StockCashFlow> stockCashFlowValidator,
            DataProcessor<StockCashFlow, Void> stockCashFlowProcessor,
            MeterRegistry meterRegistry,
            @Qualifier("asyncExecutor") Executor executor,
            TradeBrainClient tradeBrainClient,
            StockCashFlowFinanceMapper cashFlowMapper) {
        super(dataFetcher, stockCashFlowValidator, stockCashFlowProcessor, meterRegistry, executor);
        this.tradeBrainClient = tradeBrainClient;
        this.cashFlowMapper = cashFlowMapper;
    }

    public StockCashFlowDataOperation withSymbol(String symbol) {
        return (StockCashFlowDataOperation) super.withIndexSymbol(symbol);
    }
    
    @Override
    protected String getDataTypeName() {
        return "stock-cash-flow";
    }
    
    @Override
    protected void handleSuccess(Void result) {
        log.info("Successfully processed stock cash flow");
    }
    
    @Override
    @SneakyThrows
    protected StockCashFlow fetchData() {
        try {
            ApiResponse response = tradeBrainClient.getCashFlow(getIndexSymbol());
            CashFlowResponse cashFlow = cashFlowMapper.parse(response.getData());
            if (cashFlow == null) {
                return null;
            }
            lastFetchedData = cashFlowMapper.toCashFlow(getIndexSymbol(), cashFlow);
            return lastFetchedData;
        } catch (Exception e) {
            throw new DataValidationException(getIndexSymbol(), getDataTypeName(), maxRetries, "Failed to fetch stock cash flow data", e);
        }
    }
    
    public String getIndexSymbol() {
        return super.getIndexSymbol();
    }

    public StockCashFlow getLastFetchedData() {
        return lastFetchedData;
    }
}