// package com.marketdata.config;

// import org.springframework.context.annotation.Configuration;
// import org.springframework.messaging.simp.config.MessageBrokerRegistry;
// import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
// import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
// import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// /**
//  * WebSocket configuration for real-time market data
//  */
// @Configuration
// @EnableWebSocketMessageBroker
// public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

//     @Override
//     public void configureMessageBroker(MessageBrokerRegistry config) {
//         // Enable a simple in-memory message broker for sending messages to clients
//         config.enableSimpleBroker("/topic");
        
//         // Set prefix for client-to-server messages
//         config.setApplicationDestinationPrefixes("/app");
//     }

//     @Override
//     public void registerStompEndpoints(StompEndpointRegistry registry) {
//         // Register STOMP endpoints with SockJS fallback
//         registry.addEndpoint("/market-data-ws")
//                 .setAllowedOrigins("*")
//                 .withSockJS();
//     }
// }
