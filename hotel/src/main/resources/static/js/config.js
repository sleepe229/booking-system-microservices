/**
 * config.js - Конфигурация приложения
 */

const CONFIG = {
    // Hotel Service (REST API)
    GATEWAY_URL: `${window.location.protocol}//${window.location.hostname}:8089/api`,

    // WebSocket для real-time обновлений цен
    NOTIFICATION_HOST: window.location.hostname,
    NOTIFICATION_PORT: 8085,

    // Audit Service (если нужно)
    AUDIT_URL: `${window.location.protocol}//${window.location.hostname}:8082/api/audit`
};

// Глобальное состояние
const STATE = {
    userId: null,
    wsConnected: false,
    currentBooking: null,
    selectedHotel: null,
    currentMode: 'booking',
    searchParams: {
        city: 'Moscow',
        checkIn: null,
        checkOut: null,
        guests: 2
    },
    hotels: []
};
