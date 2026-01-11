package com.am.marketdata.common.model.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalPerformanceResponse {
    private String symbol;
    private int startYear;
    private int endYear;
    private Double overallReturn;
    private List<YearlyPerformance> yearlyPerformance;
}
