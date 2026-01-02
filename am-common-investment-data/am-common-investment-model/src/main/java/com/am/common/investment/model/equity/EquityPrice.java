package com.am.common.investment.model.equity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

import com.am.common.investment.model.historical.OHLCVTPoint;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EquityPrice {
    private String symbol;
    private String isin;
    private Instant time;
    private Double lastPrice;
    private OHLCVTPoint ohlcv;
    private String exchange;
    private String currency;
}
