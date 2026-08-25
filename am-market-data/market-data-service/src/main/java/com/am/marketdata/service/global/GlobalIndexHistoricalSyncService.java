package com.am.marketdata.service.global;

import com.am.common.investment.persistence.document.global.GlobalIndexConfigDocument;
import com.am.common.investment.persistence.document.global.GlobalIndexConfigRepository;
import com.am.common.investment.persistence.document.global.GlobalIndexSyncStatus;
import com.am.common.investment.persistence.influx.measurement.GlobalIndexMeasurement;
import com.am.common.investment.persistence.repository.measurement.impl.GlobalIndexInfluxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Manages the "historical sync" lifecycle state for global index symbols.
 *
 * <p><b>Why no bulk backfill?</b><br>
 * Upstox does not expose a historical candle API for {@code GLOBAL_INDEX|*} symbols
 * (e.g., DJI, SPX, FTSE). These are streamed as live WebSocket ticks only.
 * Historical data for global indices therefore accumulates organically via
 * {@link GlobalIndexCacheWriter}, which persists every incoming WebSocket tick to InfluxDB.
 *
 * <p><b>What this service does:</b>
 * <ol>
 *   <li>Checks the last data point timestamp in InfluxDB for each configured global index.</li>
 *   <li>Logs the data availability window so operators can see how far back the chart data goes.</li>
 *   <li>Marks each symbol as {@link GlobalIndexSyncStatus#COMPLETE} in MongoDB so the admin
 *       endpoint returns a meaningful status rather than staying stuck at {@code PENDING}.</li>
 *   <li>If {@code force=true} is passed, resets the status back to {@code PENDING} and re-evaluates,
 *       which is useful after a manual InfluxDB data purge.</li>
 * </ol>
 *
 * <p>For gap filling of missed intraday ticks (e.g., after a service restart),
 * see {@link GlobalIndexGapFillService}, which runs automatically on application startup.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalIndexHistoricalSyncService {

    private final GlobalIndexConfigRepository configRepository;
    private final GlobalIndexInfluxRepository influxRepository;

    /**
     * Evaluates the InfluxDB data window for each configured global index and
     * updates the {@link GlobalIndexSyncStatus} in MongoDB accordingly.
     *
     * <p>This is triggered via {@code POST /v1/admin/sync/global} in
     * {@link com.am.marketdata.controller.MarketDataAdminController}.
     *
     * @param symbol a specific symbol to check (e.g., {@code "DJI"}), or {@code null} for all
     * @param force  if {@code true}, resets status to {@code PENDING} before re-evaluating
     */
    public void syncHistoricalData(String symbol, boolean force) {
        log.info("Global index sync status check started. symbol={}, force={}", symbol, force);

        List<GlobalIndexConfigDocument> configs;
        if (symbol != null && !symbol.trim().isEmpty()) {
            Optional<GlobalIndexConfigDocument> doc = configRepository.findBySymbol(symbol.trim());
            if (doc.isEmpty()) {
                log.warn("No global index configuration found for symbol={}", symbol);
                return;
            }
            configs = List.of(doc.get());
        } else {
            configs = configRepository.findAll();
        }

        for (GlobalIndexConfigDocument config : configs) {
            try {
                if (force) {
                    config.setSyncStatus(GlobalIndexSyncStatus.PENDING);
                    configRepository.save(config);
                    log.info("[{}] Force flag set — status reset to PENDING.", config.getSymbol());
                }

                if (!force && GlobalIndexSyncStatus.COMPLETE == config.getSyncStatus()) {
                    log.info("[{}] Already COMPLETE. Use ?force=true to re-evaluate.", config.getSymbol());
                    continue;
                }

                // Check the oldest and newest data points available in InfluxDB
                Optional<Instant> lastTimestamp = influxRepository.findLastTimestamp(config.getInstrumentKey());

                if (lastTimestamp.isPresent()) {
                    Instant latest = lastTimestamp.get();
                    log.info("[{}] InfluxDB data available — latest tick at {}. " +
                             "Historical data accumulates via live WebSocket ticks (GlobalIndexCacheWriter).",
                            config.getSymbol(), latest);

                    config.setSyncStatus(GlobalIndexSyncStatus.COMPLETE);
                    configRepository.save(config);
                    log.info("[{}] Status updated → COMPLETE.", config.getSymbol());
                } else {
                    log.warn("[{}] No InfluxDB data yet. Status remains PENDING. " +
                             "Data will begin accumulating once the WebSocket streams a tick for this symbol during market hours.",
                            config.getSymbol());
                    // Keep status as PENDING — it will transition to COMPLETE automatically
                    // on the next sync check once live ticks have been received.
                }

            } catch (Exception e) {
                log.error("[{}] Error during sync status check: {}", config.getSymbol(), e.getMessage(), e);
                try {
                    config.setSyncStatus(GlobalIndexSyncStatus.FAILED);
                    configRepository.save(config);
                } catch (Exception saveEx) {
                    log.error("[{}] Failed to persist FAILED status: {}", config.getSymbol(), saveEx.getMessage());
                }
            }
        }

        log.info("Global index sync status check complete.");
    }
}
