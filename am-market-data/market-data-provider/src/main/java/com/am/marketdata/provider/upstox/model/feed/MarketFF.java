package com.am.marketdata.provider.upstox.model.feed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketFF {
    private LTPC ltpc;
    private MarketOHLC marketOHLC;
    private Double atp;
    private String vtt;
    private Long oi;
    private Double iv;
    private Long tbq;
    private Long tsq;
    // MarketLevel and OptionGreeks can be added if needed, skipping for now to
    // focus on OHLC
}
