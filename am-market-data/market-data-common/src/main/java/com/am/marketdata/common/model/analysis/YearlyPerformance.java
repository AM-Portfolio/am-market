package com.am.marketdata.common.model.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YearlyPerformance {
    private int year;
    private Double yearlyReturn;
    // Month Name (JANUARY) -> Percentage Return
    private Map<String, Double> monthlyReturns;
    // Month Name (JANUARY) -> Day (1-31) -> Percentage Return (Optional)
    private Map<String, Map<Integer, Double>> dailyReturns;
}
