package com.am.marketdata.provider.upstox.repo;

import com.am.marketdata.common.model.UpstoxInstrument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UpstoxInstrumentRepository extends MongoRepository<UpstoxInstrument, String> {

    // For "gym balls" - exact match on list of symbols
    List<UpstoxInstrument> findByAssetSymbolIn(List<String> assetSymbols);

    // Also support finding by trading symbol list
    List<UpstoxInstrument> findByTradingSymbolIn(List<String> tradingSymbols);

    // "Semantic search" / text search implementation using Regex
    // Search in name, assetSymbol, or tradingSymbol
    @Query("{ '$or': [ { 'name': { '$regex': ?0, '$options': 'i' } }, { 'assetSymbol': { '$regex': ?0, '$options': 'i' } }, { 'tradingSymbol': { '$regex': ?0, '$options': 'i' } } ] }")
    List<UpstoxInstrument> searchByText(String searchText);

    // Find options/derivatives for a given underlying symbol
    List<UpstoxInstrument> findByAssetSymbolAndInstrumentType(String assetSymbol, String instrumentType);

}
