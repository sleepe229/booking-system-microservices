/**
 * config.js - Конфигурация приложения
 *
 * ⚠️ ИЗМЕНИТЕ ЭТИ ЗНАЧЕНИЯ СОГЛАСНО ВАШЕЙ ИНФРАСТРУКТУРЕ!
 */

const CONFIG = {
    // 🏨 ГЕЙТВЕЙ (отели, бронирования)
    GATEWAY_URL: `${window.location.protocol}//${window.location.hostname}:8080/api`,

    // 🔔 NOTIFICATION SERVICE (WebSocket для цен в реал-тайм)
    NOTIFICATION_HOST: window.location.hostname,
    NOTIFICATION_PORT: 8085,

    // 📊 AUDIT SERVICE (аудит логи) - если понадобится в будущем
    AUDIT_URL: `${window.location.protocol}//${window.location.hostname}:8082/api`
};

/**
 * ЕСЛИ ВАША КОНФИГУРАЦИЯ ДРУГАЯ:
 *
 * Пример 1: Все сервисы на разных хостах
 *   GATEWAY_URL: 'http://gateway.example.com:8080/api',
 *   NOTIFICATION_HOST: 'notifications.example.com',
 *   NOTIFICATION_PORT: 8085,
 *   AUDIT_URL: 'http://audit.example.com:8082/api'
 *
 * Пример 2: Все за одним обратным прокси
 *   GATEWAY_URL: '/api/gateway',
 *   NOTIFICATION_HOST: window.location.hostname,
 *   NOTIFICATION_PORT: window.location.port,
 *   AUDIT_URL: '/api/audit'
 *
 * Пример 3: Docker на localhost с разными портами
 *   GATEWAY_URL: 'http://localhost:8080/api',
 *   NOTIFICATION_HOST: 'localhost',
 *   NOTIFICATION_PORT: 8085,
 *   AUDIT_URL: 'http://localhost:8082/api'
 */

// Глобальное состояние приложения
const STATE = {
    userId: null,
    pendingBookingId: null,
    wsConnected: false,
    socket: null,
    currentBooking: null
};
