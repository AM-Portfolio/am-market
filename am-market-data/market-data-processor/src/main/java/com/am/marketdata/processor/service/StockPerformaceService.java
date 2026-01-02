package com.am.marketdata.processor.service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import com.am.common.investment.model.board.BoardOfDirectors;
import com.am.common.investment.model.equity.financial.balancesheet.StockBalanceSheet;
import com.am.common.investment.model.equity.financial.cashflow.StockCashFlow;
import com.am.common.investment.model.equity.financial.factsheetdividend.StockFactSheetDividend;
import com.am.common.investment.model.equity.financial.profitandloss.StockProfitAndLoss;
import com.am.common.investment.model.equity.financial.resultstatement.QuaterlyResult;
import com.am.common.investment.service.StockFinancialPerformanceService;
import com.am.marketdata.processor.service.operation.QuaterlyFinancialDataOperation;
import com.am.marketdata.processor.service.operation.StockBalanceSheetDataOperation;
import com.am.marketdata.processor.service.operation.StockCashFlowDataOperation;
import com.am.marketdata.processor.service.operation.StockFactsheetDividendDataOperation;
import com.am.marketdata.processor.service.operation.StockOverviewDataOperation;
import com.am.marketdata.processor.service.operation.StockProfitAndLossDataOperation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockPerformaceService {

    private final StockOverviewDataOperation stockOverviewDataOperation;
    private final QuaterlyFinancialDataOperation quaterlyFinancialDataOperation;
    private final StockBalanceSheetDataOperation balanceSheetDataOperation;
    private final StockFactsheetDividendDataOperation factSheetDividendDataOperation;
    private final StockProfitAndLossDataOperation profitAndLossDataOperation;
    private final StockCashFlowDataOperation cashFlowDataOperation;
    private final StockFinancialPerformanceService stockFinancialPerformanceService;

    /**
     * Fetch and process stock overview data, then get board of directors
     * @param symbol Stock symbol to process
     * @return Optional BoardOfDirectors data
     */
    public Optional<BoardOfDirectors> fetchAndProcessStockOverview(String symbol) {
        try {
            // Execute the stock overview operation and wait for completion
            CompletableFuture<Boolean> result = stockOverviewDataOperation
                .withSymbol(symbol)
                .executeAsync()
                .exceptionally(ex -> {
                    log.error("Failed to process stock overview for symbol {}: {}", symbol, ex.getMessage());
                    return false;
                });

            // Wait for the operation to complete
            boolean operationSuccess = result.get();

            if (operationSuccess) {
                // Only fetch board of directors if the operation was successful
                return stockFinancialPerformanceService.getBoardOfDirectors(symbol);
            } else {
                log.warn("Stock overview processing failed for symbol: {}", symbol);
                return Optional.empty();
            }

        } catch (InterruptedException | ExecutionException e) {
            log.error("Error processing stock overview for symbol {}: {}", symbol, e.getMessage());
            Thread.currentThread().interrupt(); // Restore interrupted status
            return Optional.empty();
        }
    }

    /**
     * Fetch and process stock overview data, then get board of directors
     * @param symbol Stock symbol to process
     * @return Optional QuaterlyResult data
     */
    public Optional<QuaterlyResult> fetchAndProcessQuaterlyFinancials(String symbol) {
        try {
            // Execute the stock overview operation and wait for completion
            CompletableFuture<Boolean> result = quaterlyFinancialDataOperation
                .withSymbol(symbol) 
                .executeAsync()
                .exceptionally(ex -> {
                    log.error("Failed to process stock overview for symbol {}: {}", symbol, ex.getMessage());
                    return false;
                });

            // Wait for the operation to complete
            boolean operationSuccess = result.get();

            if (operationSuccess) {
                // Only fetch board of directors if the operation was successful
                return stockFinancialPerformanceService.getQuaterlyResult(symbol);
            } else {
                log.warn("Stock overview processing failed for symbol: {}", symbol);
                return Optional.empty();
            }

        } catch (InterruptedException | ExecutionException e) {
            log.error("Error processing stock overview for symbol {}: {}", symbol, e.getMessage());
            Thread.currentThread().interrupt(); // Restore interrupted status
            return Optional.empty();
        }
    }

    public Optional<StockBalanceSheet> fetchAndProcessBalanceSheet(String symbol) {
        try {
            CompletableFuture<Boolean> result = balanceSheetDataOperation
                .withSymbol(symbol) 
                .executeAsync()
                .exceptionally(ex -> {
                    log.error("Failed to process balance sheet for symbol {}: {}", symbol, ex.getMessage());
                    return false;
                });

            boolean operationSuccess = result.get();

            if (operationSuccess) {
                return stockFinancialPerformanceService.getBalanceSheet(symbol);
            } else {
                log.warn("Balance sheet processing failed for symbol: {}", symbol);
                return Optional.empty();
            }

        } catch (InterruptedException | ExecutionException e) {
            log.error("Error processing balance sheet for symbol {}: {}", symbol, e.getMessage());
            Thread.currentThread().interrupt(); // Restore interrupted status
            return Optional.empty();
        }
    }

    public Optional<StockCashFlow> fetchAndProcessCashFlow(String symbol) {
        try {
            CompletableFuture<Boolean> result = cashFlowDataOperation
                .withSymbol(symbol) 
                .executeAsync()
                .exceptionally(ex -> {
                    log.error("Failed to process cash flow for symbol {}: {}", symbol, ex.getMessage());
                    return false;
                });

            boolean operationSuccess = result.get();

            if (operationSuccess) {
                return stockFinancialPerformanceService.getCashFlow(symbol);
            } else {
                log.warn("Cash flow processing failed for symbol: {}", symbol);
                return Optional.empty();
            }

        } catch (InterruptedException | ExecutionException e) {
            log.error("Error processing cash flow for symbol {}: {}", symbol, e.getMessage());
            Thread.currentThread().interrupt(); // Restore interrupted status
            return Optional.empty();
        }
    }

    public Optional<StockFactSheetDividend> fetchAndProcessFactSheetDividend(String symbol) {
        try {
            CompletableFuture<Boolean> result = factSheetDividendDataOperation
                .withSymbol(symbol) 
                .executeAsync()
                .exceptionally(ex -> {
                    log.error("Failed to process fact sheet dividend for symbol {}: {}", symbol, ex.getMessage());
                    return false;
                });

            boolean operationSuccess = result.get();

            if (operationSuccess) {
                return stockFinancialPerformanceService.getFactSheetDividend(symbol);
            } else {
                log.warn("Fact sheet dividend processing failed for symbol: {}", symbol);
                return Optional.empty();
            }

        } catch (InterruptedException | ExecutionException e) {
            log.error("Error processing fact sheet dividend for symbol {}: {}", symbol, e.getMessage());
            Thread.currentThread().interrupt(); // Restore interrupted status
            return Optional.empty();
        }
    }

    public Optional<StockProfitAndLoss> fetchAndProcessProfitAndLoss(String symbol) {
        try {
            CompletableFuture<Boolean> result = profitAndLossDataOperation
                .withSymbol(symbol) 
                .executeAsync()
                .exceptionally(ex -> {
                    log.error("Failed to process profit and loss for symbol {}: {}", symbol, ex.getMessage());
                    return false;
                });

            boolean operationSuccess = result.get();

            if (operationSuccess) {
                return stockFinancialPerformanceService.getProfitAndLoss(symbol);
            } else {
                log.warn("Profit and loss processing failed for symbol: {}", symbol);
                return Optional.empty();
            }

        } catch (InterruptedException | ExecutionException e) {
            log.error("Error processing profit and loss for symbol {}: {}", symbol, e.getMessage());
            Thread.currentThread().interrupt(); // Restore interrupted status
            return Optional.empty();
        }
    }
}
