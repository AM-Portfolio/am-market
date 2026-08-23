package com.am.common.investment.persistence.document.global;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for global market index configurations.
 *
 * <p>Collection: {@code global_index_config}
 *
 * <p>This repository is the single source of truth for:
 * <ul>
 *   <li>Determining whether a given symbol is a global index (for dynamic routing
 *       in {@code MarketIndexController}).</li>
 *   <li>Providing instrument keys for Upstox WebSocket subscriptions.</li>
 *   <li>Tracking admin sync state to prevent double-writes to InfluxDB.</li>
 * </ul>
 *
 * <p><b>Important:</b> This repository is intentionally separate from all Indian
 * index configuration mechanisms. Do NOT use this to query Indian index data.
 */
@Repository
public interface GlobalIndexConfigRepository extends MongoRepository<GlobalIndexConfigDocument, String> {

    /**
     * Finds a global index config by its human-readable symbol.
     *
     * <p>Used by {@code MarketIndexController} to validate whether a requested symbol
     * belongs to the global index universe before routing to {@code GlobalIndexService}.
     *
     * @param symbol the index symbol (e.g., "DJI", "SPX")
     * @return the config document, or empty if the symbol is not a global index
     */
    Optional<GlobalIndexConfigDocument> findBySymbol(String symbol);

    /**
     * Returns all configured global index instrument keys.
     *
     * <p>Used by {@code GlobalMarketScheduleService} to build the subscription
     * set when the WebSocket connects and the global vote is active.
     *
     * @return list of all global index config documents
     */
    List<GlobalIndexConfigDocument> findAll();

    /**
     * Checks if the collection has any documents.
     *
     * <p>Used by {@code GlobalIndexSeeder} on startup to determine if seeding is needed.
     *
     * @return true if the collection is empty (needs seeding)
     */
    default boolean isCollectionEmpty() {
        return count() == 0;
    }
}
