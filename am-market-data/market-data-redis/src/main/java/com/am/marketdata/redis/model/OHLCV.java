package com.am.marketdata.redis.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OHLCV {
    private LocalDateTime time;
    private Double lastPrice;
    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;
}
