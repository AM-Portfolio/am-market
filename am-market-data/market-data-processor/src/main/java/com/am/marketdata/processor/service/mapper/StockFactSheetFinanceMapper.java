package com.am.marketdata.processor.service.mapper;

import com.am.common.investment.model.equity.EquityFundamental.FinancialRatios;
import com.am.common.investment.model.equity.financial.BaseModel;
import com.am.common.investment.model.equity.financial.factsheetdividend.FactSheetDividend;
import com.am.common.investment.model.equity.financial.factsheetdividend.StockFactSheetDividend;
import com.am.common.investment.model.equity.financial.profitandloss.ProfitAndLoss;
import com.am.common.investment.model.equity.financial.resultstatement.FinancialResult;
import com.am.common.investment.model.equity.metrics.CostMetrics;
import com.am.common.investment.model.equity.metrics.EpsMetrics;
import com.am.common.investment.model.equity.metrics.GrowthMetrics;
import com.am.common.investment.model.equity.metrics.ProfitMetrics;
import com.am.common.investment.model.equity.metrics.TaxMetrics;
import com.am.marketdata.common.model.tradeB.financials.dividend.DividendData;
import com.am.marketdata.common.model.tradeB.financials.dividend.DividendMetrics;
import com.am.marketdata.common.model.tradeB.financials.dividend.FactSheetDividendResponse;
import com.am.marketdata.common.model.tradeB.financials.profitloss.ProfitLossData;
import com.am.marketdata.common.model.tradeB.financials.results.QuarterlyFinancialMetrics;
import com.am.marketdata.common.model.tradeB.financials.results.StockFinancialData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class StockFactSheetFinanceMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @SneakyThrows
    public FactSheetDividendResponse parse(String jsonData) {
        return objectMapper.readValue(jsonData, 
            TypeFactory.defaultInstance().constructType(FactSheetDividendResponse.class));
    }

    private final BaseModelMapper baseModelMapper = new BaseModelMapper();
    
    /**
     * Create QuarterlyFinancialMetrics from symbol and QuaterlyFinancialStatementResponse object
     * 
     * @param symbol Stock symbol
     * @param financials FactSheetDividendResponse object
     * @return StockFactSheetDividend object
     */
    public StockFactSheetDividend toFactSheetDividend(String symbol, FactSheetDividendResponse financials) {
        if (financials == null) {
            return null;
        }
        BaseModel baseModel = baseModelMapper.getBaseModel(symbol, "TradeB");
        var StockFactSheetDividendBuilder = StockFactSheetDividend.builder()
            .id(baseModel.getId())
            .symbol(baseModel.getSymbol())
            .source(baseModel.getSource())
            .audit(baseModel.getAudit())
            .factSheetDividend(toFactSheetDividend(financials.getStock()));

        return StockFactSheetDividendBuilder.build();
    }

    private List<FactSheetDividend> toFactSheetDividend(DividendData financials) {
        if (financials == null) {
            return null;
        }
        return financials.getQuarterKeys().stream()
            .map(quarterKey -> toFactSheetDividend(quarterKey, financials.getMetrics(quarterKey)))
            .collect(Collectors.toList());
    }

    private FactSheetDividend toFactSheetDividend(String quarterKey, DividendMetrics dividendMetrics) {
        if (dividendMetrics == null) {
            return null;
        }
        var FactSheetDividendBuilder = FactSheetDividend.builder()
            .yearEnd(quarterKey)
            //.growthMetrics(toGrowthMetrics(dividendMetrics))
            //.financialRatios(toFinancialRatios(dividendMetrics))
            .dividendMetrics(toDividendMetrics(dividendMetrics))
            .assetTurnoverRatio(dividendMetrics.getAssetTurnoverRatio())
            .workingCapitalDays(dividendMetrics.getWorkingCapitalDays())
            .inventoryTurnoverRatio(dividendMetrics.getInventoryTurnoverRatio())
            .adjDividendPerShare(dividendMetrics.getAdjDividendPerShare())
            .adjEarningsPerShare(dividendMetrics.getAdjEarningsPerShare())
            .enterpriseValue(dividendMetrics.getEnterpriseValue())
            .pegRatio(dividendMetrics.getPegRatio())
            .priceSalesRatio(dividendMetrics.getPriceSalesRatio())
            .freeCashFlowPerShare(dividendMetrics.getFreeCashFlowPerShare())
            .freeCashFlowYield(dividendMetrics.getFreeCashFlowYield());

        return FactSheetDividendBuilder.build();
    }

    private FinancialRatios toFinancialRatios(DividendMetrics dividendMetrics) {
        return FinancialRatios.builder()
            // .totalExpenditure(dividendMetrics.getTotalExpenses())
            // .manufacturingCost(dividendMetrics.getOperatingRevenue())
            // .employeeCost(dividendMetrics.getEmployeeCost())
            // .interest(dividendMetrics.getInterest())
            // .operatingExpenses(dividendMetrics.getOperatingExpenses())
            // .depreciationAndAmortization(dividendMetrics.getDepreciationAndAmortization())
            .build();
    }

    private com.am.common.investment.model.equity.metrics.DividendMetrics toDividendMetrics(DividendMetrics dividendMetrics) {
        return com.am.common.investment.model.equity.metrics.DividendMetrics.builder()
            .dividendPerShare(dividendMetrics.getDividendPerShare())
            .dividendYield(dividendMetrics.getDividendYield())
            .adjDividendPerShare(dividendMetrics.getAdjDividendPerShare())
            .dividendPayoutRatio(dividendMetrics.getDividendPayoutRatio())
            .dividendYield(dividendMetrics.getDividendYield())
            .freeCashFlowPerShare(dividendMetrics.getFreeCashFlowPerShare())
            .freeCashFlowYield(dividendMetrics.getFreeCashFlowYield())
            .build();
    }

    private GrowthMetrics toGrowthMetrics(DividendMetrics dividendMetrics) {
        return GrowthMetrics.builder()
            .revenueGrowthPer(null)
            .netProfitGrowth(null)
            .netProfitMarginGrowth(null)
            .patMarginGrowth(null)
            .build();
    }

}
