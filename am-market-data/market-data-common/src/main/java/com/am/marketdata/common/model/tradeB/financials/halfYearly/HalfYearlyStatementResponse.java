package com.am.marketdata.common.model.tradeB.financials.halfYearly;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HalfYearlyStatementResponse {
    private String type;
    private StockHalfYearlyData stock;
}
