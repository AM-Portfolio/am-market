package com.am.marketdata.provider.upstox.model;

import lombok.Data;

@Data
public class DepthData {
    private MarketDepthEntry[] buy;
    private MarketDepthEntry[] sell;
}