package com.am.marketdata.external.api.model;

import lombok.Builder;
import lombok.Data;

/**
 * Model representing the health status of an individual endpoint
 */
@Data
@Builder
public class EndpointHealth {
    private String endpointId;
    private String path;
    private int statusCode;
    private String errorMessage;
    private long durationMs;
}
