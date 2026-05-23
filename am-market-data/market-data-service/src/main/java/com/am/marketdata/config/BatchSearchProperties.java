package com.am.marketdata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configurable limits for POST /v1/securities/batch-search.
 * Mongo query limit defaults to 0 (no {@code $limit} on the query).
 * Candidate cap applies in-memory after fetch, before match scoring.
 */
@Component
@ConfigurationProperties(prefix = "market-data.batch-search")
public class BatchSearchProperties {

    /** Max queries per HTTP request. */
    private int maxQueries = 1000;

    /** How many queries to process per internal loop (memory batching). */
    private int internalBatchSize = 100;

    /**
     * Mongo {@code Query.limit}; 0 means no limit (full regex result set from DB).
     */
    private int mongoQueryLimit = 0;

    /**
     * Max documents scored per query after Mongo/fallback; 0 means no cap.
     */
    private int maxCandidatesPerQuery = 100;

    private boolean cacheEnabled = true;

    public int getMaxQueries() {
        return maxQueries;
    }

    public void setMaxQueries(int maxQueries) {
        this.maxQueries = maxQueries;
    }

    public int getInternalBatchSize() {
        return internalBatchSize;
    }

    public void setInternalBatchSize(int internalBatchSize) {
        this.internalBatchSize = internalBatchSize;
    }

    public int getMongoQueryLimit() {
        return mongoQueryLimit;
    }

    public void setMongoQueryLimit(int mongoQueryLimit) {
        this.mongoQueryLimit = mongoQueryLimit;
    }

    public int getMaxCandidatesPerQuery() {
        return maxCandidatesPerQuery;
    }

    public void setMaxCandidatesPerQuery(int maxCandidatesPerQuery) {
        this.maxCandidatesPerQuery = maxCandidatesPerQuery;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }
}
