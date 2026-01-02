package com.am.common.investment.model.equity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MarketData {
    private Double last;
    private Double variation;
    private Double percentChange;
    private Double open;
    private Double high;
    private Double low;
    private Double previousClose;
    private Double yearHigh;
    private Double yearLow;
    private Double indicativeClose;
}
