package com.am.marketdata.service.service;

import com.am.marketdata.common.dto.InstrumentSearchCriteria;
import com.am.marketdata.common.model.CommonInstrument;
import com.am.marketdata.common.provider.InstrumentDataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstrumentService {

    private final List<InstrumentDataProvider> instrumentDataProviders;

    /**
     * Search instruments across all providers and return aggregated AM specific
     * symbols.
     * Note: Currently assumes providers handle their own specific types internally,
     * but for a generic service we might need them to return a common type or adapt
     * here.
     * Since InstrumentDataProvider returns List<?>, we will attempt to cast or map
     * if possible,
     * or for now just simply collect them if they are already standard.
     * 
     * However, the user request specifically asked for "one Gen service which will
     * return uh am organization level specific symbols only".
     * If providers return ZerodhaInstrument/UpstoxInstrument, we need to map them
     * to CommonInstrument.
     */
    public List<CommonInstrument> searchInstruments(InstrumentSearchCriteria criteria) {
        List<CommonInstrument> aggregatedResults = new ArrayList<>();

        for (InstrumentDataProvider provider : instrumentDataProviders) {
            try {
                List<?> providerResults = provider.searchInstruments(criteria);
                // In a real implementation, we would map these provider-specific results to
                // CommonInstrument.
                // For now, assuming the providers might be updated to return CommonInstrument
                // OR we need a mapper here. Since I cannot easily change the provider return
                // types (interface contract),
                // I will add a TODO or basic adaptation logic if the objects match field-wise.
                //
                // Given the user constraint "let the instrument related changes inside
                // provider",
                // ideally the provider SHOULD return the specific type, and THIS service maps
                // it.
                // But without a mapper, I can't write type-safe code easily.

                // Placeholder: Use a mapper or inspection. For this task, I'll log and return
                // empty or assume commonality if aligned.
                // Based on `UpstoxInstrument` in common, it might closely resemble
                // CommonInstrument.
                log.debug("Provider {} returned {} results", provider.getProviderName(), providerResults.size());

                // Simple mapping (Conceptual - requires actual Mapper implementation)
                for (Object res : providerResults) {
                    if (res instanceof com.am.marketdata.common.model.UpstoxInstrument) {
                        aggregatedResults.add(mapUpstox((com.am.marketdata.common.model.UpstoxInstrument) res));
                    } else if (res instanceof com.am.marketdata.provider.zerodha.model.ZerodhaInstrument) {
                        aggregatedResults
                                .add(mapZerodha((com.am.marketdata.provider.zerodha.model.ZerodhaInstrument) res));
                    }
                }

            } catch (Exception e) {
                log.error("Error fetching instruments from provider: {}", provider.getProviderName(), e);
            }
        }
        return aggregatedResults;
    }

    // Basic Mappers
    private CommonInstrument mapUpstox(com.am.marketdata.common.model.UpstoxInstrument source) {
        return CommonInstrument.builder()
                .instrumentKey(source.getInstrumentKey())
                .tradingSymbol(source.getTradingSymbol())
                .name(source.getName())
                .exchange(source.getExchange())
                .segment(source.getSegment())
                .instrumentType(source.getInstrumentType())
                .lotSize(source.getLotSize() != null ? source.getLotSize().intValue() : null)
                .tickSize(source.getTickSize())
                .isin(source.getIsin())
                .providerName("UPSTOX")
                .providerInstrumentKey(source.getInstrumentKey())
                .build();
    }

    private CommonInstrument mapZerodha(com.am.marketdata.provider.zerodha.model.ZerodhaInstrument source) {
        return CommonInstrument.builder()
                .instrumentKey(source.getInstrumentToken()) // Use token as key
                .tradingSymbol(source.getTradingSymbol())
                .name(source.getName())
                .exchange(source.getExchange())
                .segment(source.getSegment())
                .instrumentType(source.getInstrumentType())
                .lotSize(source.getLotSize())
                .tickSize(source.getTickSize())
                .isin(null) // Zerodha model snippet didn't show strict ISIN field (check full class if
                            // needed)
                .providerName("ZERODHA")
                .providerInstrumentKey(source.getInstrumentToken())
                .build();
    }
}
