package com.am.marketdata.upstock.model;

import lombok.Data;

@Data
public class MarketDepthEntry {
    private Integer quantity;
    private Double price;
    private Integer orders;
}