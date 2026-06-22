package com.am.marketdata.api.service;

import com.am.marketdata.api.model.StreamConnectRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketDataPollingServiceStreamStrategyTest {

    @Test
    void mockProvider_alwaysStartsMockPollingWhenStreamRequested() {
        StreamConnectRequest request = StreamConnectRequest.builder()
                .provider("MOCK")
                .instrumentKeys(List.of("NIFTY 50", "NIFTY BANK"))
                .stream(true)
                .build();

        assertTrue(MarketDataPollingService.isMockProvider(request, "MOCK"));
        assertEquals(MarketDataPollingService.StreamStrategy.MOCK_POLLING,
                MarketDataPollingService.resolveStreamStrategy(request, "MOCK", false, true));
    }

    @Test
    void mockProvider_startsMockPollingWhenMarketClosed() {
        StreamConnectRequest request = StreamConnectRequest.builder()
                .provider("mock")
                .stream(true)
                .build();

        assertEquals(MarketDataPollingService.StreamStrategy.MOCK_POLLING,
                MarketDataPollingService.resolveStreamStrategy(request, "MOCK", false, true));
    }

    @Test
    void mockModeFlag_startsMockPollingWithoutProviderName() {
        StreamConnectRequest request = StreamConnectRequest.builder()
                .provider("UPSTOX")
                .mockMode(true)
                .stream(true)
                .build();

        assertTrue(MarketDataPollingService.isMockProvider(request, "MOCK"));
        assertEquals(MarketDataPollingService.StreamStrategy.MOCK_POLLING,
                MarketDataPollingService.resolveStreamStrategy(request, "UPSTOX", true, true));
    }

    @Test
    void mockProvider_snapshotOnlyWhenStreamDisabled() {
        StreamConnectRequest request = StreamConnectRequest.builder()
                .provider("MOCK")
                .stream(false)
                .build();

        assertEquals(MarketDataPollingService.StreamStrategy.SNAPSHOT_ONLY,
                MarketDataPollingService.resolveStreamStrategy(request, "MOCK", false, true));
    }

    @Test
    void serverDefaultMock_noProviderInRequest_startsMockPolling() {
        StreamConnectRequest request = StreamConnectRequest.builder()
                .instrumentKeys(List.of("NIFTY 50"))
                .stream(true)
                .build();

        assertTrue(MarketDataPollingService.isMockProvider(request, "MOCK"));
        assertEquals(MarketDataPollingService.StreamStrategy.MOCK_POLLING,
                MarketDataPollingService.resolveStreamStrategy(request, "MOCK", false, true));
    }

    @Test
    void upstoxClosed_returnsSnapshotOnlyUnlessForcePolling() {
        StreamConnectRequest request = StreamConnectRequest.builder()
                .provider("UPSTOX")
                .stream(true)
                .forcePolling(false)
                .build();

        assertEquals(MarketDataPollingService.StreamStrategy.SNAPSHOT_ONLY,
                MarketDataPollingService.resolveStreamStrategy(request, "UPSTOX", false, false));
    }

    @Test
    void upstoxOpen_returnsLiveStream() {
        StreamConnectRequest request = StreamConnectRequest.builder()
                .provider("UPSTOX")
                .stream(true)
                .build();

        assertEquals(MarketDataPollingService.StreamStrategy.LIVE_STREAM,
                MarketDataPollingService.resolveStreamStrategy(request, "UPSTOX", true, false));
    }
}
