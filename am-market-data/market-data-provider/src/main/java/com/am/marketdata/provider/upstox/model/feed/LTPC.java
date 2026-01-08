package com.am.marketdata.provider.upstox.model.feed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LTPC {
    private Double ltp;
    private String ltt;
    private String ltq;
    private Double cp;
}
