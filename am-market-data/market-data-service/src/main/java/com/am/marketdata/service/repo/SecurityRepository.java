package com.am.marketdata.service.repo;

import com.am.marketdata.service.model.security.SecurityDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SecurityRepository extends MongoRepository<SecurityDocument, String> {

    @Query("{ 'key.symbol' : { $in: ?0 } }")
    List<SecurityDocument> findBySymbolIn(List<String> symbols);

    // Find by ISIN
    @Query("{ 'key.isin' : ?0 }")
    SecurityDocument findByIsin(String isin);

    // Enhanced fuzzy search by symbol, ISIN, or company name (case-insensitive)
    @Query("{ $or: [ { 'key.symbol': { $regex: ?0, $options: 'i' } }, { 'key.isin': { $regex: ?0, $options: 'i' } }, { 'metadata.company_name': { $regex: ?0, $options: 'i' } } ] }")
    List<SecurityDocument> search(String text);

    // Bulk update matching methods - return List to handle potential duplicates

    @Query("{ 'key.symbol' : ?0 }")
    List<SecurityDocument> findByKeySymbol(String symbol);

    @Query("{ 'key.isin' : ?0 }")
    List<SecurityDocument> findByKeyIsin(String isin);

    @Query("{ 'key.symbol' : ?0, 'key.isin' : ?1 }")
    List<SecurityDocument> findByKeySymbolAndKeyIsin(String symbol, String isin);

    // Loose matching (case-insensitive)
    @Query("{ 'key.symbol' : { $regex: ?0, $options: 'i' } }")
    List<SecurityDocument> findByKeySymbolIgnoreCase(String symbol);

    @Query("{ 'key.isin' : { $regex: ?0, $options: 'i' } }")
    List<SecurityDocument> findByKeyIsinIgnoreCase(String isin);
}
