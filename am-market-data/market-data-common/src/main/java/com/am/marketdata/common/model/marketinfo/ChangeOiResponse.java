package com.am.marketdata.common.model.marketinfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeOiResponse {
    private String status;
    private ChangeOiData data;
}
