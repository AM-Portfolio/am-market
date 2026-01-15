package com.am.marketdata.api.websocket;

import com.am.marketdata.common.model.MarketDataUpdate;
import com.am.marketdata.common.service.MarketDataPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class MarketDataWebSocketHandler extends TextWebSocketHandler implements MarketDataPublisher {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void publish(MarketDataUpdate update) {
        broadcast(update);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("New WebSocket connection established: {} | URI: {} | Attributes: {}",
                session.getId(), session.getUri(), session.getAttributes());
        sessions.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("Received message from session {}: {}", session.getId(), payload);

        // Basic echo or command handling if needed (currently minimal)
        if ("ping".equalsIgnoreCase(payload)) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket connection closed: {} | Code: {} | Reason: {}",
                session.getId(), status.getCode(), status.getReason());
        sessions.remove(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session: {} | Error: {}", session.getId(), exception.getMessage());
        super.handleTransportError(session, exception);
    }

    public void broadcast(MarketDataUpdate update) {
        if (sessions.isEmpty())
            return;

        try {
            String message = objectMapper.writeValueAsString(update);
            TextMessage textMessage = new TextMessage(message);

            log.debug("Broadcasting update to {} sessions | Payload Size: {} bytes | Quotes: {}",
                    sessions.size(), message.length(), update.getQuotes() != null ? update.getQuotes().size() : 0);

            for (WebSocketSession session : sessions) {
                try {
                    if (session.isOpen()) {
                        log.trace("Sending to session: {}", session.getId());
                        session.sendMessage(textMessage);
                    } else {
                        log.warn("Session {} is closed, stripping from active list pending cleanup", session.getId());
                    }
                } catch (IOException e) {
                    log.error("Error sending message to session {}", session.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error broadcasting message", e);
        }
    }
}
