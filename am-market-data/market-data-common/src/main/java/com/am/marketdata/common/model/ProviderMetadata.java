package com.am.marketdata.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderMetadata {
    private String providerName;
    private String instrumentKey;
    private String exchange;
    private String segment;
    private String originalToken;

    @Builder.Default
    private Map<String, Object> additionalData = new HashMap<>();
}
