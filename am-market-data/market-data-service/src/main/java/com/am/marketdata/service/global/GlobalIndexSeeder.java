package com.am.marketdata.service.global;

import com.am.common.investment.persistence.document.global.GlobalIndexConfigDocument;
import com.am.common.investment.persistence.document.global.GlobalIndexConfigRepository;
import com.am.common.investment.persistence.document.global.GlobalIndexSyncStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Startup component that seeds the {@code global_index_config} MongoDB collection
 * with the default global market index configurations on first boot.
 *
 * <p><b>When does seeding happen?</b>
 * Seeding runs ONLY if the {@code global_index_config} collection is completely empty
 * (i.e., {@code count() == 0}). If the collection already has documents
 * (from a previous deployment or manual admin sync), seeding is SKIPPED entirely.
 * This makes the seeder fully idempotent — safe to run on every server restart.
 *
 * <p><b>What is seeded?</b>
 * The default 10 global indices: Dow Jones, S&P 500, Nasdaq, FTSE 100, DAX, CAC 40,
 * Nikkei 225, Hang Seng, Shanghai Composite, and GIFT NIFTY.
 * All seeded documents have {@code syncStatus = PENDING}, which means no historical
 * data has been backfilled to InfluxDB yet. Historical data is populated separately
 * by calling {@code POST /v1/market-data/admin/global/sync}.
 *
 * <p><b>Instrument Key Format:</b>
 * The {@code instrumentKey} field must match the exact format used in the Upstox
 * Global Instruments JSON. If Upstox changes these keys, the admin needs to manually
 * update the collection (or update this seeder and redeploy).
 *
 * <p>{@code global-market-schedule.yml} is the single source for the provider
 * instrument, session and freshness configuration. Existing populated collections
 * are changed only by the separately gated {@link GlobalIndexConfigReconciler}.
 */
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalIndexSeeder implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(GlobalIndexSeeder.class);

    private final GlobalIndexConfigRepository globalIndexConfigRepository;
    private final GlobalMarketScheduleService globalMarketScheduleService;

    @Override
    public void run(ApplicationArguments args) {
        if (!globalIndexConfigRepository.isCollectionEmpty()) {
            logger.info("[GlobalIndexSeeder] global_index_config is not empty ({} documents found). Skipping seed.",
                    globalIndexConfigRepository.count());
            return;
        }

        logger.info("[GlobalIndexSeeder] global_index_config is empty. Seeding default global index configurations...");

        List<GlobalIndexConfigDocument> defaults = buildDefaultConfigs();
        globalIndexConfigRepository.saveAll(defaults);

        logger.info("[GlobalIndexSeeder] Successfully seeded {} global index configurations.", defaults.size());
    }

    /**
     * Builds the default list of global index config documents to seed.
     *
     * <p><b>NOTE TO REVIEWERS:</b>
     * If Upstox changes the instrument key format for any of these global indices,
     * update the corresponding {@code instrumentKey} here and bump the document
     * version in MongoDB (or drop and re-seed the collection).
     *
     * @return list of default {@link GlobalIndexConfigDocument} objects
     */
    private List<GlobalIndexConfigDocument> buildDefaultConfigs() {
        return globalMarketScheduleService.getMarkets().stream()
                .map(market -> GlobalIndexConfigDocument.builder()
                        .symbol(market.symbol())
                        .name(market.name())
                        .instrumentKey(market.hasInstrument() ? market.instrumentKey() : null)
                        .exchange(market.exchange())
                        .timezone(market.timezone())
                        .marketOpenTime(market.marketOpen())
                        .marketCloseTime(market.marketClose())
                        .influxAggregateOffset(market.influxAggregateOffset())
                        .syncStatus(GlobalIndexSyncStatus.PENDING)
                        .build())
                .toList();
    }
}
