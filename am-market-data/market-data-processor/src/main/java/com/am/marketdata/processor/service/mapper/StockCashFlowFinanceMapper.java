package com.am.marketdata.processor.service.mapper;

import com.am.common.investment.model.equity.financial.BaseModel;
import com.am.common.investment.model.equity.financial.cashflow.CashFlow;
import com.am.common.investment.model.equity.financial.cashflow.StockCashFlow;
import com.am.common.investment.model.equity.financial.resultstatement.FinancialResult;
import com.am.common.investment.model.equity.metrics.CashFlowFinancingMetrics;
import com.am.common.investment.model.equity.metrics.CashFlowInvestingMetrics;
import com.am.common.investment.model.equity.metrics.CashFlowOperatingMetrics;
import com.am.common.investment.model.equity.metrics.CashFlowOperatingMetrics.CashFlowOperatingMetricsBuilder;
import com.am.common.investment.model.equity.metrics.CostMetrics;
import com.am.common.investment.model.equity.metrics.EpsMetrics;
import com.am.common.investment.model.equity.metrics.GrowthMetrics;
import com.am.common.investment.model.equity.metrics.ProfitMetrics;
import com.am.common.investment.model.equity.metrics.TaxMetrics;
import com.am.marketdata.common.model.tradeB.financials.cashflow.CashFlowData;
import com.am.marketdata.common.model.tradeB.financials.cashflow.CashFlowMetrics;
import com.am.marketdata.common.model.tradeB.financials.cashflow.CashFlowResponse;
import com.am.marketdata.common.model.tradeB.financials.results.QuarterlyFinancialMetrics;
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
public class StockCashFlowFinanceMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @SneakyThrows
    public CashFlowResponse parse(String jsonData) {
        return objectMapper.readValue(jsonData, 
            TypeFactory.defaultInstance().constructType(CashFlowResponse.class));
    }

    private final BaseModelMapper baseModelMapper = new BaseModelMapper();
    
    /**
     * Create CashFlowResponse from symbol and CashFlowResponse object
     * 
     * @param symbol Stock symbol
     * @param financials CashFlowResponse object
     * @return StockCashFlow object
     */
    public StockCashFlow toCashFlow(String symbol, CashFlowResponse cashFlowResponse) {
        if (cashFlowResponse == null) {
            return null;
        }
        BaseModel baseModel = baseModelMapper.getBaseModel(symbol, "TradeB");
        var StockCashFlowBuilder = StockCashFlow.builder()
            .id(baseModel.getId())
            .symbol(baseModel.getSymbol())
            .source(baseModel.getSource())
            .audit(baseModel.getAudit())
            .cashFlow(toCashFlow(cashFlowResponse.getStock()));

        return StockCashFlowBuilder.build();
    }

    private List<CashFlow> toCashFlow(CashFlowData cashFlow) {
        if (cashFlow == null) {
            return null;
        }
        return cashFlow.getQuarterKeys().stream()
            .map(quarterKey -> toCashFlow(cashFlow.getMetrics(quarterKey)))
            .collect(Collectors.toList());
    }

    private CashFlow toCashFlow(CashFlowMetrics cashFlow) {
        if (cashFlow == null) {
            return null;
        }
        var CashFlowBuilder = CashFlow.builder()
            .yearEnd(cashFlow.getYearEnd())
            .operatingMetrics(toOperatingMetrics(cashFlow))
            .investingMetrics(toInvestingMetrics(cashFlow))
            .financingMetrics(toFinancingMetrics(cashFlow))
            .freeCashFlow(cashFlow.getFreeCashFlow());

        return CashFlowBuilder.build();
    }

    private CashFlowOperatingMetrics toOperatingMetrics(CashFlowMetrics cashFlow) {
        return CashFlowOperatingMetrics.builder()
            .cashFromOperatingActivities(cashFlow.getProfitBeforeTax())
            .profitFromOperations(cashFlow.getDepreciationAndAmortization())
            .interestReceived(cashFlow.getChangeInWorkingCapital())
            .dividendReceived(cashFlow.getChangeInWorkingCapital())
            .directTaxes(cashFlow.getChangeInWorkingCapital())
            .exceptionalCfItems(cashFlow.getChangeInWorkingCapital())
            .inventory(cashFlow.getCashFromOperatingActivities())
            .build();
    }

    private CashFlowInvestingMetrics toInvestingMetrics(CashFlowMetrics cashFlow) {
        return CashFlowInvestingMetrics.builder()
            .cashFromInvestingActivities(cashFlow.getCashFromInvestingActivities())
            // .fixedAssetsPurchased(cashFlow.getFixedAssetsPurchased())
            // .fixedAssetsSold(cashFlow.getFixedAssetsSold())
            // .investmentsPurchased(cashFlow.getInvestmentsPurchased())
            // .investmentsSold(cashFlow.getInvestmentsSold())
            // .otherInvestingItems(cashFlow.getOtherInvestingItems())
            .build();
    }

    private CashFlowFinancingMetrics toFinancingMetrics(CashFlowMetrics cashFlow) {
        return CashFlowFinancingMetrics.builder()
            //.cashFromFinancingActivities(cashFlow.getCashFromFinancingActivities())
            // .netBorrowings(cashFlow.getNetBorrowings())
            // .repaymentOfDebt(cashFlow.getRepaymentOfDebt())
            // .issuanceOfShares(cashFlow.getIssuanceOfShares())
            // .repurchaseOfShares(cashFlow.getRepurchaseOfShares())
            // .dividendsPaid(cashFlow.getDividendsPaid())
            // .otherFinancingActivities(cashFlow.getOtherFinancingActivities())
            // .cashFromFinancingActivities(cashFlow.getCashFromFinancingActivities())
            .build();
    }
}
