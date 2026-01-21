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

// ✅ Глобальное состояние (ЕДИНСТВЕННОЕ объявление STATE во всем приложении)
const STATE = {
    // User & WebSocket
    userId: null,
    wsConnected: false,

    // Current booking flow
    currentBooking: null,
    selectedHotel: null,

    // Search & Hotels
    searchParams: {
        city: 'Moscow',
        checkIn: null,
        checkOut: null,
        guests: 2
    },
    hotels: [],

    // UI State
    currentMode: 'booking',

    // ✅ WebSocket buffering (защита от race condition)
    pendingWebSocketMessages: [],

    // ✅ Таймеры для управления ожиданием
    warningTimeout: null,
    fallbackTimeout: null,
    pollingInterval: null
};

console.log('✅ Config loaded');
