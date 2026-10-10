package com.am.marketdata.provider.upstox;

import com.am.marketdata.provider.common.InstrumentContext;
import com.am.marketdata.provider.upstox.resolver.UpstoxSymbolResolver;
import com.upstox.api.GetMarketQuoteLastTradedPriceResponseV3;
import com.upstox.api.MarketQuoteSymbolLtpV3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
}
