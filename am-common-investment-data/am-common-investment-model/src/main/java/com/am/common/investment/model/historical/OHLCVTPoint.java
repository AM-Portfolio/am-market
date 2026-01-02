package com.am.common.investment.model.historical;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Model class representing a single OHLCVT (Open, High, Low, Close, Volume, Time) data point
 * for financial time series data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OHLCVTPoint {
    private LocalDateTime time;
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Long volume;
}
