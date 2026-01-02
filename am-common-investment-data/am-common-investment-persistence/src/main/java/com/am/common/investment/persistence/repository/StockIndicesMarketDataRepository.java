package com.am.common.investment.persistence.repository;

import com.am.common.investment.persistence.document.StockIndicesMarketDataDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * MongoDB repository for stock indices market data
 */
@Repository
public interface StockIndicesMarketDataRepository extends MongoRepository<StockIndicesMarketDataDocument, String> {
    
    /**
     * Find stock indices market data documents by index symbols
     * 
     * @param indexSymbols Set of index symbols to search for
     * @return List of matching documents
     */
    List<StockIndicesMarketDataDocument> findByIndexSymbolIn(Set<String> indexSymbols);
    
    /**
     * Find stock indices market data document by index symbol
     * 
     * @param indexSymbol Index symbol to search for
     * @return Matching document or null if not found
     */
    StockIndicesMarketDataDocument findByIndexSymbol(String indexSymbol);
}
