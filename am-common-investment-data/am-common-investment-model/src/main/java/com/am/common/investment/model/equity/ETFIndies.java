package com.am.common.investment.model.equity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ETFIndies {
    private String assets;
    private String symbol;
    private MetaData metaData;
    private MarketData marketData;
    private MarketBreadth marketBreadth;
    private LocalDateTime timestamp;
}
