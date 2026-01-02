package com.am.marketdata.external.api.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * Model representing the health check response for all endpoints
 */
@Data
@Builder
public class EndpointHealthCheckResponse {
    private List<EndpointHealth> healthy;
    private List<EndpointHealth> unhealthy;
    private int totalEndpoints;
    private int successCount;
    private int failureCount;
    private String successRate;
}
