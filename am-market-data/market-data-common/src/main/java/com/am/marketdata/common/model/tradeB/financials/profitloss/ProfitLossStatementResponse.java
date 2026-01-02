package com.am.marketdata.common.model.tradeB.financials.profitloss;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Response model for profit and loss statement data
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfitLossStatementResponse {
    private String type;
    private ProfitLossData stock;
}
