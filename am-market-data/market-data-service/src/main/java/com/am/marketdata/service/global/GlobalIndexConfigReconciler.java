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
 * One-time, opt-in reconciliation for existing global-index configuration rows.
 * Enable with {@code global.index-config.reconcile=true} for a controlled run,
 * then disable it. Sync state and historical data are deliberately preserved.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "global.index-config.reconcile", havingValue = "true")
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class GlobalIndexConfigReconciler implements ApplicationRunner {

    private final GlobalIndexConfigRepository configRepository;
    private final GlobalMarketScheduleService scheduleService;

    @Override
    public void run(ApplicationArguments args) {
        Map<String, GlobalMarketScheduleService.MarketEntry> sourceBySymbol = scheduleService.getMarkets().stream()
                .collect(Collectors.toMap(GlobalMarketScheduleService.MarketEntry::symbol, Function.identity()));
        int updated = 0;
        for (GlobalIndexConfigDocument config : configRepository.findAll()) {
            GlobalMarketScheduleService.MarketEntry source = sourceBySymbol.get(config.getSymbol());
            if (source == null) continue;
            String newKey = source.hasInstrument() ? source.instrumentKey() : null;
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
            config.setExchange(source.exchange());
            config.setTimezone(source.timezone());
            config.setMarketOpenTime(source.marketOpen());
            config.setMarketCloseTime(source.marketClose());
            config.setInfluxAggregateOffset(source.influxAggregateOffset());
            configRepository.save(config);
            updated++;
        }
        log.warn("[GlobalIndexConfigReconciler] Completed opt-in reconciliation: updated {} rows. Disable global.index-config.reconcile after this run.", updated);
    }
}
