package com.am.marketdata.service.global;

import com.am.common.investment.persistence.document.global.GlobalIndexConfigDocument;
import com.am.common.investment.persistence.document.global.GlobalIndexConfigRepository;
import com.am.common.investment.persistence.influx.measurement.GlobalIndexMeasurement;
import com.am.common.investment.persistence.repository.measurement.impl.GlobalIndexInfluxRepository;
import com.am.marketdata.common.model.OHLCQuote;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalIndexCacheWriter {

    private final GlobalIndexInfluxRepository influxRepository;
    private final GlobalIndexConfigRepository configRepository;

    // Cache to store instrumentKey to name map to avoid frequent MongoDB calls on every tick
    private final ConcurrentHashMap<String, String> nameCache = new ConcurrentHashMap<>();

    /**
     * Writes an incoming WebSocket global index tick to InfluxDB.
     *
     * @param instrumentKey the Upstox instrument key (e.g., "GLOBAL_INDEX|DJI")
     * @param quote         the live tick quote
     */
    public void writeTick(String instrumentKey, OHLCQuote quote) {
        try {
            String name = nameCache.computeIfAbsent(instrumentKey, key -> {
                String symbol = key.replace("GLOBAL_INDEX|", "");
                return configRepository.findBySymbol(symbol)
                        .map(GlobalIndexConfigDocument::getName)
                        .orElse(key);
            });

            GlobalIndexMeasurement measurement = new GlobalIndexMeasurement();
            measurement.setInstrumentKey(instrumentKey);
            measurement.setName(name);
            measurement.setSegment("GLOBAL");
            measurement.setTime(Instant.now());
            
            if (quote.getOhlc() != null) {
                measurement.setOpen(quote.getOhlc().getOpen());
                measurement.setHigh(quote.getOhlc().getHigh());
                measurement.setLow(quote.getOhlc().getLow());
                measurement.setClose(quote.getOhlc().getClose());
            } else {
                // If tick doesn't contain OHLC block, fallback to last price
                measurement.setOpen(quote.getLastPrice());
                measurement.setHigh(quote.getLastPrice());
                measurement.setLow(quote.getLastPrice());
                measurement.setClose(quote.getLastPrice());
            }

            measurement.setPreviousClose(quote.getPreviousClose());
            
            double change = quote.getLastPrice() - quote.getPreviousClose();
            double changePercent = quote.getPreviousClose() > 0 ? (change / quote.getPreviousClose()) * 100 : 0.0;
            measurement.setChangePercent(changePercent);

            influxRepository.save(measurement);
            
            log.debug("Written global index tick to InfluxDB for instrumentKey={}", instrumentKey);
        } catch (Exception e) {
            log.error("Failed to write global index tick to InfluxDB for instrumentKey={}: {}", instrumentKey, e.getMessage(), e);
        }
    }
}
