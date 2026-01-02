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
public class CommonInstrument {
    private String instrumentKey;
    private String tradingSymbol;
    private String name;
    private String exchange;
    private String segment;
    private String instrumentType;
    private Integer lotSize;
    private Double tickSize;
    private String isin;
    private String providerName;
    private String providerInstrumentKey;

    @Builder.Default
    private Map<String, ProviderMetadata> providerMetadata = new HashMap<>();
}
