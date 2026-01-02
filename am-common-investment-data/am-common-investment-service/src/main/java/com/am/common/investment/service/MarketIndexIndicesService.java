package com.am.common.investment.service;

import com.am.common.investment.model.equity.MarketIndexIndices;

import java.util.List;

public interface MarketIndexIndicesService {
    // Basic operations
    void save(MarketIndexIndices indices);
    List<MarketIndexIndices> getByKey(String key);
}
