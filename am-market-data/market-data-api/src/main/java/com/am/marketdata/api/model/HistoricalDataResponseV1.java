package com.am.marketdata.api.model;

import com.am.common.investment.model.historical.HistoricalData;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalDataResponseV1 {
    private Map<String, HistoricalData> data;
    private HistoricalDataMetadata metadata;
    private String error;
    private String message;
}
