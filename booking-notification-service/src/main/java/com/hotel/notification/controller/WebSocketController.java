package com.hotel.notification.controller;

import com.hotel.notification.websocket.NotificationWebSocketHandler;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class WebSocketController {

    private final NotificationWebSocketHandler handler;

    public WebSocketController(NotificationWebSocketHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/broadcast")
    public Map<String, Object> broadcast(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        handler.broadcast(message);

        return Map.of(
                "status", "success",
                "message", "Broadcasted to all users",
                "activeUsers", handler.getActiveUsers(),
                "totalSessions", handler.getTotalSessions()
        );
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return Map.of(
                "activeUsers", handler.getActiveUsers(),
                "totalSessions", handler.getTotalSessions(),
                "userIds", handler.getActiveUserIds()
        );
    }
}
