package com.am.marketdata.common.model.tradeB.financials.results;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuaterlyFinancialStatementResponse {
    private String type;
    private StockFinancialData stock;
}
