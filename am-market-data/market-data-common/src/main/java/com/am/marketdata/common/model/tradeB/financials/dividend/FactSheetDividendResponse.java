package com.am.marketdata.common.model.tradeB.financials.dividend;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Response model for cash flow statement data
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FactSheetDividendResponse {
    private String type;
    private DividendData stock;
}
