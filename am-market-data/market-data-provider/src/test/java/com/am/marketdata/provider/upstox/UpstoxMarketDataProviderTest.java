package com.am.marketdata.provider.upstox;

import com.am.marketdata.provider.common.InstrumentContext;
import com.am.marketdata.provider.upstox.resolver.UpstoxSymbolResolver;
import com.am.marketdata.common.model.InstrumentIdentity;
import com.am.marketdata.common.model.UpstoxInstrument;
import com.am.marketdata.common.provider.InstrumentDataProvider;
import com.upstox.api.GetMarketQuoteLastTradedPriceResponseV3;
import com.upstox.api.MarketQuoteSymbolLtpV3;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.time.ZonedDateTime;

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

    /**
     * Verifies that StockQuote dynamically derives previousClose = lastPrice - change
     * when upstream Upstox does not provide an explicit 'previous_close' field.
     * Validates against real Upstox production quotes:
     * - ADANIENT: 2628.0 - (-6.4) = 2634.40 (change: -6.4, changePercent: -0.24%)
     * - RELIANCE: 1155.2 - (-15.1) = 1170.30 (change: -15.1, changePercent: -1.29%)
     */
    @Test
    void stockQuoteDerivesPreviousCloseFromNetChangeDynamicBaseline() {
        // ADANIENT: lastPrice 2628.0, net_change -6.4 -> previousClose 2634.40
        com.am.marketdata.provider.upstox.model.common.StockQuote adanient =
                com.am.marketdata.provider.upstox.model.common.StockQuote.builder()
                        .symbol("ADANIENT")
                        .lastPrice(2628.0)
                        .change(-6.4)
                        .build();

        assertEquals(2634.40, adanient.getPreviousClose(), 0.001);
        assertEquals(-6.4, adanient.getChange(), 0.001);
        assertEquals(-0.24, adanient.getChangePercent(), 0.01);

        // RELIANCE: lastPrice 1155.2, net_change -15.1 -> previousClose 1170.30
        com.am.marketdata.provider.upstox.model.common.StockQuote reliance =
                com.am.marketdata.provider.upstox.model.common.StockQuote.builder()
                        .symbol("RELIANCE")
                        .lastPrice(1155.2)
                        .change(-15.1)
                        .build();

        assertEquals(1170.30, reliance.getPreviousClose(), 0.001);
        assertEquals(-15.1, reliance.getChange(), 0.001);
        assertEquals(-1.29, reliance.getChangePercent(), 0.01);
    }

    @Test
    void sameSnapshotNetChangeWinsOverStaleExplicitPreviousClose() {
        var quote = com.am.marketdata.provider.upstox.model.common.StockQuote.builder()
                .lastPrice(1155.2)
                .change(-15.1)
                .previousClose(1182.0)
                .build();

        assertEquals(1170.30, quote.getPreviousClose(), 0.001);
    }

    @Test
    void upstoxQuoteJsonMapsOptionalDepthAndSnapshotFields() throws Exception {
        String payload = """
                {"last_price":1154.5,"net_change":-15.8,"volume":9099955,"oi":12345,
                 "oi_day_high":13000,"oi_day_low":11000,
                 "average_price":1163.71,"total_buy_quantity":747519.0,
                 "total_sell_quantity":882868.0,"lower_circuit_limit":1053.3,
                 "upper_circuit_limit":1287.3,"timestamp":"2026-10-12T14:47:04.261+05:30",
                 "depth":{"buy":[{"quantity":2896,"price":1154.5,"orders":29}],
                          "sell":[{"quantity":207,"price":1154.8,"orders":3}]}}
                """;
        var quote = new ObjectMapper().findAndRegisterModules().readValue(payload,
                com.am.marketdata.provider.upstox.model.common.StockQuote.class);

        assertEquals(1154.5, quote.getLastPrice(), 0.001);
        assertEquals(1170.3, quote.getPreviousClose(), 0.001);
        assertEquals(9099955L, quote.getVolume());
        assertEquals(12345L, quote.getOi());
        assertEquals(13000L, quote.getOiDayHigh());
        assertEquals(11000L, quote.getOiDayLow());
        assertEquals(ZonedDateTime.parse("2026-10-12T14:47:04.261+05:30").toInstant(), quote.getLastUpdateTime().toInstant());
        assertEquals(1, quote.getMarketDepth().getBuyOrders().size());
        assertEquals(2896L, quote.getMarketDepth().getBuyOrders().get(0).getQuantity());
        assertEquals(1, quote.getMarketDepth().getSellOrders().size());
    }

    /**
     * Verifies that UpstoxMarketDataProvider.getOHLC for daily timeframe queries Upstox
     * quotes (via upstoxApiService.getLtp) to obtain lastPrice, OHLC, and derived previousClose
     * in a single batch, correctly propagating the session baseline to returned OHLCQuote models.
     */
    @Test
    void getOhlcPopulatesValidatedSessionBaselineFromUpstoxQuotes() {
        UpstoxApiService apiService = mock(UpstoxApiService.class);
        UpstoxSdkService sdkService = mock(UpstoxSdkService.class);
        UpstoxSymbolResolver resolver = mock(UpstoxSymbolResolver.class);

        String adaniKey = "NSE_EQ|INE423A01024";
        String relianceKey = "NSE_EQ|INE002A01018";

        InstrumentContext ctx = new InstrumentContext(
                List.of(adaniKey, relianceKey),
                Map.of(adaniKey, "ADANIENT", relianceKey, "RELIANCE"));
        when(resolver.resolveContext(List.of("ADANIENT", "RELIANCE"))).thenReturn(ctx);

        com.am.marketdata.provider.upstox.model.common.StockQuote adaniQuote =
                com.am.marketdata.provider.upstox.model.common.StockQuote.builder()
                        .symbol("ADANIENT")
                        .lastPrice(2628.0)
                        .change(-6.4)
                        .oi(500L)
                        .oiDayHigh(600L)
                        .oiDayLow(400L)
                        .ohlc(new com.am.marketdata.provider.upstox.model.common.StockQuote.Ohlc(2641.0, 2664.2, 2625.2, 2628.0))
                        .build();

        com.am.marketdata.provider.upstox.model.common.StockQuote relianceQuote =
                com.am.marketdata.provider.upstox.model.common.StockQuote.builder()
                        .symbol("RELIANCE")
                        .lastPrice(1155.2)
                        .change(-15.1)
                        .ohlc(new com.am.marketdata.provider.upstox.model.common.StockQuote.Ohlc(1180.3, 1185.5, 1153.2, 1155.2))
                        .lastTradeTime("1791796644261")
                        .build();

        var depth = new com.am.marketdata.provider.upstox.model.common.MarketDepth();
        var bid = new com.am.marketdata.provider.upstox.model.common.MarketDepth.DepthEntry();
        bid.setPrice(2628.0);
        bid.setQuantity(120L);
        bid.setOrders(4);
        depth.setBuyOrders(List.of(bid));
        adaniQuote.setMarketDepth(depth);
        adaniQuote.setLastUpdateTime(ZonedDateTime.parse("2026-10-12T14:47:04.261+05:30"));

        com.am.marketdata.provider.upstox.model.MarketQuoteResponse response =
                new com.am.marketdata.provider.upstox.model.MarketQuoteResponse();
        response.setData(Map.of(adaniKey, adaniQuote, relianceKey, relianceQuote));

        when(apiService.getLtp(List.of(adaniKey, relianceKey))).thenReturn(response);

        UpstoxMarketDataProvider provider = new UpstoxMarketDataProvider(apiService, sdkService, resolver);
        Map<String, com.am.marketdata.common.model.OHLCQuote> quotes =
                provider.getOHLC(List.of("ADANIENT", "RELIANCE"), com.am.marketdata.common.model.TimeFrame.DAY);

        assertEquals(2, quotes.size());
        assertEquals(2628.0, quotes.get("ADANIENT").getLastPrice(), 0.001);
        assertEquals(2634.40, quotes.get("ADANIENT").getPreviousClose(), 0.001);
        assertEquals(500L, quotes.get("ADANIENT").getOi());
        assertEquals(600L, quotes.get("ADANIENT").getOiDayHigh());
        assertEquals(400L, quotes.get("ADANIENT").getOiDayLow());
        assertEquals(1155.2, quotes.get("RELIANCE").getLastPrice(), 0.001);
        assertEquals(1170.30, quotes.get("RELIANCE").getPreviousClose(), 0.001);
        assertEquals(java.time.Instant.ofEpochMilli(1791796644261L),
                quotes.get("RELIANCE").getQuoteTimestamp());
        assertEquals(1, quotes.get("ADANIENT").getMarketDepth().getBuy().size());
        assertEquals(120L, quotes.get("ADANIENT").getMarketDepth().getBuy().get(0).getQuantity());
        assertEquals(2628.0, quotes.get("ADANIENT").getMarketDepth().getBuy().get(0).getPrice(), 0.001);
        assertEquals(120L, quotes.get("ADANIENT").getMarketDepth().getBuy().get(0).getQuantity());
        assertEquals(ZonedDateTime.parse("2026-10-12T14:47:04.261+05:30").toInstant(),
                quotes.get("ADANIENT").getQuoteTimestamp());
    }
}
