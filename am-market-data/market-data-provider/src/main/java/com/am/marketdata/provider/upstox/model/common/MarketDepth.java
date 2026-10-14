package com.am.marketdata.provider.upstox.model.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketDepth {
    @JsonProperty("buy")
    private List<DepthEntry> buyOrders;
    @JsonProperty("sell")
    private List<DepthEntry> sellOrders;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DepthEntry {
        private Double price;
        private Long quantity;
        private Integer orders;
    }
}
