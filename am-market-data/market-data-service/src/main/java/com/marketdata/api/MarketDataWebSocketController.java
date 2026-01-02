// package com.marketdata.api;

// import com.marketdata.service.common.MarketDataProvider;
// import com.marketdata.service.common.MarketDataProviderFactory;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.messaging.handler.annotation.MessageMapping;
// import org.springframework.messaging.handler.annotation.SendTo;
// import org.springframework.messaging.simp.SimpMessagingTemplate;
// import org.springframework.stereotype.Controller;

// import java.util.List;
// import java.util.Map;
// import java.util.concurrent.ConcurrentHashMap;

// /**
//  * WebSocket controller for real-time market data
//  */
// @Slf4j
// @Controller
// public class MarketDataWebSocketController {

//     private final MarketDataProviderFactory providerFactory;
//     private final SimpMessagingTemplate messagingTemplate;
//     private final Map<String, Object> activeTickerSessions = new ConcurrentHashMap<>();

//     public MarketDataWebSocketController(
//             MarketDataProviderFactory providerFactory,
//             SimpMessagingTemplate messagingTemplate) {
//         this.providerFactory = providerFactory;
//         this.messagingTemplate = messagingTemplate;
//     }

//     /**
//      * Subscribe to market data for instruments
//      * @param request Subscription request containing instruments
//      * @return Subscription status
//      */
//     @MessageMapping("/market-data/subscribe")
//     @SendTo("/topic/market-data/status")
//     public Map<String, Object> subscribe(SubscriptionRequest request) {
//         try {
//             log.info("Received subscription request for {} instruments", request.getInstruments().size());
            
//             MarketDataProvider provider = providerFactory.getProvider();
            
//             // Create a tick listener that forwards data to WebSocket clients
//             Object tickListener = createTickListener(request.getSessionId());
            
//             // Initialize ticker with the instruments
//             Object ticker = provider.initializeTicker(request.getInstruments(), tickListener);
            
//             // Store the ticker session for later management
//             activeTickerSessions.put(request.getSessionId(), ticker);
            
//             return Map.of(
//                 "status", "subscribed",
//                 "sessionId", request.getSessionId(),
//                 "instrumentCount", request.getInstruments().size(),
//                 "provider", provider.getProviderName()
//             );
//         } catch (Exception e) {
//             log.error("Error subscribing to market data: {}", e.getMessage(), e);
//             return Map.of(
//                 "status", "error",
//                 "message", e.getMessage(),
//                 "sessionId", request.getSessionId()
//             );
//         }
//     }

//     /**
//      * Unsubscribe from market data
//      * @param request Unsubscription request
//      * @return Unsubscription status
//      */
//     @MessageMapping("/market-data/unsubscribe")
//     @SendTo("/topic/market-data/status")
//     public Map<String, Object> unsubscribe(UnsubscriptionRequest request) {
//         try {
//             log.info("Received unsubscription request for session {}", request.getSessionId());
            
//             // Remove the ticker session
//             Object ticker = activeTickerSessions.remove(request.getSessionId());
            
//             if (ticker != null) {
//                 // Implement provider-specific cleanup if needed
//                 return Map.of(
//                     "status", "unsubscribed",
//                     "sessionId", request.getSessionId()
//                 );
//             } else {
//                 return Map.of(
//                     "status", "not_found",
//                     "sessionId", request.getSessionId()
//                 );
//             }
//         } catch (Exception e) {
//             log.error("Error unsubscribing from market data: {}", e.getMessage(), e);
//             return Map.of(
//                 "status", "error",
//                 "message", e.getMessage(),
//                 "sessionId", request.getSessionId()
//             );
//         }
//     }

//     /**
//      * Create a tick listener based on the provider
//      * @param sessionId Session ID for routing
//      * @return Provider-specific tick listener
//      */
//     private Object createTickListener(String sessionId) {
//         MarketDataProvider provider = providerFactory.getProvider();
//         String providerName = provider.getProviderName();
        
//         // Create appropriate listener based on provider
//         if ("zerodha".equals(providerName)) {
//             return createZerodhaTickListener(sessionId);
//         } else {
//             throw new UnsupportedOperationException("Provider not supported for WebSocket: " + providerName);
//         }
//     }

//     /**
//      * Create a Zerodha-specific tick listener
//      * @param sessionId Session ID for routing
//      * @return Zerodha tick listener
//      */
//     private Object createZerodhaTickListener(String sessionId) {
//         return (com.zerodhatech.ticker.OnTicks) ticks -> {
//             // Forward ticks to WebSocket clients
//             messagingTemplate.convertAndSend(
//                 "/topic/market-data/ticks/" + sessionId,
//                 Map.of("ticks", ticks)
//             );
//         };
//     }

//     /**
//      * Subscription request model
//      */
//     public static class SubscriptionRequest {
//         private String sessionId;
//         private List<String> instruments;

//         public String getSessionId() {
//             return sessionId;
//         }

//         public void setSessionId(String sessionId) {
//             this.sessionId = sessionId;
//         }

//         public List<String> getInstruments() {
//             return instruments;
//         }

//         public void setInstruments(List<String> instruments) {
//             this.instruments = instruments;
//         }
//     }

//     /**
//      * Unsubscription request model
//      */
//     public static class UnsubscriptionRequest {
//         private String sessionId;

//         public String getSessionId() {
//             return sessionId;
//         }

//         public void setSessionId(String sessionId) {
//             this.sessionId = sessionId;
//         }
//     }
// }
