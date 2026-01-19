package com.hotel.notification.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);
    private static final String REDIS_WS_PREFIX = "ws:user:";
    private static final String REDIS_BROADCAST_CHANNEL = "ws:broadcast"; // ✅ добавили

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StringRedisTemplate redisTemplate;
    private final RedisMessageListenerContainer listenerContainer;

    private final Map<String, Set<WebSocketSession>> localSessions = new ConcurrentHashMap<>();
    private final Set<String> activeSubscriptions = ConcurrentHashMap.newKeySet();

    public NotificationWebSocketHandler(
            StringRedisTemplate redisTemplate,
            RedisMessageListenerContainer listenerContainer) {
        this.redisTemplate = redisTemplate;
        this.listenerContainer = listenerContainer;

        // ✅ Подписываемся на broadcast канал при инициализации
        subscribeToBroadcastChannel();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = extractUserId(session);
        if (userId == null || userId.isEmpty()) {
            log.warn("⚠️ Нет userId в WS подключении, закрываем: {}", session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        localSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(session);
        session.getAttributes().put("userId", userId);

        subscribeToUserChannel(userId);

        log.info("✅ WS connected: userId={}, sessionId={}, localTotal={}",
                userId, session.getId(), localSessions.get(userId).size());

        sendConnectionMessage(session, userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            Set<WebSocketSession> sessions = localSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    localSessions.remove(userId);
                    unsubscribeFromUserChannel(userId);
                }
            }
        }
        log.info("❌ WS disconnected: userId={}, reason={}", userId, status.getReason());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        if ("PING".equals(payload)) {
            sendMessage(session, new TextMessage("PONG"));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("⚠️ WS transport error: sessionId={}", session.getId(), exception);
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            cleanupSession(userId, session);
        }
    }

    /**
     * Отправка сообщения конкретному пользователю через Redis Pub/Sub
     */
    public boolean sendToUser(String userId, String message) {
        String channel = REDIS_WS_PREFIX + userId;

        try {
            redisTemplate.convertAndSend(channel, message);
            log.debug("📤 Published to Redis: userId={}, channel={}", userId, channel);
            return true;
        } catch (Exception e) {
            log.error("❌ Redis publish failed: userId={}", userId, e);
            return false;
        }
    }

    /**
     * ✅ Broadcast сообщения всем подключенным пользователям через Redis
     */
    public void broadcast(String message) {
        try {
            redisTemplate.convertAndSend(REDIS_BROADCAST_CHANNEL, message);
            log.info("📢 Broadcasted to Redis: {} users", localSessions.size());
        } catch (Exception e) {
            log.error("❌ Broadcast failed", e);
        }
    }

    /**
     * ✅ Broadcast объекта (автоматическая сериализация в JSON)
     */
    public void broadcastObject(Object message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            broadcast(json);
        } catch (Exception e) {
            log.error("❌ Failed to serialize broadcast message", e);
        }
    }

    /**
     * ✅ Подписка на broadcast канал (для всех пользователей)
     */
    private void subscribeToBroadcastChannel() {
        ChannelTopic topic = new ChannelTopic(REDIS_BROADCAST_CHANNEL);

        listenerContainer.addMessageListener((message, pattern) -> {
            String msg = new String(message.getBody());
            log.debug("📨 Received broadcast from Redis: {}", msg);

            TextMessage textMessage = new TextMessage(msg);
            int sent = 0;
            int total = 0;

            // Отправляем всем локальным сессиям
            for (Set<WebSocketSession> sessions : localSessions.values()) {
                for (WebSocketSession session : sessions) {
                    total++;
                    if (sendMessage(session, textMessage)) {
                        sent++;
                    }
                }
            }

            log.info("📢 Broadcast delivered: {}/{} sessions", sent, total);
        }, topic);

        log.info("✅ Subscribed to broadcast channel: {}", REDIS_BROADCAST_CHANNEL);
    }

    /**
     * Подписка на персональный канал пользователя
     */
    private void subscribeToUserChannel(String userId) {
        String channel = REDIS_WS_PREFIX + userId;

        if (!activeSubscriptions.contains(channel)) {
            ChannelTopic topic = new ChannelTopic(channel);

            listenerContainer.addMessageListener((message, pattern) -> {
                String msg = new String(message.getBody());
                log.debug("📨 Received from Redis: channel={}, message={}", channel, msg);

                Set<WebSocketSession> sessions = localSessions.get(userId);
                if (sessions != null && !sessions.isEmpty()) {
                    TextMessage textMessage = new TextMessage(msg);
                    int sent = 0;
                    for (WebSocketSession session : sessions) {
                        if (sendMessage(session, textMessage)) {
                            sent++;
                        }
                    }
                    log.info("📤 Delivered locally: userId={}, sessions={}/{}",
                            userId, sent, sessions.size());
                }
            }, topic);

            activeSubscriptions.add(channel);
            log.info("✅ Subscribed to Redis channel: {}", channel);
        }
    }

    /**
     * Отписка от персонального канала
     */
    private void unsubscribeFromUserChannel(String userId) {
        String channel = REDIS_WS_PREFIX + userId;
        activeSubscriptions.remove(channel);
        log.info("❌ Unsubscribed from channel: {}", channel);
    }

    /**
     * Отправка сообщения в WebSocket сессию
     */
    private boolean sendMessage(WebSocketSession session, TextMessage message) {
        if (!session.isOpen()) return false;
        try {
            synchronized (session) {
                session.sendMessage(message);
            }
            return true;
        } catch (Exception e) {
            log.warn("⚠️ Send failed: sessionId={}", session.getId());
            return false;
        }
    }

    /**
     * Отправка приветственного сообщения
     */
    private void sendConnectionMessage(WebSocketSession session, String userId) throws Exception {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "CONNECTED");
        msg.put("userId", userId);
        msg.put("message", "Successfully connected to WebSocket");
        msg.put("timestamp", System.currentTimeMillis());

        String json = objectMapper.writeValueAsString(msg);
        sendMessage(session, new TextMessage(json));
    }

    /**
     * Очистка сессии при ошибке
     */
    private void cleanupSession(String userId, WebSocketSession session) {
        Set<WebSocketSession> sessions = localSessions.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                localSessions.remove(userId);
                unsubscribeFromUserChannel(userId);
            }
        }
    }

    /**
     * Извлечение userId из query параметров
     */
    private String extractUserId(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && !query.isEmpty()) {
            for (String param : query.split("&")) {
                if (param.startsWith("userId=")) {
                    try {
                        String userId = java.net.URLDecoder.decode(
                                param.substring("userId=".length()).trim(),
                                java.nio.charset.StandardCharsets.UTF_8
                        );
                        if (!userId.isEmpty()) return userId;
                    } catch (Exception e) {
                        log.warn("⚠️ Failed to decode userId", e);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Получить количество активных пользователей
     */
    public int getActiveUsers() {
        return localSessions.size();
    }

    /**
     * Получить общее количество сессий
     */
    public int getTotalSessions() {
        return localSessions.values().stream()
                .mapToInt(Set::size)
                .sum();
    }

    /**
     * ✅ Получить список активных пользователей (для stats API)
     */
    public Set<String> getActiveUserIds() {
        return localSessions.keySet();
    }
}
