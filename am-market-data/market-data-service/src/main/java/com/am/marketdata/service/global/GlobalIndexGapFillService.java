package com.am.marketdata.service.global;

import com.am.common.investment.persistence.document.global.GlobalIndexConfigDocument;
import com.am.common.investment.persistence.document.global.GlobalIndexConfigRepository;
import com.am.common.investment.persistence.influx.measurement.GlobalIndexMeasurement;
import com.am.common.investment.persistence.repository.measurement.impl.GlobalIndexInfluxRepository;
import com.am.common.investment.model.historical.HistoricalData;
import com.am.common.investment.model.historical.OHLCVTPoint;
import com.am.marketdata.common.model.TimeFrame;
import com.marketdata.common.MarketDataProvider;
import com.am.marketdata.provider.common.MarketDataProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalIndexGapFillService implements ApplicationRunner {

    private final GlobalIndexConfigRepository configRepository;
    private final GlobalIndexInfluxRepository influxRepository;
    private final MarketDataProviderFactory providerFactory;
    private final GlobalMarketScheduleService scheduleService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Triggering Global Index Intraday Gap-Fill Service asynchronously...");
        CompletableFuture.runAsync(this::fillGaps);
    }

    public void fillGaps() {
        log.info("Starting Global Index Intraday Gap-Fill background execution...");
        
        try {
            List<GlobalIndexConfigDocument> configs = configRepository.findAll();
            MarketDataProvider provider = providerFactory.getProvider("upstox");

            for (GlobalIndexConfigDocument config : configs) {
                String instrumentKey = config.getInstrumentKey();
                if (instrumentKey == null || instrumentKey.isBlank()) {
                    log.info("Skipping global history gap-fill for {}: no verified provider instrument is configured.", config.getSymbol());
                    continue;
                }
                Optional<GlobalIndexMeasurement> lastMeasurementOpt = influxRepository.findLastMeasurement(instrumentKey);

                if (lastMeasurementOpt.isEmpty() || lastMeasurementOpt.get().getTime() == null) {
                    log.info("No previous InfluxDB data found for {}. Gap fill skipped.", config.getSymbol());
                    continue;
                }

                GlobalIndexMeasurement lastMeasurement = lastMeasurementOpt.get();
                Instant lastTime = lastMeasurement.getTime();
                Double sessionBaseline = lastMeasurement.getPreviousClose();
                Instant now = Instant.now();
                Duration gap = Duration.between(lastTime, now);

                // Only fill gap if it is larger than 5 minutes
                if (gap.toMinutes() <= 5) {
                    log.debug("No gap detected for {}. (Gap: {} mins)", config.getSymbol(), gap.toMinutes());
                    continue;
                }

                log.info("Gap of {} minutes detected for {} since last timestamp {}", gap.toMinutes(), config.getSymbol(), lastTime);

                // Fetch 1-minute historical data for the gap window
                Date fromDate = Date.from(lastTime);
                Date toDate = Date.from(now);

                try {
                    HistoricalData histData = provider.getHistoricalData(
                            instrumentKey,
                            fromDate,
                            toDate,
                            TimeFrame.MINUTE,
                            false,
                            new HashMap<>()
                    );

                    if (histData != null && histData.getDataPoints() != null && !histData.getDataPoints().isEmpty()) {
                        List<GlobalIndexMeasurement> measurements = new ArrayList<>();
                        for (OHLCVTPoint pt : histData.getDataPoints()) {
                            // Skip data points that we already have (equal or before lastTime)
                            if (!pt.getTime().toInstant(java.time.ZoneOffset.UTC).isAfter(lastTime)) {
                                continue;
                            }

                            GlobalIndexMeasurement m = new GlobalIndexMeasurement();
                            m.setInstrumentKey(instrumentKey);
                            m.setName(config.getName());
                            m.setSegment("GLOBAL");
                            m.setTime(pt.getTime().toInstant(java.time.ZoneOffset.UTC));
                            m.setOpen(pt.getOpen());
                            m.setHigh(pt.getHigh());
                            m.setLow(pt.getLow());
                            m.setClose(pt.getClose());
                            measurements.add(m);
                        }

                        if (!measurements.isEmpty()) {
                            // Sort by time ascending
                            measurements.sort(Comparator.comparing(GlobalIndexMeasurement::getTime));

                            // Use session baseline previousClose from last confirmed point or first candle open
                            double baseline = (sessionBaseline != null && sessionBaseline > 0)
                                    ? sessionBaseline
                                    : measurements.get(0).getOpen();

                            for (GlobalIndexMeasurement m : measurements) {
                                m.setPreviousClose(baseline);
                                if (baseline > 0) {
                                    m.setChangePercent(((m.getClose() - baseline) / baseline) * 100.0);
                                } else {
                                    m.setChangePercent(0.0);
                                }
                            }

                            influxRepository.saveAll(measurements);
                            log.info("Successfully filled gap with {} intraday points for {}", measurements.size(), config.getSymbol());
                        }
                    } else {
                        log.debug("No new historical points returned for {} during gap window", config.getSymbol());
                    }
                } catch (Exception e) {
                    log.error("Failed to perform gap fill for symbol={}: {}", config.getSymbol(), e.getMessage(), e);
                }

                // Small delay to respect rate limit
                Thread.sleep(300);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("Global index gap fill job interrupted");
        } catch (Exception e) {
            log.error("Error running global index gap fill job", e);
        }
    }
}
