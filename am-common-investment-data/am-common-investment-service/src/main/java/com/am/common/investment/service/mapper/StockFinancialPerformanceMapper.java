package com.am.common.investment.service.mapper;

import com.am.common.investment.model.board.BoardOfDirectors;
import com.am.common.investment.model.equity.financial.balancesheet.StockBalanceSheet;
import com.am.common.investment.model.equity.financial.cashflow.StockCashFlow;
import com.am.common.investment.model.equity.financial.factsheetdividend.StockFactSheetDividend;
import com.am.common.investment.model.equity.financial.profitandloss.StockProfitAndLoss;
import com.am.common.investment.model.equity.financial.resultstatement.QuaterlyResult;
import com.am.common.investment.model.equity.financial.resultstatement.StockFinancialResult;
import com.am.common.investment.persistence.document.companyprofile.BoardOfDirectorsDocument;
import com.am.common.investment.persistence.document.stock.financial.balancesheet.BalanceSheetDocument;
import com.am.common.investment.persistence.document.stock.financial.cashflow.CashFlowDocument;
import com.am.common.investment.persistence.document.stock.financial.factsheetdividend.FactSheetDividendDocument;
import com.am.common.investment.persistence.document.stock.financial.profitandloss.ProfitAndLossDocument;
import com.am.common.investment.persistence.document.stock.financial.result.FinancialResultDocument;
import com.am.common.investment.persistence.document.stock.financial.result.QuaterlyFinancialResultDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.UUID;

@Component
public class StockFinancialPerformanceMapper {

    // Board of Directors mapping
    public BoardOfDirectors toModel(BoardOfDirectorsDocument document) {
        if (document == null) {
            return null;
        }
        
        BoardOfDirectors model = new BoardOfDirectors();
        model.setId(document.getId());
        model.setSymbol(document.getSymbol());
        model.setVersion(document.getVersion());
        model.setAudit(document.getAudit());
        model.setSource(document.getSource());
        
        if (document.getDirectors() != null) {
            model.setDirectors(document.getDirectors());
        } else {
            model.setDirectors(new ArrayList<>());
        }
        
        return model;
    }
    
    public BoardOfDirectorsDocument toDocument(BoardOfDirectors model) {
        if (model == null) {
            return null;
        }
        
        BoardOfDirectorsDocument document = new BoardOfDirectorsDocument();
        document.setId(UUID.randomUUID().toString());
        document.setSymbol(model.getSymbol());
        document.setVersion(model.getVersion());
        document.setAudit(model.getAudit());
        document.setSource(model.getSource());
        
        if (model.getDirectors() != null) {
            document.setDirectors(model.getDirectors());
        } else {
            document.setDirectors(new ArrayList<>());
        }
        
        return document;
    }

    // Cash Flow mapping
    public StockCashFlow toModel(CashFlowDocument document) {
        if (document == null) {
            return null;
        }
        
        StockCashFlow model = new StockCashFlow();
        model.setId(document.getId());
        model.setSymbol(document.getSymbol());
        model.setVersion(document.getVersion());
        model.setAudit(document.getAudit());
        model.setSource(document.getSource());
        model.setCashFlow(document.getCashFlow());
        return model;
    }
    
    public CashFlowDocument toDocument(StockCashFlow model) {
        if (model == null) {
            return null;
        }
        
        CashFlowDocument document = new CashFlowDocument();
        document.setId(UUID.randomUUID().toString());
        document.setSymbol(model.getSymbol());
        document.setVersion(model.getVersion());
        document.setAudit(model.getAudit());
        document.setSource(model.getSource());
        document.setCashFlow(model.getCashFlow());
        
        return document;
    }

    // Balance Sheet mapping
    public StockBalanceSheet toModel(BalanceSheetDocument document) {
        if (document == null) {
            return null;
        }
        
        StockBalanceSheet model = new StockBalanceSheet();
        model.setId(document.getId());
        model.setSymbol(document.getSymbol());
        model.setVersion(document.getVersion());
        model.setAudit(document.getAudit());
        model.setSource(document.getSource());
        model.setBalanceSheet(document.getBalanceSheet());
        
        return model;
    }
    
    public BalanceSheetDocument toDocument(StockBalanceSheet model) {
        if (model == null) {
            return null;
        }
        
        BalanceSheetDocument document = new BalanceSheetDocument();
        document.setId(UUID.randomUUID().toString());
        document.setSymbol(model.getSymbol());
        document.setVersion(model.getVersion());
        document.setAudit(model.getAudit());
        document.setSource(model.getSource());
        document.setBalanceSheet(model.getBalanceSheet());
        
        return document;
    }

    // Profit and Loss mapping
    public StockProfitAndLoss toModel(ProfitAndLossDocument document) {
        if (document == null) {
            return null;
        }
        
        StockProfitAndLoss model = new StockProfitAndLoss();
        model.setId(document.getId());
        model.setSymbol(document.getSymbol());
        model.setVersion(document.getVersion());
        model.setAudit(document.getAudit());
        model.setSource(document.getSource());
        model.setProfitAndLoss(document.getProfitAndLoss());
        
        return model;
    }
    
    public ProfitAndLossDocument toDocument(StockProfitAndLoss model) {
        if (model == null) {
            return null;
        }
        
        ProfitAndLossDocument document = new ProfitAndLossDocument();
        document.setId(UUID.randomUUID().toString());
        document.setSymbol(model.getSymbol());
        document.setVersion(model.getVersion());
        document.setAudit(model.getAudit());
        document.setSource(model.getSource());
        document.setProfitAndLoss(model.getProfitAndLoss());
        
        return document;
    }

    // FactSheet Dividend mapping
    public StockFactSheetDividend toModel(FactSheetDividendDocument document) {
        if (document == null) {
            return null;
        }
        
        StockFactSheetDividend model = new StockFactSheetDividend();
        model.setId(document.getId());
        model.setSymbol(document.getSymbol());
        model.setVersion(document.getVersion());
        model.setAudit(document.getAudit());
        model.setSource(document.getSource());
        model.setFactSheetDividend(document.getFactSheetDividend());
        
        return model;
    }
    
    public FactSheetDividendDocument toDocument(StockFactSheetDividend model) {
        if (model == null) {
            return null;
        }
        
        FactSheetDividendDocument document = new FactSheetDividendDocument();
        document.setId(UUID.randomUUID().toString());
        document.setSymbol(model.getSymbol());
        document.setVersion(model.getVersion());
        document.setAudit(model.getAudit());
        document.setSource(model.getSource());
        document.setFactSheetDividend(model.getFactSheetDividend());
        
        return document;
    }

    // Financial Result mapping
    public QuaterlyResult toModel(QuaterlyFinancialResultDocument document) {
        if (document == null) {
            return null;
        }
        
        QuaterlyResult model = new QuaterlyResult();
        model.setId(document.getId());
        model.setSymbol(document.getSymbol());
        model.setVersion(document.getVersion());
        model.setAudit(document.getAudit());
        model.setSource(document.getSource());
        model.setFinancialResults(document.getFinancialResults());
        
        return model;
    }
    
    public QuaterlyFinancialResultDocument toDocument(QuaterlyResult model) {
        if (model == null) {
            return null;
        }
        
        QuaterlyFinancialResultDocument document = new QuaterlyFinancialResultDocument();
        document.setId(UUID.randomUUID().toString());
        document.setSymbol(model.getSymbol());
        document.setVersion(model.getVersion());
        document.setAudit(model.getAudit());
        document.setSource(model.getSource());
        document.setFinancialResults(model.getFinancialResults());
        
        return document;
    }

    public StockFinancialResult toModel(FinancialResultDocument document) {
        if (document == null) {
            return null;
        }
        
        StockFinancialResult model = new StockFinancialResult();
        model.setId(document.getId());
        model.setSymbol(document.getSymbol());
        model.setVersion(document.getVersion());
        model.setAudit(document.getAudit());
        model.setSource(document.getSource());
        model.setFinancialResults(document.getFinancialResults());
        
        return model;
    }
    
    public FinancialResultDocument toDocument(StockFinancialResult model) {
        if (model == null) {
            return null;
        }
        
        FinancialResultDocument document = new FinancialResultDocument();
        document.setId(UUID.randomUUID().toString());
        document.setSymbol(model.getSymbol());
        document.setVersion(model.getVersion());
        document.setAudit(model.getAudit());
        document.setSource(model.getSource());
        document.setFinancialResults(model.getFinancialResults());
        
        return document;
    }
}