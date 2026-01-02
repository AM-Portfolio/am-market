package com.am.marketdata.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamConnectResponse {
    private String status;
    private String message;
    private MarketDataUpdate data;
}
