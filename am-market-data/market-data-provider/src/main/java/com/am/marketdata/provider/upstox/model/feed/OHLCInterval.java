package com.am.marketdata.provider.upstox.model.feed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OHLCInterval {
    private String interval;
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private String vol;
    private String ts;
}
