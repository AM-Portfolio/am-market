package com.am.marketdata.common.model.tradeB.financials.balancesheet;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Response model for balance sheet data
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BalanceSheetResponse {
    private String type;
    private BalanceSheetData stock;
}
