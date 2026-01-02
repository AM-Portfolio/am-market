package com.am.common.investment.model.equity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HistoricalComparison {
    private Double value;
    private Double perChange365d;
    private LocalDateTime date365dAgo;
    private Double perChange30d;
    private LocalDateTime date30dAgo;
    private Double previousDay;
    private Double oneWeekAgo;
    private Double oneMonthAgo;
    private Double oneYearAgo;
}
