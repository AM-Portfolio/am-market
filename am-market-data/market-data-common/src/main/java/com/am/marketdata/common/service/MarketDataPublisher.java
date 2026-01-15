package com.am.marketdata.common.service;

import com.am.marketdata.common.model.MarketDataUpdate;

public interface MarketDataPublisher {
    void publish(MarketDataUpdate update);
}
