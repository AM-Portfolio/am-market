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
public class CalendarHeatmapResponse {
    private String symbol;
    private int year;
    // Month Name (JANUARY) -> Day (1-31) -> Percentage Return
    private Map<String, Map<Integer, Double>> data;
}
