package com.am.marketdata.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMoverDTO {
    private String symbol;
    private Double lastPrice;
    private Double change;
    private Double pChange;
    private Double percentChange;
    private Double previousClose;
}
