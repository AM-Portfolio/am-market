// package com.am.marketdata.processor.service.mapper;

// import com.am.common.investment.model.equity.financial.BaseModel;
// import com.am.common.investment.model.equity.financial.resultstatement.FinancialResult;
// import com.am.common.investment.model.equity.financial.resultstatement.QuaterlyResult;
// import com.am.common.investment.model.equity.metrics.CostMetrics;
// import com.am.common.investment.model.equity.metrics.EpsMetrics;
// import com.am.common.investment.model.equity.metrics.GrowthMetrics;
// import com.am.common.investment.model.equity.metrics.ProfitMetrics;
// import com.am.common.investment.model.equity.metrics.TaxMetrics;
// import com.am.marketdata.common.model.tradeB.financials.results.QuarterlyFinancialMetrics;
// import com.am.marketdata.common.model.tradeB.financials.results.QuaterlyFinancialStatementResponse;
// import com.am.marketdata.common.model.tradeB.financials.results.StockFinancialData;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.fasterxml.jackson.databind.type.TypeFactory;

// import java.util.List;
// import java.util.stream.Collectors;

// import org.springframework.stereotype.Component;

// import lombok.SneakyThrows;
// import lombok.extern.slf4j.Slf4j;

// /**
//  * Mapper for converting between BoardOfDirectors and BoardOfDirector
//  */
// @Component
// @Slf4j
// public class StockResultFinanceMapper {

//     private static final ObjectMapper objectMapper = new ObjectMapper()
//             .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

//     @SneakyThrows
//     public StockRes parseQuaterlyFinancials(String jsonData) {
//         return objectMapper.readValue(jsonData, 
//             TypeFactory.defaultInstance().constructType(QuaterlyFinancialStatementResponse.class));
//     }

//     private final BaseModelMapper baseModelMapper = new BaseModelMapper();
    
//     /**
//      * Create QuarterlyFinancialMetrics from symbol and QuaterlyFinancialStatementResponse object
//      * 
//      * @param symbol Stock symbol
//      * @param financials QuaterlyFinancialStatementResponse object
//      * @return FinancialResult object
//      */
//     public QuaterlyResult toQuarterlyFinancialMetrics(String symbol, QuaterlyFinancialStatementResponse financials) {
//         if (financials == null) {
//             return null;
//         }
//         BaseModel baseModel = baseModelMapper.getBaseModel(symbol, "TradeB");
//         var QuaterlyResultBuilder = QuaterlyResult.builder()
//             .id(baseModel.getId())
//             .symbol(baseModel.getSymbol())
//             .source(baseModel.getSource())
//             .audit(baseModel.getAudit())
//             .financialResults(toFinancialResults(financials.getStock()));

//         return QuaterlyResultBuilder.build();
//     }

//     private List<FinancialResult> toFinancialResults(StockFinancialData financials) {
//         if (financials == null) {
//             return null;
//         }
//         return financials.getQuarterKeys().stream()
//             .map(quarterKey -> toFinancialResult(financials.getMetrics(quarterKey)))
//             .collect(Collectors.toList());
//     }

//     private FinancialResult toFinancialResult(QuarterlyFinancialMetrics quarterlyFinancialMetrics) {
//         if (quarterlyFinancialMetrics == null) {
//             return null;
//         }
//         var FinancialResultBuilder = FinancialResult.builder()
//             .costMetrics(toCostMetrics(quarterlyFinancialMetrics))
//             .totalRevenue(quarterlyFinancialMetrics.getTotalRevenue())
//             .otherIncome(quarterlyFinancialMetrics.getOtherIncome())
//             .operatingRevenue(quarterlyFinancialMetrics.getOperatingRevenue())
//             .yearEnd(quarterlyFinancialMetrics.getYearEnd())
//             .growthMetrics(toGrowthMetrics(quarterlyFinancialMetrics))
//             .profitMetrics(toProfitMetrics(quarterlyFinancialMetrics))
//             .taxMetrics(toTaxMetrics(quarterlyFinancialMetrics))
//             .epsMetrics(toEpsMetrics(quarterlyFinancialMetrics));

//         return FinancialResultBuilder.build();
//     }

//     private CostMetrics toCostMetrics(QuarterlyFinancialMetrics quarterlyFinancialMetrics) {
//         return CostMetrics.builder()
//             .totalExpenditure(quarterlyFinancialMetrics.getTotalExpenses())
//             .manufacturingCost(quarterlyFinancialMetrics.getOperatingRevenue())
//             .employeeCost(quarterlyFinancialMetrics.getEmployeeCost())
//             .interest(quarterlyFinancialMetrics.getInterest())
//             .operatingExpenses(quarterlyFinancialMetrics.getOperatingExpenses())
//             .depreciationAndAmortization(quarterlyFinancialMetrics.getDepreciationAndAmortization())
//             .build();
//     }

//     private ProfitMetrics toProfitMetrics(QuarterlyFinancialMetrics quarterlyFinancialMetrics) {
//         return ProfitMetrics.builder()
//             .netProfit(quarterlyFinancialMetrics.getTotalExpenses())
//             .operationProfit(quarterlyFinancialMetrics.getOperationProfit())
//             .profitBeforeTax(quarterlyFinancialMetrics.getProfitBeforeTax())
//             .profitAfterTax(quarterlyFinancialMetrics.getProfitAfterTax())
//             .minorityShare(quarterlyFinancialMetrics.getMinorityShare())
//             .build();
//     }

//     private TaxMetrics toTaxMetrics(QuarterlyFinancialMetrics quarterlyFinancialMetrics) {
//         return TaxMetrics.builder()
//             .tax(quarterlyFinancialMetrics.getTax())
//             .taxPer(quarterlyFinancialMetrics.getTaxPer())
//             .build();
//     }

//     private EpsMetrics toEpsMetrics(QuarterlyFinancialMetrics quarterlyFinancialMetrics) {
//         return EpsMetrics.builder()
//             .basicEpsRs(quarterlyFinancialMetrics.getAdjEpsInRsBasic())
//             .dilutedEpsRs(quarterlyFinancialMetrics.getAdjEpsInRsDiluted())
//             .build();
//     }

//     private GrowthMetrics toGrowthMetrics(QuarterlyFinancialMetrics quarterlyFinancialMetrics) {
//         return GrowthMetrics.builder()
//             .revenueGrowthPer(quarterlyFinancialMetrics.getRevenueGrowthPer())
//             .netProfitGrowth(quarterlyFinancialMetrics.getNetProfitGrowth())
//             .netProfitMarginGrowth(quarterlyFinancialMetrics.getNetProfitMarginGrowth())
//             .patMarginGrowth(quarterlyFinancialMetrics.getPatMarginGrowth())
//             .build();
//     }

// }
