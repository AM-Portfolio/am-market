package com.am.marketdata.api;

import com.am.marketdata.common.model.marketinfo.UpstoxInstrumentKeys;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MarketInfoSymbolMapTest {
    @Test
    void mapsKnownIndexNamesAndAliases() {
        assertEquals("NSE_INDEX|Nifty 50",
                UpstoxInstrumentKeys.mapSymbolToInstrumentKey("NIFTY 50"));
        assertEquals("NSE_INDEX|Nifty Bank",
                UpstoxInstrumentKeys.mapSymbolToInstrumentKey("NIFTY BANK"));
        assertEquals("NSE_INDEX|Nifty Bank",
                UpstoxInstrumentKeys.mapSymbolToInstrumentKey("banknifty"));
        assertEquals("BSE_INDEX|SENSEX",
                UpstoxInstrumentKeys.mapSymbolToInstrumentKey("sensex"));
    }

    @Test
    void preservesInstrumentKeysAndTitleCasesFallback() {
        assertEquals("NSE_INDEX|Nifty 50",
                UpstoxInstrumentKeys.mapSymbolToInstrumentKey("NSE_INDEX|Nifty 50"));
        assertEquals("NSE_INDEX|Nifty It",
                UpstoxInstrumentKeys.mapSymbolToInstrumentKey("NIFTY IT"));
    }

    @Test
    void rejectsBlankSymbols() {
        assertThrows(IllegalArgumentException.class,
                () -> UpstoxInstrumentKeys.mapSymbolToInstrumentKey(" "));
    }
}
