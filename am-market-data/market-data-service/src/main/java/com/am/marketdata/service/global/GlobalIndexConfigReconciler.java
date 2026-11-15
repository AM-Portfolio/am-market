package com.am.marketdata.service.global;

import com.am.common.investment.persistence.document.global.GlobalIndexConfigDocument;
import com.am.common.investment.persistence.document.global.GlobalIndexConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reconciles global index configurations with global-market-schedule.yml.
 * Runs on startup by default (can be disabled via global.index-config.reconcile=false).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "global.index-config.reconcile", havingValue = "true", matchIfMissing = true)
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class GlobalIndexConfigReconciler implements ApplicationRunner {

    private final GlobalIndexConfigRepository configRepository;
    private final GlobalMarketScheduleService scheduleService;

    @Override
    public void run(ApplicationArguments args) {
        Map<String, GlobalIndexConfigDocument> existingConfigs = configRepository.findAll().stream()
                .collect(Collectors.toMap(GlobalIndexConfigDocument::getSymbol, Function.identity(), (a, b) -> a));
        int updated = 0;
        int inserted = 0;

        for (GlobalMarketScheduleService.MarketEntry source : scheduleService.getMarkets()) {
            if (source == null || source.symbol() == null) continue;
            String symbol = source.symbol();
            String newKey = source.hasInstrument() ? source.instrumentKey() : null;

            GlobalIndexConfigDocument config = existingConfigs.get(symbol);
            if (config == null) {
                config = GlobalIndexConfigDocument.builder()
                        .symbol(symbol)
                        .name(source.name())
                        .instrumentKey(newKey)
                        .exchange(source.exchange())
                        .timezone(source.timezone())
                        .marketOpenTime(source.marketOpen())
                        .marketCloseTime(source.marketClose())
                        .influxAggregateOffset(source.influxAggregateOffset())
                        .build();
                configRepository.save(config);
                inserted++;
                log.info("[GlobalIndexConfigReconciler] Seeded missing config for {}: key={}", symbol, newKey);
                continue;
            }

            boolean changed = !java.util.Objects.equals(config.getInstrumentKey(), newKey)
                    || !java.util.Objects.equals(config.getTimezone(), source.timezone())
                    || !java.util.Objects.equals(config.getMarketOpenTime(), source.marketOpen())
                    || !java.util.Objects.equals(config.getMarketCloseTime(), source.marketClose())
                    || !java.util.Objects.equals(config.getExchange(), source.exchange())
                    || !java.util.Objects.equals(config.getInfluxAggregateOffset(), source.influxAggregateOffset());
            if (!changed) continue;

            log.info("[GlobalIndexConfigReconciler] Updating routing/session config for {}: instrumentKey {} -> {}",
                    config.getSymbol(), config.getInstrumentKey(), newKey);
            config.setInstrumentKey(newKey);
            config.setName(source.name());
            config.setExchange(source.exchange());
            config.setTimezone(source.timezone());
            config.setMarketOpenTime(source.marketOpen());
            config.setMarketCloseTime(source.marketClose());
            config.setInfluxAggregateOffset(source.influxAggregateOffset());
            configRepository.save(config);
            updated++;
        }
        log.info("[GlobalIndexConfigReconciler] Global index reconciliation complete: updated={}, inserted={}", updated, inserted);
    }
}
