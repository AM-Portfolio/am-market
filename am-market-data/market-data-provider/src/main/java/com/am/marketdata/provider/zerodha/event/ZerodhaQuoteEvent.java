package com.am.marketdata.provider.zerodha.event;

import com.zerodhatech.models.Quote;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a Zerodha quote is received
 */
@Getter
public class ZerodhaQuoteEvent extends ApplicationEvent {
    
    private final String symbol;
    private final Quote quote;
    
    /**
     * Create a new ZerodhaQuoteEvent
     * @param symbol The instrument symbol
     * @param quote The quote data
     */
    public ZerodhaQuoteEvent(String symbol, Quote quote) {
        super(quote);
        this.symbol = symbol;
        this.quote = quote;
    }
}
