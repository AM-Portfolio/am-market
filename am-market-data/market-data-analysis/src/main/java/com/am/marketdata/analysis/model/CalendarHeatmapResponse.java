package com.am.marketdata.analysis.model;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class CalendarHeatmapResponse {
    private String symbol;
    private int year;
    // Month Name (JANUARY) -> Day (1-31) -> Percentage Return
    private Map<String, Map<Integer, Double>> data;
}
