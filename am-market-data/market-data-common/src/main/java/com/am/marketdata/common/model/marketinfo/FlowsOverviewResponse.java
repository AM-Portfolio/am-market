package com.am.marketdata.common.model.marketinfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowsOverviewResponse {
    private String interval;
    private InstitutionalFlowResponse fii;
    private InstitutionalFlowResponse dii;
    private List<OiSummary> oi;
}
