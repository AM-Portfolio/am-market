package com.am.marketdata.processor.service.mapper;

import com.am.common.investment.model.equity.financial.BaseModel;
import com.am.common.investment.model.equity.financial.profitandloss.ProfitAndLoss;
import com.am.common.investment.model.equity.financial.profitandloss.ProfitAndLoss.ProfitAndLossBuilder;
import com.am.common.investment.model.equity.financial.profitandloss.StockProfitAndLoss;
import com.am.common.investment.model.equity.financial.resultstatement.FinancialResult;
import com.am.common.investment.model.equity.financial.resultstatement.QuaterlyResult;
import com.am.common.investment.model.equity.metrics.CostMetrics;
import com.am.common.investment.model.equity.metrics.EpsMetrics;
import com.am.common.investment.model.equity.metrics.GrowthMetrics;
import com.am.common.investment.model.equity.metrics.ProfitMetrics;
import com.am.common.investment.model.equity.metrics.TaxMetrics;
import com.am.marketdata.common.model.tradeB.financials.profitloss.ProfitLossData;
import com.am.marketdata.common.model.tradeB.financials.profitloss.ProfitLossMetrics;
import com.am.marketdata.common.model.tradeB.financials.profitloss.ProfitLossStatementResponse;
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
public class StockProfitLossFinanceMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @SneakyThrows
    public ProfitLossStatementResponse parse(String jsonData) {
        return objectMapper.readValue(jsonData, 
            TypeFactory.defaultInstance().constructType(ProfitLossStatementResponse.class));
    }

    private final BaseModelMapper baseModelMapper = new BaseModelMapper();
    
    public StockProfitAndLoss toStockProfitAndLoss(String symbol, ProfitLossStatementResponse financials) {
        if (financials == null) {
            return null;
        }
        BaseModel baseModel = baseModelMapper.getBaseModel(symbol, "TradeB");
        var StockProfitAndLossBuilder = StockProfitAndLoss.builder()
            .id(baseModel.getId())
            .symbol(baseModel.getSymbol())
            .source(baseModel.getSource())
            .audit(baseModel.getAudit())
            .profitAndLoss(toProfitAndLoss(financials.getStock()));

        return StockProfitAndLossBuilder.build();
    }

    private List<ProfitAndLoss> toProfitAndLoss(ProfitLossData financials) {
        if (financials == null) {
            return null;
        }
        return financials.getQuarterKeys().stream()
            .map(quarterKey -> toProfitAndLoss(quarterKey, financials.getMetrics(quarterKey)))
            .collect(Collectors.toList());
    }

    private ProfitAndLoss toProfitAndLoss(String quarterKey, ProfitLossMetrics profitLoss) {
        if (profitLoss == null) {
            return null;
        }
        var ProfitAndLossBuilder = ProfitAndLoss.builder()
            .yearEnd(quarterKey)
            .costMetrics(toCostMetrics(profitLoss))
            .totalRevenue(profitLoss.getTotalRevenue())
            .operatingRevenue(profitLoss.getOperatingRevenue())
            .growthMetrics(toGrowthMetrics(profitLoss))
            .profitMetrics(toProfitMetrics(profitLoss))
            .taxMetrics(toTaxMetrics(profitLoss))
            .costMetrics(toCostMetrics(profitLoss))
            .epsMetrics(toEpsMetrics(profitLoss));

        return ProfitAndLossBuilder.build();
    }

    private CostMetrics toCostMetrics(ProfitLossMetrics profitLoss) {
        return CostMetrics.builder()
            .totalExpenditure(profitLoss.getTotalExpenditure())
            .manufacturingCost(profitLoss.getManufacturingCost())
            .employeeCost(profitLoss.getEmployeeCost())
            .interest(profitLoss.getInterest())
            .operatingExpenses(profitLoss.getOperatingExpenses())
            .depreciationAndAmortization(profitLoss.getDepreciationAndAmortization())
            .build();
    }

    private ProfitMetrics toProfitMetrics(ProfitLossMetrics profitLoss) {
        return ProfitMetrics.builder()
            .netProfit(profitLoss.getNetProfit())
            .operationProfit(profitLoss.getOperationProfit())
            .profitBeforeTax(profitLoss.getProfitBeforeTax())
            .profitAfterTax(profitLoss.getProfitAfterTax())
            .minorityShare(profitLoss.getMinorityShare())
            .build();
    }

    private TaxMetrics toTaxMetrics(ProfitLossMetrics profitLoss) {
        return TaxMetrics.builder()
            .tax(profitLoss.getTax())
            .taxPer(profitLoss.getTaxPer())
            .build();
    }

    private EpsMetrics toEpsMetrics(ProfitLossMetrics profitLoss) {
        return EpsMetrics.builder()
            .basicEpsRs(profitLoss.getBasicEpsRs())
            .dilutedEpsRs(profitLoss.getDilutedEpsRs())
            .build();
    }

    private GrowthMetrics toGrowthMetrics(ProfitLossMetrics profitLoss) {
        return GrowthMetrics.builder()
            .revenueGrowthPer(profitLoss.getRevenueGrowthPer())
            .netProfitGrowth(profitLoss.getNetProfitGrowth())
            .netProfitMarginGrowth(profitLoss.getNetProfitMarginGrowth())
            .build();
    }

}
