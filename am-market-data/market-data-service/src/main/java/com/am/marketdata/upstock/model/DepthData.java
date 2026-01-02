package com.am.marketdata.upstock.model;

import lombok.Data;

@Data
public class DepthData {
    private MarketDepthEntry[] buy;
    private MarketDepthEntry[] sell;
}