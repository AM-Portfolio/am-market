package com.am.marketdata.api.util;

import com.am.marketdata.common.model.UpstoxInstrument;
import com.am.common.investment.service.StockIndicesMarketDataService;
import com.am.marketdata.service.repo.UnresolvedSymbolRepository;
import com.am.marketdata.provider.upstox.repo.UpstoxInstrumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstrumentUtilsTest {

    @Mock
    private StockIndicesMarketDataService stockIndicesMarketDataService;

    @Mock
    private UpstoxInstrumentRepository upstoxInstrumentRepository;

    @Mock
    private UnresolvedSymbolRepository unresolvedSymbolRepository;

    private InstrumentUtils instrumentUtils;

    @BeforeEach
    void setUp() {
        instrumentUtils = new InstrumentUtils(
                stockIndicesMarketDataService,
                upstoxInstrumentRepository,
                unresolvedSymbolRepository
        );
    }

    @Test
    void testResolveSymbols_WithMixedTickersAndIsins_ResolvesBothWithoutUnresolvedErrors() {
        // GIVEN: A list of subscription candidates containing one normal symbol and one ISIN
        List<String> candidates = List.of("BANKBARODA", "INE028A01039");

        // Mock a valid instrument response for normal symbol
        UpstoxInstrument barodaSymbolInst = new UpstoxInstrument();
        barodaSymbolInst.setTradingSymbol("BANKBARODA");
        when(upstoxInstrumentRepository.findByTradingSymbolIn(List.of("BANKBARODA")))
                .thenReturn(List.of(barodaSymbolInst));

        // Mock a valid instrument response for ISIN — quotes must use trading symbol
        UpstoxInstrument barodaIsinInst = new UpstoxInstrument();
        barodaIsinInst.setIsin("INE028A01039");
        barodaIsinInst.setTradingSymbol("BANKBARODA");
        barodaIsinInst.setExchange("NSE_EQ");
        barodaIsinInst.setInstrumentType("EQ");
        when(upstoxInstrumentRepository.findByIsinIn(List.of("INE028A01039")))
                .thenReturn(List.of(barodaIsinInst));

        // WHEN: We run resolveSymbols validation with fetchIndexStocks = false
        Set<String> resolved = instrumentUtils.resolveSymbols(candidates, false);

        // THEN: ISIN is rewritten to the trading symbol Upstox actually quotes
        assertNotNull(resolved);
        assertTrue(resolved.contains("BANKBARODA"));
        assertTrue(!resolved.contains("INE028A01039"));

        verify(upstoxInstrumentRepository, times(1)).findByTradingSymbolIn(List.of("BANKBARODA"));
        verify(upstoxInstrumentRepository, times(1)).findByIsinIn(List.of("INE028A01039"));
        verify(unresolvedSymbolRepository, never()).incrementRequestCount(anyString());
    }

    @Test
    void aliasQuotesUnderOriginalIsins_copiesTickerQuoteOntoIsinKey() {
        UpstoxInstrument sgb = new UpstoxInstrument();
        sgb.setIsin("IN0020210228");
        sgb.setTradingSymbol("SGBD29VIII");
        sgb.setExchange("NSE");
        sgb.setInstrumentType("GB");
        when(upstoxInstrumentRepository.findByIsinIn(List.of("IN0020210228")))
                .thenReturn(List.of(sgb));

        Map<String, String> quotes = new java.util.HashMap<>();
        quotes.put("SGBD29VIII", "15130");

        instrumentUtils.aliasQuotesUnderOriginalIsins(List.of("IN0020210228"), quotes);

        assertEquals("15130", quotes.get("SGBD29VIII"));
        assertEquals("15130", quotes.get("IN0020210228"));
    }

    // Helper for assertions
    private void assertNotNull(Object obj) {
        if (obj == null) {
            throw new AssertionError("Object should not be null");
        }
    }
}
