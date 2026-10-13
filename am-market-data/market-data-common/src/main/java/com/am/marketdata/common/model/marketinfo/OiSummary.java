package com.am.marketdata.common.model.marketinfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OiSummary {
    private String symbol;
    private String instrumentKey;
    private Long totalCalls;
    private Long totalPuts;
    private Double spot;
    private String expiry;
    private Double pcr;
}
