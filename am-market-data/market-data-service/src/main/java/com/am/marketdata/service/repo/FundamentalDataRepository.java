package com.am.marketdata.service.repo;

import com.am.marketdata.common.model.fundamental.FundamentalData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for persisting and querying {@link FundamentalData}.
 * Relies on the unique index on `isin` to guarantee zero duplication.
 */
@Repository
public interface FundamentalDataRepository extends MongoRepository<FundamentalData, String> {

    /**
     * Find complete fundamental profile by unique ISIN.
     */
    Optional<FundamentalData> findByIsin(String isin);

    /**
     * Find complete fundamental profile by trading symbol.
     */
    Optional<FundamentalData> findBySymbol(String symbol);

    /**
     * Finds documents where a specific section timestamp is older than the provided threshold or missing,
     * used by background schedulers to perform rolling updates.
     *
     * @param sectionName e.g. "keyRatios" or "incomeStatement"
     * @param threshold Instant threshold
     * @return list of stale FundamentalData documents
     */
    @Query("{ $or: [ { 'sectionLastUpdated.?0': { $lt: ?1 } }, { 'sectionLastUpdated.?0': { $exists: false } } ] }")
    List<FundamentalData> findBySectionStale(String sectionName, Instant threshold);
}
