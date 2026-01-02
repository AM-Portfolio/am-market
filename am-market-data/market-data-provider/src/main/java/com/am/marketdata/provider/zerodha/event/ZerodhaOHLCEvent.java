package com.am.marketdata.provider.zerodha.event;

import com.zerodhatech.models.OHLCQuote;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when Zerodha OHLC data is received
 */
@Getter
public class ZerodhaOHLCEvent extends ApplicationEvent {
    
    private final String symbol;
    private final OHLCQuote ohlcQuote;
    
    /**
     * Create a new ZerodhaOHLCEvent
     * @param symbol The instrument symbol
     * @param ohlcQuote The OHLC data
     */
    public ZerodhaOHLCEvent(String symbol, OHLCQuote ohlcQuote) {
        super(ohlcQuote);
        this.symbol = symbol;
        this.ohlcQuote = ohlcQuote;
    }
}
