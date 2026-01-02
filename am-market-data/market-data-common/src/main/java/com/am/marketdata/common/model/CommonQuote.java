package com.am.marketdata.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonQuote {
    private String symbol;
    private Double lastTradedPrice;
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Double volume;
    private Double change;
    private Double changePercent;
    private LocalDateTime timestamp;
    private String providerName;
    private String instrumentKey;
}
