package com.am.marketdata.api.config;

import com.am.marketdata.api.websocket.MarketDataWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket configuration supporting both:
 * 1. Basic WebSocket (for custom handlers)
 * 2. STOMP over WebSocket (for message broker)
 */
@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
@ConditionalOnProperty(name = "market-data.websocket.enabled", havingValue = "true", matchIfMissing = true)
@Profile("!isolated")
public class WebSocketConfig implements WebSocketConfigurer, WebSocketMessageBrokerConfigurer {

    private final MarketDataWebSocketHandler marketDataWebSocketHandler;

    public WebSocketConfig(MarketDataWebSocketHandler marketDataWebSocketHandler) {
        this.marketDataWebSocketHandler = marketDataWebSocketHandler;
    }

    // Basic WebSocket configuration
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(marketDataWebSocketHandler, "/ws/market-data-stream")
                .setAllowedOrigins("*");
    }

    // STOMP WebSocket configuration (for SimpMessagingTemplate)
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
}
