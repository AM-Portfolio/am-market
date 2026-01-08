package com.am.marketdata.provider.upstox.model.feed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketOHLC {
    private List<OHLCInterval> ohlc;
}
