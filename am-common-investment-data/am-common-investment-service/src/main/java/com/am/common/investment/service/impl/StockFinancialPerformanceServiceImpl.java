package com.am.common.investment.service.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.am.common.investment.model.board.BoardOfDirectors;
import com.am.common.investment.model.equity.financial.balancesheet.StockBalanceSheet;
import com.am.common.investment.model.equity.financial.cashflow.StockCashFlow;
import com.am.common.investment.model.equity.financial.factsheetdividend.StockFactSheetDividend;
import com.am.common.investment.model.equity.financial.profitandloss.StockProfitAndLoss;
import com.am.common.investment.model.equity.financial.resultstatement.QuaterlyResult;
import com.am.common.investment.model.equity.financial.resultstatement.StockFinancialResult;
import com.am.common.investment.persistence.document.BaseDocument;
import com.am.common.investment.persistence.document.companyprofile.BoardOfDirectorsDocument;
import com.am.common.investment.persistence.document.stock.financial.balancesheet.BalanceSheetDocument;
import com.am.common.investment.persistence.document.stock.financial.cashflow.CashFlowDocument;
import com.am.common.investment.persistence.document.stock.financial.factsheetdividend.FactSheetDividendDocument;
import com.am.common.investment.persistence.document.stock.financial.profitandloss.ProfitAndLossDocument;
import com.am.common.investment.persistence.document.stock.financial.result.FinancialResultDocument;
import com.am.common.investment.persistence.document.stock.financial.result.QuaterlyFinancialResultDocument;
import com.am.common.investment.persistence.repository.companyprofile.BoardOfDirectorsRepository;
import com.am.common.investment.persistence.repository.stock.financial.BalanceSheetRepository;
import com.am.common.investment.persistence.repository.stock.financial.CashFlowRepository;
import com.am.common.investment.persistence.repository.stock.financial.FactSheetRepository;
import com.am.common.investment.persistence.repository.stock.financial.FinancialResultRepository;
import com.am.common.investment.persistence.repository.stock.financial.QuaterlyFinancialResultRepository;
import com.am.common.investment.persistence.repository.stock.financial.ProfitAndLossRepository;
import com.am.common.investment.service.DocumentVersionService;
import com.am.common.investment.service.StockFinancialPerformanceService;
import com.am.common.investment.service.mapper.StockFinancialPerformanceMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockFinancialPerformanceServiceImpl implements StockFinancialPerformanceService {
    
    private final BoardOfDirectorsRepository boardOfDirectorsRepository;
    private final ProfitAndLossRepository profitAndLossRepository;
    private final BalanceSheetRepository balanceSheetRepository;
    private final QuaterlyFinancialResultRepository quaterlyFinancialResultRepository;
    private final FinancialResultRepository financialResultRepository;
    private final FactSheetRepository factSheetRepository;
    private final CashFlowRepository cashFlowRepository;
    private final StockFinancialPerformanceMapper mapper;
    private final DocumentVersionService<BaseDocument> versionService;
    
    @Override
    public Optional<BoardOfDirectors> getBoardOfDirectors(String symbol) {
        Optional<BoardOfDirectorsDocument> document = boardOfDirectorsRepository.findBySymbolWithVersionAndTime(symbol);
        return document.map(mapper::toModel);
    }
    
    @Override
    @Transactional
    public BoardOfDirectors saveBoardOfDirectors(BoardOfDirectors boardOfDirectors) {
        BoardOfDirectorsDocument document = mapper.toDocument(boardOfDirectors);
        
        // Increment version before saving
        versionService.incrementVersion(document);
        
        BoardOfDirectorsDocument savedDocument = boardOfDirectorsRepository.save(document);
        return mapper.toModel(savedDocument);
    }
    
    @Override
    public Optional<StockProfitAndLoss> getProfitAndLoss(String symbol) {
        Optional<ProfitAndLossDocument> document = profitAndLossRepository.findBySymbolWithVersionAndTime(symbol);
        return document.map(mapper::toModel);
    }
    
    @Override
    @Transactional
    public StockProfitAndLoss saveProfitAndLoss(StockProfitAndLoss profitAndLoss) {
        ProfitAndLossDocument document = mapper.toDocument(profitAndLoss);
        
        // Increment version before saving
        versionService.incrementVersion(document);
        
        ProfitAndLossDocument savedDocument = profitAndLossRepository.save(document);
        return mapper.toModel(savedDocument);
    }
    
    @Override
    public Optional<StockBalanceSheet> getBalanceSheet(String symbol) {
        Optional<BalanceSheetDocument> document = balanceSheetRepository.findBySymbolWithVersionAndTime(symbol);
        return document.map(mapper::toModel);
    }
    
    @Override
    @Transactional
    public StockBalanceSheet saveBalanceSheet(StockBalanceSheet balanceSheet) {
        BalanceSheetDocument document = mapper.toDocument(balanceSheet);
        
        // Increment version before saving
        versionService.incrementVersion(document);
        
        BalanceSheetDocument savedDocument = balanceSheetRepository.save(document);
        return mapper.toModel(savedDocument);
    }
    
    @Override
    public Optional<QuaterlyResult> getQuaterlyResult(String symbol) {
        Optional<QuaterlyFinancialResultDocument> document = quaterlyFinancialResultRepository.findBySymbolWithVersionAndTime(symbol);
        return document.map(mapper::toModel);
    }
    
    @Override
    @Transactional
    public QuaterlyResult saveQuaterlyResult(QuaterlyResult quaterlyResult) {
        QuaterlyFinancialResultDocument document = mapper.toDocument(quaterlyResult);
        
        // Increment version before saving
        versionService.incrementVersion(document);
        
        QuaterlyFinancialResultDocument savedDocument = quaterlyFinancialResultRepository.save(document);
        return mapper.toModel(savedDocument);
    }
    
    @Override
    public Optional<StockFactSheetDividend> getFactSheetDividend(String symbol) {
        Optional<FactSheetDividendDocument> document = factSheetRepository.findBySymbolWithVersionAndTime(symbol);
        return document.map(mapper::toModel);
    }
    
    @Override
    @Transactional
    public StockFactSheetDividend saveFactSheetDividend(StockFactSheetDividend factSheetDividend) {
        FactSheetDividendDocument document = mapper.toDocument(factSheetDividend);
        
        // Increment version before saving
        versionService.incrementVersion(document);
        
        FactSheetDividendDocument savedDocument = factSheetRepository.save(document);
        return mapper.toModel(savedDocument);
    }
    
    @Override
    public Optional<StockCashFlow> getCashFlow(String symbol) {
        Optional<CashFlowDocument> document = cashFlowRepository.findBySymbolWithVersionAndTime(symbol);
        return document.map(mapper::toModel);
    }
    
    @Override
    @Transactional
    public StockCashFlow saveCashFlow(StockCashFlow cashFlow) {
        CashFlowDocument document = mapper.toDocument(cashFlow);
        
        // Increment version before saving
        versionService.incrementVersion(document);
        
        CashFlowDocument savedDocument = cashFlowRepository.save(document);
        return mapper.toModel(savedDocument);
    }

    @Override
    public Optional<StockFinancialResult> getFinancialResult(String symbol) {
        Optional<FinancialResultDocument> document = financialResultRepository.findBySymbolWithVersionAndTime(symbol);
        return document.map(mapper::toModel);
    }

    @Override
    public StockFinancialResult saveFinancialResult(StockFinancialResult financialResult) {
        FinancialResultDocument document = mapper.toDocument(financialResult);
        
        // Increment version before saving
        versionService.incrementVersion(document);
        
        FinancialResultDocument savedDocument = financialResultRepository.save(document);
        return mapper.toModel(savedDocument);
    }
}
