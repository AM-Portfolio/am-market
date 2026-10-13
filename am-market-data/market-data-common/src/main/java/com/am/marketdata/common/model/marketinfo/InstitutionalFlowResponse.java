package com.am.marketdata.common.model.marketinfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstitutionalFlowResponse {
    private String status;
    private Map<String, List<InstitutionalFlowRecord>> data;
}
