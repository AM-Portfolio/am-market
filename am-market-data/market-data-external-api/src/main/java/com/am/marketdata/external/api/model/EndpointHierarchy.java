package com.am.marketdata.external.api.model;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

/**
 * Model representing a hierarchical structure of endpoints
 */
@Data
@Builder
public class EndpointHierarchy {
    private Map<String, Object> endpoints;
    private int totalEndpoints;
}
