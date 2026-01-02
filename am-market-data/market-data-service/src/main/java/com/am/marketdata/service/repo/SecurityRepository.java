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

    // Search by symbol or isin containing text (for advanced search later)
    @Query("{ $or: [ { 'key.symbol': { $regex: ?0, $options: 'i' } }, { 'key.isin': { $regex: ?0, $options: 'i' } } ] }")
    List<SecurityDocument> search(String text);
}
