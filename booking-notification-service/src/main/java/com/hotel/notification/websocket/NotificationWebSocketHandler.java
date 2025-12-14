package com.hotel.notification.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);

    private final Map<String, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = extractUserId(session);

        if (userId == null || userId.isEmpty()) {
            userId = "session_" + session.getId();
            log.warn(" Подключение без userId, используем sessionId: {}", userId);
        }

        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(session);

        session.getAttributes().put("userId", userId);

        log.info(" WebSocket подключение: userId={}, sessionId={}, активных пользователей: {}",
                userId, session.getId(), userSessions.size());

        sendMessage(session, "Подключено к системе уведомлений отеля");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        log.debug(" Сообщение от {}: {}", session.getId(), payload);

        if ("PING".equals(payload)) {
            sendMessage(session, "PONG");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get("userId");

        if (userId != null) {
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
        }

        log.info(" Отключение: userId={}, причина={}, активных пользователей: {}",
                userId, status.getReason(), userSessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error(" Ошибка транспорта для сессии {}", session.getId());

        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
        }
    }

    public boolean sendToUser(String userId, String message) {
        Set<WebSocketSession> sessions = userSessions.get(userId);

        if (sessions == null || sessions.isEmpty()) {
            log.debug(" Пользователь {} не подключен к WebSocket", userId);
            return false;
        }

        TextMessage textMessage = new TextMessage(message);
        int sent = 0;

        for (WebSocketSession session : sessions) {
            if (sendMessage(session, textMessage)) {
                sent++;
            }
        }

        log.info(" WebSocket сообщение отправлено userId={} в {} сессий", userId, sent);
        return sent > 0;
    }

    public int broadcast(String message) {
        TextMessage textMessage = new TextMessage(message);
        int sent = 0;

        for (Set<WebSocketSession> sessions : userSessions.values()) {
            for (WebSocketSession session : sessions) {
                if (sendMessage(session, textMessage)) {
                    sent++;
                }
            }
        }

        log.info(" Broadcast: отправлено {} сессиям", sent);
        return sent;
    }

    private boolean sendMessage(WebSocketSession session, String message) {
        return sendMessage(session, new TextMessage(message));
    }

    private boolean sendMessage(WebSocketSession session, TextMessage message) {
        if (!session.isOpen()) {
            return false;
        }

        try {
            synchronized (session) {
                session.sendMessage(message);
            }
            return true;
        } catch (IOException e) {
            log.warn(" Ошибка отправки в сессию {}: {}", session.getId(), e.getMessage());
            return false;
        }
    }

    private String extractUserId(WebSocketSession session) {
        String query = Objects.requireNonNull(session.getUri()).getQuery();
        if (query != null && !query.isEmpty()) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("userId=")) {
                    try {
                        String userId = java.net.URLDecoder.decode(
                                param.substring("userId=".length()).trim(),
                                StandardCharsets.UTF_8
                        );
                        if (!userId.isEmpty()) {
                            return userId;
                        }
                    } catch (Exception e) {
                        log.warn(" Ошибка декодирования userId", e);
                    }
                }
            }
        }
        return null;
    }

    public int getActiveUsers() {
        return userSessions.size();
    }

    public int getTotalSessions() {
        return userSessions.values().stream()
                .mapToInt(Set::size)
                .sum();
    }
}