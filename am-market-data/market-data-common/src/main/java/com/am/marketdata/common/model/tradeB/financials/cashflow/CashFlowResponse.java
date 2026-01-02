package com.am.marketdata.common.model.tradeB.financials.cashflow;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Response model for cash flow statement data
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CashFlowResponse {
    private String type;
    private CashFlowData stock;
}
