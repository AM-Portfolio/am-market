package com.am.marketdata.service.global;

import com.am.common.investment.persistence.document.global.GlobalIndexConfigDocument;
import com.am.common.investment.persistence.document.global.GlobalIndexConfigRepository;
import com.am.common.investment.persistence.document.global.GlobalIndexSyncStatus;
import com.am.common.investment.persistence.influx.measurement.GlobalIndexMeasurement;
import com.am.common.investment.persistence.repository.measurement.impl.GlobalIndexInfluxRepository;
import com.am.common.investment.model.historical.HistoricalData;
import com.am.common.investment.model.historical.OHLCVTPoint;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.provider.common.MarketDataProviderFactory;
import com.marketdata.common.MarketDataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Manages the "historical sync" lifecycle state for global index symbols.
 *
 * <p>Historical daily candles for exact Upstox global-index keys are backfilled
 * through the documented V3 API. Live WebSocket ticks continue to provide intraday data.
 *
 * <p><b>What this service does:</b>
 * <ol>
 *   <li>Backfills daily candles for PENDING symbols, or refreshes them when {@code force=true}.</li>
 *   <li>Uses only the exact key configured for a symbol; blank or mismatched keys fail closed.</li>
 *   <li>Marks a symbol COMPLETE only after valid candles were persisted successfully.</li>
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
    private final MarketDataProviderFactory providerFactory;
    private final GlobalMarketScheduleService scheduleService;

    /** Upstox V3 daily candles support at most one decade of history. */
    @Value("${market.global.history-backfill-years:10}")
    private int historyBackfillYears;

    /**
     * Fetches and persists daily history for each eligible configured global index,
     * then updates its sync status. This is still the existing admin-only async action.
     *
     * <p>This is triggered via {@code POST /v1/admin/sync/global} in
     * {@link com.am.marketdata.controller.MarketDataAdminController}.
     *
     * @param symbol a specific symbol to check (e.g., {@code "DJI"}), or {@code null} for all
     * @param force  if {@code true}, re-fetches the configured history window even when COMPLETE
     */
    public void syncHistoricalData(String symbol, boolean force) {
        log.info("Global index historical sync started. symbol={}, force={}", symbol, force);

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

        if (historyBackfillYears < 1 || historyBackfillYears > 10) {
            log.error("Global history backfill disabled: configured years={} is outside supported range 1..10.",
                    historyBackfillYears);
            return;
        }

        MarketDataProvider provider;
        try {
            provider = providerFactory.getProvider("upstox");
        } catch (Exception e) {
            log.error("Unable to initialize Upstox provider for global history backfill: {}", e.getMessage());
            return;
        }
        if (provider == null) {
            log.error("Unable to initialize Upstox provider for global history backfill: provider unavailable.");
            return;
        }

        for (GlobalIndexConfigDocument config : configs) {
            try {
                if (config.getInstrumentKey() == null || config.getInstrumentKey().isBlank()) {
                    log.warn("[{}] Global history skipped: no exact provider instrument is configured.", config.getSymbol());
                    continue;
                }

                Optional<GlobalMarketScheduleService.MarketEntry> market = scheduleService.getMarkets().stream()
                        .filter(entry -> config.getSymbol().equals(entry.symbol()))
                        .findFirst();
                if (market.isEmpty() || !config.getInstrumentKey().equals(market.get().instrumentKey())) {
                    log.warn("[{}] Global history skipped: Mongo provider key does not match the exact schedule key.",
                            config.getSymbol());
                    continue;
                }

                if (!force && GlobalIndexSyncStatus.COMPLETE == config.getSyncStatus()) {
                    log.info("[{}] Already COMPLETE. Use force=true to refresh {} years of daily history.",
                            config.getSymbol(), historyBackfillYears);
                    continue;
                }
                if (!force && GlobalIndexSyncStatus.IN_PROGRESS == config.getSyncStatus()) {
                    log.info("[{}] Global history sync is already marked IN_PROGRESS; skipping duplicate request.",
                            config.getSymbol());
                    continue;
                }

                config.setSyncStatus(GlobalIndexSyncStatus.IN_PROGRESS);
                configRepository.save(config);

                backfillDailyHistory(config, market.get(), provider);

            } catch (Exception e) {
                log.error("[{}] Global daily-history backfill failed: {}", config.getSymbol(), e.getMessage(), e);
                try {
                    config.setSyncStatus(GlobalIndexSyncStatus.FAILED);
                    configRepository.save(config);
                } catch (Exception saveEx) {
                    log.error("[{}] Failed to persist FAILED status: {}", config.getSymbol(), saveEx.getMessage());
                }
            }
        }

        log.info("Global index historical sync complete.");
    }

    private void backfillDailyHistory(GlobalIndexConfigDocument config,
            GlobalMarketScheduleService.MarketEntry market, MarketDataProvider provider) {
        LocalDate toDate = scheduleService.latestCompletedSessionDate(market, Instant.now()).orElse(null);
        if (toDate == null) {
            config.setSyncStatus(GlobalIndexSyncStatus.PENDING);
            configRepository.save(config);
            log.warn("[{}] Global history remains PENDING: no completed exchange session could be resolved.",
                    config.getSymbol());
            return;
        }

        LocalDate fromDate = toDate.minusYears(historyBackfillYears);
        Instant requestStarted = Instant.now();
        log.info("[{}] Global history fetch started: key={}, unit=DAY, from={}, to={}, lookbackYears={}",
                config.getSymbol(), config.getInstrumentKey(), fromDate, toDate, historyBackfillYears);

        HistoricalData response = provider.getHistoricalData(
                config.getInstrumentKey(),
                Date.from(fromDate.atStartOfDay(ZoneOffset.UTC).toInstant()),
                Date.from(toDate.atStartOfDay(ZoneOffset.UTC).toInstant()),
                TimeFrame.DAY,
                false,
                new HashMap<>());

        if (response == null || response.getDataPoints() == null || response.getDataPoints().isEmpty()) {
            config.setSyncStatus(GlobalIndexSyncStatus.PENDING);
            configRepository.save(config);
            log.warn("[{}] Global history returned no daily candles; existing Influx data is unchanged and status remains PENDING.",
                    config.getSymbol());
            return;
        }

        TreeMap<Instant, OHLCVTPoint> candles = new TreeMap<>();
        int rejected = 0;
        for (OHLCVTPoint point : response.getDataPoints()) {
            if (!isValidCandle(point)) {
                rejected++;
                continue;
            }
            Instant sourceTime = point.getTime().toInstant(ZoneOffset.UTC);
            if (sourceTime.isAfter(Instant.now().plusSeconds(60))) {
                rejected++;
                continue;
            }
            candles.putIfAbsent(sourceTime, point);
        }

        if (candles.isEmpty()) {
            config.setSyncStatus(GlobalIndexSyncStatus.PENDING);
            configRepository.save(config);
            log.warn("[{}] Global history had no valid candles (received={}, rejected={}); status remains PENDING.",
                    config.getSymbol(), response.getDataPoints().size(), rejected);
            return;
        }

        List<GlobalIndexMeasurement> measurements = new ArrayList<>(candles.size());
        Double previousClose = null;
        for (Map.Entry<Instant, OHLCVTPoint> candle : candles.entrySet()) {
            OHLCVTPoint point = candle.getValue();
            GlobalIndexMeasurement measurement = new GlobalIndexMeasurement();
            measurement.setInstrumentKey(config.getInstrumentKey());
            measurement.setName(config.getName());
            measurement.setSegment("GLOBAL");
            measurement.setTime(candle.getKey());
            measurement.setOpen(point.getOpen());
            measurement.setHigh(point.getHigh());
            measurement.setLow(point.getLow());
            measurement.setClose(point.getClose());
            measurement.setPreviousClose(previousClose);
            if (previousClose != null && previousClose > 0) {
                measurement.setChangePercent((point.getClose() - previousClose) / previousClose * 100.0);
            }
            measurements.add(measurement);
            previousClose = point.getClose();
        }

        influxRepository.saveAll(measurements);
        config.setSyncStatus(GlobalIndexSyncStatus.COMPLETE);
        config.setLastSyncedAt(Instant.now());
        configRepository.save(config);
        log.info("[{}] Global history backfill persisted: key={}, received={}, stored={}, rejected={}, from={}, to={}, elapsedMs={}",
                config.getSymbol(), config.getInstrumentKey(), response.getDataPoints().size(), measurements.size(),
                rejected, fromDate, toDate, Duration.between(requestStarted, Instant.now()).toMillis());
    }

    private boolean isValidCandle(OHLCVTPoint point) {
        if (point == null || point.getTime() == null || point.getOpen() == null || point.getHigh() == null
                || point.getLow() == null || point.getClose() == null) {
            return false;
        }
        double open = point.getOpen();
        double high = point.getHigh();
        double low = point.getLow();
        double close = point.getClose();
        return Double.isFinite(open) && Double.isFinite(high) && Double.isFinite(low) && Double.isFinite(close)
                && open > 0 && high > 0 && low > 0 && close > 0
                && high >= Math.max(open, close) && low <= Math.min(open, close) && high >= low;
    }
}
