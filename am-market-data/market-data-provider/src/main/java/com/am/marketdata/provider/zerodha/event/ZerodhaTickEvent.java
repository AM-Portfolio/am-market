package com.am.marketdata.provider.zerodha.event;

import com.zerodhatech.models.Tick;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a Zerodha tick is received from the websocket
 */
@Getter
public class ZerodhaTickEvent extends ApplicationEvent {
    
    private final Tick tick;
    
    /**
     * Create a new ZerodhaTickEvent
     * @param tick The tick data
     */
    public ZerodhaTickEvent(Tick tick) {
        super(tick);
        this.tick = tick;
    }
}
