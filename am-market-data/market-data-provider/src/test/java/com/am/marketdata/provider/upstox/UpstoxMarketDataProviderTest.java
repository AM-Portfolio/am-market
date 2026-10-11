package com.am.marketdata.provider.upstox;

import com.am.marketdata.provider.common.InstrumentContext;
import com.am.marketdata.provider.upstox.resolver.UpstoxSymbolResolver;
import com.am.marketdata.common.model.InstrumentIdentity;
import com.am.marketdata.common.model.UpstoxInstrument;
import com.am.marketdata.common.provider.InstrumentDataProvider;
import com.upstox.api.GetMarketQuoteLastTradedPriceResponseV3;
import com.upstox.api.MarketQuoteSymbolLtpV3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

class UpstoxMarketDataProviderTest {

    @Test
    void getLtpReturnsOneEntryForEachResolvedInstrument() throws Exception {
        String instrumentKey = "NSE_EQ|INE009A01021";
        String requestedSymbol = "NSE:INFY";

        UpstoxApiService apiService = mock(UpstoxApiService.class);
        UpstoxSdkService sdkService = mock(UpstoxSdkService.class);
        UpstoxSymbolResolver resolver = mock(UpstoxSymbolResolver.class);
        when(resolver.resolveContext(List.of(requestedSymbol)))
                .thenReturn(new InstrumentContext(List.of(instrumentKey), Map.of(instrumentKey, requestedSymbol)));

        MarketQuoteSymbolLtpV3 quote = mock(MarketQuoteSymbolLtpV3.class);
        when(quote.getLastPrice()).thenReturn(1023.4);
        GetMarketQuoteLastTradedPriceResponseV3 response = mock(GetMarketQuoteLastTradedPriceResponseV3.class);
        when(response.getData()).thenReturn(Map.of(instrumentKey, quote));
        when(sdkService.getLtp(anyList())).thenReturn(response);

        UpstoxMarketDataProvider provider = new UpstoxMarketDataProvider(apiService, sdkService, resolver);
        Map<String, com.zerodhatech.models.LTPQuote> result = provider.getLTP(new String[] { requestedSymbol });

        assertEquals(1, result.size());
        assertTrue(result.containsKey(requestedSymbol));
        assertEquals(1023.4, result.get(requestedSymbol).lastPrice, 0.001);
    }

    @Test
    void resolverUsesVerifiedMemberIsinWhenProviderInstrumentRowIsMissing() {
        InstrumentDataProvider instruments = mock(InstrumentDataProvider.class);
        UpstoxIndexIdentifier indexIdentifier = mock(UpstoxIndexIdentifier.class);
        when(indexIdentifier.resolveIndices(anyList())).thenReturn(Map.of());
        when(instruments.searchInstruments(any())).thenReturn(List.of());

        UpstoxSymbolResolver resolver = new UpstoxSymbolResolver(instruments, indexIdentifier);
        InstrumentIdentity identity = new InstrumentIdentity("TECHM", "INE669C01036", "NSE");

        InstrumentContext context = resolver.resolveContextForIdentities(List.of(identity));

        String instrumentKey = "NSE_EQ|INE669C01036";
        assertTrue(context.getInstrumentKeys().contains(instrumentKey));
        assertEquals("TECHM", context.getSymbol(instrumentKey));
    }

    @Test
    void resolverReplacesConflictingProviderMasterIdentityWithMemberIsin() {
        InstrumentDataProvider instruments = mock(InstrumentDataProvider.class);
        UpstoxIndexIdentifier indexIdentifier = mock(UpstoxIndexIdentifier.class);
        when(indexIdentifier.resolveIndices(anyList())).thenReturn(Map.of());
        UpstoxInstrument stale = new UpstoxInstrument();
        stale.setInstrumentKey("NSE_EQ|INE467B01029");
        stale.setTradingSymbol("TECHM");
        stale.setExchange("NSE");
        stale.setSegment("NSE_EQ");
        stale.setIsin("INE467B01029");
        doReturn(List.of(stale)).when(instruments).searchInstruments(any());

        UpstoxSymbolResolver resolver = new UpstoxSymbolResolver(instruments, indexIdentifier);
        InstrumentContext context = resolver.resolveContextForIdentities(
                List.of(new InstrumentIdentity("TECHM", "INE669C01036", "NSE")));

        assertTrue(context.getInstrumentKeys().contains("NSE_EQ|INE669C01036"));
        assertTrue(!context.getInstrumentKeys().contains("NSE_EQ|INE467B01029"));
        assertEquals("TECHM", context.getSymbol("NSE_EQ|INE669C01036"));
    }
}
