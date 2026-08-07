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

        // Mock a valid instrument response for ISIN
        UpstoxInstrument barodaIsinInst = new UpstoxInstrument();
        barodaIsinInst.setIsin("INE028A01039");
        when(upstoxInstrumentRepository.findByIsinIn(List.of("INE028A01039")))
                .thenReturn(List.of(barodaIsinInst));

        // WHEN: We run resolveSymbols validation with fetchIndexStocks = false
        Set<String> resolved = instrumentUtils.resolveSymbols(candidates, false);

        // THEN: We verify both standard ticker and ISIN resolved successfully
        assertNotNull(resolved);
        
        // Both candidates must be in the resolved list
        assertTrue(resolved.contains("BANKBARODA"));
        assertTrue(resolved.contains("INE028A01039"));

        // Verify the database was queried correctly for both types
        verify(upstoxInstrumentRepository, times(1)).findByTradingSymbolIn(List.of("BANKBARODA"));
        verify(upstoxInstrumentRepository, times(1)).findByIsinIn(List.of("INE028A01039"));

        // Verify that unresolvedSymbolRepository was NEVER called (meaning no symbols failed)
        verify(unresolvedSymbolRepository, never()).incrementRequestCount(anyString());
    }

    // Helper for assertions
    private void assertNotNull(Object obj) {
        if (obj == null) {
            throw new AssertionError("Object should not be null");
        }
    }
}
