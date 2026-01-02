package com.am.common.investment.service;

import com.am.common.investment.model.stockindice.StockIndicesMarketData;

import java.util.List;
import java.util.Set;

/**
 * Service interface for stock indices market data operations
 */
public interface StockIndicesMarketDataService {

    /**
     * Save stock indices market data
     * 
     * @param marketData The market data to save
     * @return The saved market data
     */
    StockIndicesMarketData save(StockIndicesMarketData marketData);

    /**
     * Find stock indices market data by multiple index symbols
     * 
     * @param indexSymbols Set of index symbols to search for
     * @return List of matching market data
     */
    List<StockIndicesMarketData> findByIndexSymbols(Set<String> indexSymbols);
    
    /**
     * Find all stock indices market data containing a specific stock symbol
     * 
     * @param symbol The stock symbol to search for
     * @return List of matching market data
     */
    StockIndicesMarketData findByIndexSymbol(String symbol);
}
