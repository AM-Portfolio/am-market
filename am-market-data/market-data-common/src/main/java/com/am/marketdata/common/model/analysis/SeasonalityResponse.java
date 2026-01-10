package com.am.marketdata.common.model.analysis;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeasonalityResponse {
    private String symbol;
    private Map<String, Double> dayOfWeekReturns;
    private Map<String, Double> monthlyReturns;
}
