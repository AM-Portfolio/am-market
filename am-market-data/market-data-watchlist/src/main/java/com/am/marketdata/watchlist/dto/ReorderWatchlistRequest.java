package com.am.marketdata.watchlist.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReorderWatchlistRequest {
    private List<String> symbols;
}
