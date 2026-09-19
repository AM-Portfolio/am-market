package com.am.marketdata.common.model.marketinfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OiResponse {
    private String status;
    private OiData data;
}
