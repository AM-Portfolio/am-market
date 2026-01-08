package com.am.marketdata.provider.upstox.model.feed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FullFeed {
    private MarketFF marketFF;
    private String requestMode;
}
