package com.am.marketdata.api.config;

import com.am.marketdata.api.websocket.MarketDataWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MarketDataWebSocketHandler marketDataWebSocketHandler;

    public WebSocketConfig(MarketDataWebSocketHandler marketDataWebSocketHandler) {
        this.marketDataWebSocketHandler = marketDataWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(marketDataWebSocketHandler, "/ws/market-data-stream")
                .setAllowedOrigins("*");
    }
}
