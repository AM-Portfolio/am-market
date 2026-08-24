package com.am.marketdata.service.global;

import com.am.common.investment.persistence.document.global.GlobalIndexConfigDocument;
import com.am.common.investment.persistence.document.global.GlobalIndexConfigRepository;
import com.am.common.investment.persistence.document.global.GlobalIndexSyncStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
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
 * <p><b>Configuration Source:</b>
 * The schedule data (timezone, market hours) here is duplicated from
 * {@code global-market-schedule.yml}. The YAML is the primary source of truth
 * for the scheduling service, while MongoDB is the source of truth for routing
 * and backfill tracking. Keeping them in sync is a deployment responsibility.
 */
@Component
@RequiredArgsConstructor
public class GlobalIndexSeeder implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(GlobalIndexSeeder.class);

    private final GlobalIndexConfigRepository globalIndexConfigRepository;

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
        return List.of(
                // -----------------------------------------------------------------
                // US MARKETS — NYSE/NASDAQ (UTC-5 EST, UTC-4 EDT)
                // WebSocket trades hours: 9:30 PM - 4:00 AM IST (Mon-Fri)
                // -----------------------------------------------------------------

                GlobalIndexConfigDocument.builder()
                        .symbol("DJI")
                        .name("Dow Jones Industrial Average")
                        .instrumentKey("GLOBAL_INDEX|DJI")
                        .exchange("NYSE")
                        .timezone("America/New_York")
                        .marketOpenTime("09:30")
                        .marketCloseTime("16:00")
                        .influxAggregateOffset("-14h30m") // NYSE opens at 09:30 EST = 00:30 UTC → offset from UTC midnight
                        .syncStatus(GlobalIndexSyncStatus.PENDING)
                        .build(),

                GlobalIndexConfigDocument.builder()
                        .symbol("SPX")
                        .name("S&P 500")
                        .instrumentKey("GLOBAL_INDEX|SPX")
                        .exchange("NYSE")
                        .timezone("America/New_York")
                        .marketOpenTime("09:30")
                        .marketCloseTime("16:00")
                        .influxAggregateOffset("-14h30m")
                        .syncStatus(GlobalIndexSyncStatus.PENDING)
                        .build(),

                GlobalIndexConfigDocument.builder()
                        .symbol("IXIC")
                        .name("Nasdaq Composite")
                        .instrumentKey("GLOBAL_INDEX|IXIC")
                        .exchange("NASDAQ")
                        .timezone("America/New_York")
                        .marketOpenTime("09:30")
                        .marketCloseTime("16:00")
                        .influxAggregateOffset("-14h30m")
                        .syncStatus(GlobalIndexSyncStatus.PENDING)
                        .build(),

                // -----------------------------------------------------------------
                // EUROPEAN MARKETS
                // -----------------------------------------------------------------

                GlobalIndexConfigDocument.builder()
                        .symbol("FTSE")
                        .name("FTSE 100")
                        .instrumentKey("GLOBAL_INDEX|FTSE")
                        .exchange("LSE")
                        .timezone("Europe/London")      // GMT in winter, BST (UTC+1) in summer — DST handled by ZonedDateTime
                        .marketOpenTime("08:00")
                        .marketCloseTime("16:30")
                        .influxAggregateOffset("-8h")   // LSE opens 08:00 GMT = 08:00 UTC → -8h offset from UTC midnight
                        .syncStatus(GlobalIndexSyncStatus.PENDING)
                        .build(),

                GlobalIndexConfigDocument.builder()
                        .symbol("GDAXI")
                        .name("DAX")
                        .instrumentKey("GLOBAL_INDEX|GDAXI")
                        .exchange("XETRA")
                        .timezone("Europe/Berlin")      // CET (UTC+1) / CEST (UTC+2) in summer
                        .marketOpenTime("09:00")
                        .marketCloseTime("17:30")
                        .influxAggregateOffset("-8h")
                        .syncStatus(GlobalIndexSyncStatus.PENDING)
                        .build(),

                GlobalIndexConfigDocument.builder()
                        .symbol("FCHI")
                        .name("CAC 40")
                        .instrumentKey("GLOBAL_INDEX|FCHI")
                        .exchange("Euronext Paris")
                        .timezone("Europe/Paris")
                        .marketOpenTime("09:00")
                        .marketCloseTime("17:30")
                        .influxAggregateOffset("-8h")
                        .syncStatus(GlobalIndexSyncStatus.PENDING)
                        .build(),

                // -----------------------------------------------------------------
                // ASIAN MARKETS
                // -----------------------------------------------------------------

                GlobalIndexConfigDocument.builder()
                        .symbol("N225")
                        .name("Nikkei 225")
                        .instrumentKey("GLOBAL_INDEX|N225")
                        .exchange("TSE")
                        .timezone("Asia/Tokyo")         // JST (UTC+9) — Japan does NOT observe DST
                        .marketOpenTime("09:00")
                        .marketCloseTime("15:30")
                        .influxAggregateOffset("30m")   // TSE opens 09:00 JST = 00:30 UTC → +30m offset
                        .syncStatus(GlobalIndexSyncStatus.PENDING)
                        .build(),

                GlobalIndexConfigDocument.builder()
                        .symbol("HSI")
                        .name("Hang Seng")
                        .instrumentKey("GLOBAL_INDEX|HSI")
                        .exchange("HKEX")
                        .timezone("Asia/Hong_Kong")     // HKT (UTC+8) — no DST
                        .marketOpenTime("09:30")
                        .marketCloseTime("16:00")
                        .influxAggregateOffset("1h30m")
                        .syncStatus(GlobalIndexSyncStatus.PENDING)
                        .build(),

                GlobalIndexConfigDocument.builder()
                        .symbol("SSEC")
                        .name("Shanghai Composite")
                        .instrumentKey("GLOBAL_INDEX|SSEC")
                        .exchange("SSE")
                        .timezone("Asia/Shanghai")      // CST (UTC+8) — no DST
                        .marketOpenTime("09:30")
                        .marketCloseTime("15:00")
                        .influxAggregateOffset("1h30m")
                        .syncStatus(GlobalIndexSyncStatus.PENDING)
                        .build(),

                // -----------------------------------------------------------------
                // INDIA IFSC — GIFT NIFTY (extended hours, same timezone as NSE)
                // -----------------------------------------------------------------

                GlobalIndexConfigDocument.builder()
                        .symbol("GIFTNIFTY")
                        .name("GIFT NIFTY")
                        .instrumentKey("GLOBAL_INDEX|GIFTNIFTY")
                        .exchange("NSE_IFSC")
                        .timezone("Asia/Kolkata")       // IST (UTC+5:30) — no DST
                        .marketOpenTime("06:00")
                        .marketCloseTime("23:30")       // Extended hours: 6AM to 11:30PM IST
                        .influxAggregateOffset("+5h30m")
                        .syncStatus(GlobalIndexSyncStatus.PENDING)
                        .build()
        );
    }
}
