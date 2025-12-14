/**
 * app.js - Инициализация приложения
 */

document.addEventListener('DOMContentLoaded', () => {
    console.log('🚀 Application starting...');

    // Инициализировать WebSocket и userId
    WS.initialize();

    // Инициализировать форму
    Bookings.setupFormHandler();

    // Установить значения дат по умолчанию
    UI.setupDateDefaults();

    console.log('✅ Application initialized');
});