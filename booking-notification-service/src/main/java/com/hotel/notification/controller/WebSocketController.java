package com.hotel.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.notification.websocket.NotificationWebSocketHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/websocket")
public class WebSocketController {

    private final NotificationWebSocketHandler handler;

    public WebSocketController(NotificationWebSocketHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, Object>> broadcast(@RequestBody Map<String, Object> payload) {
        try {
            String message = new ObjectMapper().writeValueAsString(payload);
            int sent = handler.broadcast(message);
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "sentTo", sent,
                    "message", message
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/send/{userId}")
    public ResponseEntity<Map<String, Object>> sendToUser(
            @PathVariable String userId,
            @RequestBody Map<String, Object> payload) {
        try {
            String message = new ObjectMapper().writeValueAsString(payload);
            boolean sent = handler.sendToUser(userId, message);
            return ResponseEntity.ok(Map.of(
                    "status", sent ? "ok" : "user_not_connected",
                    "userId", userId,
                    "delivered", sent,
                    "message", message
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Статистика подключений
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(Map.of(
                "activeUsers", handler.getActiveUsers(),
                "totalSessions", handler.getTotalSessions()
        ));
    }
}