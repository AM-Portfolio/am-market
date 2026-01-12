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
public class MonthlyIndicesPerformance {
    private int year;
    private int month; // 1-12
    private String monthName; // "JANUARY"
    private IndexPerformance topPerformer;
    private IndexPerformance worstPerformer;
    private List<IndexPerformance> allIndices;
}
