package com.am.marketdata.upstock.model.common;

import lombok.Data;
import java.util.List;

@Data
public class MarketDepth {
    private List<DepthEntry> buyOrders;
    private List<DepthEntry> sellOrders;
    
    @Data
    public static class DepthEntry {
        private Double price;
        private Long quantity;
        private Integer orders;
    }
} 