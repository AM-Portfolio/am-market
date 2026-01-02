package com.am.common.investment.service;

import com.am.common.investment.model.board.BoardOfDirectors;
import com.am.common.investment.model.equity.financial.balancesheet.StockBalanceSheet;
import com.am.common.investment.model.equity.financial.cashflow.StockCashFlow;
import com.am.common.investment.model.equity.financial.factsheetdividend.StockFactSheetDividend;
import com.am.common.investment.model.equity.financial.profitandloss.StockProfitAndLoss;
import com.am.common.investment.model.equity.financial.resultstatement.QuaterlyResult;
import com.am.common.investment.model.equity.financial.resultstatement.StockFinancialResult;

import java.util.Optional;

/**
 * Service for managing stock financial performance data
 */
public interface StockFinancialPerformanceService {
    Optional<BoardOfDirectors> getBoardOfDirectors(String symbol);
    BoardOfDirectors saveBoardOfDirectors(BoardOfDirectors boardOfDirectors);

    Optional<StockProfitAndLoss> getProfitAndLoss(String symbol);
    StockProfitAndLoss saveProfitAndLoss(StockProfitAndLoss profitAndLoss);

    Optional<StockBalanceSheet> getBalanceSheet(String symbol);
    StockBalanceSheet saveBalanceSheet(StockBalanceSheet balanceSheet);

    Optional<StockFactSheetDividend> getFactSheetDividend(String symbol);
    StockFactSheetDividend saveFactSheetDividend(StockFactSheetDividend factSheetDividend);

    Optional<StockCashFlow> getCashFlow(String symbol);
    StockCashFlow saveCashFlow(StockCashFlow cashFlow);

    Optional<QuaterlyResult> getQuaterlyResult(String symbol);
    QuaterlyResult saveQuaterlyResult(QuaterlyResult quaterlyResult);

    Optional<StockFinancialResult> getFinancialResult(String symbol);
    StockFinancialResult saveFinancialResult(StockFinancialResult financialResult);
}
