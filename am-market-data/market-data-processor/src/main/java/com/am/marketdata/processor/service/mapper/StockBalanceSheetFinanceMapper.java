package com.am.marketdata.processor.service.mapper;

import com.am.common.investment.model.equity.financial.BaseModel;
import com.am.common.investment.model.equity.financial.balancesheet.BalanceSheet;
import com.am.common.investment.model.equity.financial.balancesheet.StockBalanceSheet;
import com.am.common.investment.model.equity.financial.resultstatement.FinancialResult;
import com.am.common.investment.model.equity.financial.resultstatement.QuaterlyResult;
import com.am.common.investment.model.equity.metrics.AssetsMetrics;
import com.am.common.investment.model.equity.metrics.CostMetrics;
import com.am.common.investment.model.equity.metrics.EpsMetrics;
import com.am.common.investment.model.equity.metrics.EquityMetrics;
import com.am.common.investment.model.equity.metrics.GrowthMetrics;
import com.am.common.investment.model.equity.metrics.LiabilitiesMetrics;
import com.am.common.investment.model.equity.metrics.ProfitMetrics;
import com.am.common.investment.model.equity.metrics.TaxMetrics;
import com.am.marketdata.common.model.tradeB.financials.balancesheet.BalanceSheetData;
import com.am.marketdata.common.model.tradeB.financials.balancesheet.BalanceSheetMetrics;
import com.am.marketdata.common.model.tradeB.financials.balancesheet.BalanceSheetResponse;
import com.am.marketdata.common.model.tradeB.financials.results.QuarterlyFinancialMetrics;
import com.am.marketdata.common.model.tradeB.financials.results.QuaterlyFinancialStatementResponse;
import com.am.marketdata.common.model.tradeB.financials.results.StockFinancialData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Mapper for converting between BoardOfDirectors and BoardOfDirector
 */
@Component
@Slf4j
public class StockBalanceSheetFinanceMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @SneakyThrows
    public BalanceSheetResponse parse(String jsonData) {
        return objectMapper.readValue(jsonData, 
            TypeFactory.defaultInstance().constructType(BalanceSheetResponse.class));
    }

    private final BaseModelMapper baseModelMapper = new BaseModelMapper();

    
    /**
     * Create QuarterlyFinancialMetrics from symbol and QuaterlyFinancialStatementResponse object
     * 
     * @param symbol Stock symbol
     * @param financials BalanceSheetResponse object
     * @return StockBalanceSheet object
     */
    public StockBalanceSheet toBalanceSheet(String symbol, BalanceSheetResponse balanceSheetResponse) {
        if (balanceSheetResponse == null) {
            return null;
        }
        BaseModel baseModel = baseModelMapper.getBaseModel(symbol, "TradeB");
        var StockBalanceSheetBuilder = StockBalanceSheet.builder()
            .id(baseModel.getId())
            .symbol(baseModel.getSymbol())
            .source(baseModel.getSource())
            .audit(baseModel.getAudit())
            .balanceSheet(toBalanceSheet(balanceSheetResponse.getStock()));

        return StockBalanceSheetBuilder.build();
    }

    private List<BalanceSheet> toBalanceSheet(BalanceSheetData balanceSheet) {
        if (balanceSheet == null) {
            return null;
        }
        return balanceSheet.getQuarterKeys().stream()
            .map(quarterKey -> toBalanceSheet(quarterKey, balanceSheet.getMetrics(quarterKey)))
            .collect(Collectors.toList());
    }

    private BalanceSheet toBalanceSheet(String key, BalanceSheetMetrics balanceSheet) {
        if (balanceSheet == null) {
            return null;
        }
        var BalanceSheetBuilder = BalanceSheet.builder()
            .yearEnd(balanceSheet.getYearEnd())
            .quarter(key)
            .assets(balanceSheet.getAssets())
            .liabilitiesEquity(balanceSheet.getLiabilitiesEquity())
            .totalDebits(balanceSheet.getTotalDebits())
            .assetsMetrics(toAssetsMetrics(balanceSheet))
            .liabilitiesMetrics(toLiabilitiesMetrics(balanceSheet))
            .equityMetrics(toEquityMetrics(balanceSheet));

        return BalanceSheetBuilder.build();
    }

    private AssetsMetrics toAssetsMetrics(BalanceSheetMetrics balanceSheet) {
        return AssetsMetrics.builder()
            .inventory(balanceSheet.getInventory())
            .fixedAssets(balanceSheet.getFixedAssets())
            .capitalWorkInProgress(balanceSheet.getCapitalWorkInProgress())
            .intangibleAssets(balanceSheet.getIntangibleAssets())
            .currentAssets(balanceSheet.getCurrentAssets())
            .accountsReceivables(balanceSheet.getAccountsReceivables())
            .shortTermInvestments(balanceSheet.getShortTermInvestments())
            .cashAndBankBalances(balanceSheet.getCashAndBankBalances())
            .nonCurrentAssets(balanceSheet.getNonCurrentAssets())
            .intangibleAssetsUnderDev(balanceSheet.getIntangibleAssetsUnderDev())
            .longTermInvestments(balanceSheet.getLongTermInvestments())
            .netBlock(balanceSheet.getNetBlock())
            .build();
    }

    private LiabilitiesMetrics toLiabilitiesMetrics(BalanceSheetMetrics balanceSheet) {
        return LiabilitiesMetrics.builder()
            .provisions(balanceSheet.getProvisions())
            .shortTermBorrowings(balanceSheet.getShortTermBorrowings())
            .currentLiabilities(balanceSheet.getCurrentLiabilities())
            .accountPayables(balanceSheet.getAccountPayables())
            .nonCurrentLiabilities(balanceSheet.getNonCurrentLiabilities())
            .otherCurrentLiabilities(balanceSheet.getOtherCurrentLiabilities())
            .build();
    }

    private EquityMetrics toEquityMetrics(BalanceSheetMetrics balanceSheet) {
        return EquityMetrics.builder()
            .shareCapital(balanceSheet.getShareCapital())
            .preferenceCapital(balanceSheet.getPreferenceCapital())
            .equityCapital(balanceSheet.getEquityCapital())
            .shareholdersFunds(balanceSheet.getShareholdersFunds())
            .reserves(balanceSheet.getReserves())
            .build();
    }
}
