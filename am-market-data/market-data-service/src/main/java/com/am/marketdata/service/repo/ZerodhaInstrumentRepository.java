package com.am.marketdata.service.repo;

import com.am.marketdata.service.model.ZerodhaInstrument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ZerodhaInstrumentRepository extends MongoRepository<ZerodhaInstrument, String> {

    // For "gym balls" - exact match on list of symbols (using 'name' as asset
    // symbol equivalent in Zerodha)
    List<ZerodhaInstrument> findByNameIn(List<String> names);

    // Also support finding by trading symbol list
    List<ZerodhaInstrument> findByTradingSymbolIn(List<String> tradingSymbols);

    // "Semantic search" / text search implementation using Regex
    // Search in name or tradingSymbol
    @Query("{ '$or': [ { 'name': { '$regex': ?0, '$options': 'i' } }, { 'tradingSymbol': { '$regex': ?0, '$options': 'i' } } ] }")
    List<ZerodhaInstrument> searchByText(String searchText);

    // Find options/derivatives for a given underlying symbol
    List<ZerodhaInstrument> findByNameAndInstrumentType(String name, String instrumentType);

}
