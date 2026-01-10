package com.am.marketdata.analysis.model;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class SeasonalityResponse {
    private String symbol;
    private Map<String, Double> dayOfWeekReturns;
    private Map<String, Double> monthlyReturns;
}
