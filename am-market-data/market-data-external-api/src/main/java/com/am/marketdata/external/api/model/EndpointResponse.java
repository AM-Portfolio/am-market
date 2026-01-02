package com.am.marketdata.external.api.model;

import lombok.Builder;
import lombok.Data;

/**
 * Model representing a response from an endpoint
 */
@Data
@Builder
public class EndpointResponse {
    private String endpointId;
    private String url;
    private int statusCode;
    private long responseTime;
    private Object data;
    private String errorMessage;
    private boolean success;
}
