package com.am.marketdata.watchlist.migration;

import com.am.marketdata.watchlist.entity.WatchlistItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/**
 * Zero-downtime MongoDB Migration Runner.
 * Executes on application startup before new compound indexes are evaluated.
 * Backfills missing 'exchange' fields on legacy watchlist items with default value "NSE"
 * to guarantee that index creation '{watchlistId, symbol, exchange}' never crashes.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WatchlistMigrationRunner implements ApplicationRunner {

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            // Find all watchlist documents where 'exchange' is null or does not exist
            Query query = Query.query(Criteria.where("exchange").exists(false));
            Update update = Update.update("exchange", "NSE");

            long modifiedCount = mongoTemplate.updateMulti(query, update, WatchlistItem.class).getModifiedCount();
            if (modifiedCount > 0) {
                log.info("WatchlistMigrationRunner: Successfully backfilled 'exchange: NSE' on {} legacy items", modifiedCount);
            } else {
                log.debug("WatchlistMigrationRunner: All watchlist items already have exchange defined.");
            }
        } catch (Exception e) {
            log.warn("WatchlistMigrationRunner: Non-fatal migration notice: {}", e.getMessage());
        }
    }
}
