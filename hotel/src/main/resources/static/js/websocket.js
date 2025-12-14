/**
 * websocket.js - Управление WebSocket подключением
 */

class WebSocketManager {
    constructor() {
        this.socket = null;
    }

    /**
     * Инициализировать userId и подключиться к WebSocket
     */
    initialize() {
        this.initializeUserId();
        this.connect();
    }

    /**
     * Генерирование или восстановление userId
     */
    initializeUserId() {
        let userId = localStorage.getItem('bookingUserId');
        if (!userId) {
            userId = `user_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
            localStorage.setItem('bookingUserId', userId);
        }
        STATE.userId = userId;
        console.log('✅ UserId initialized:', userId);
    }

    /**
     * Подключиться к WebSocket
     */
    connect() {
        const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsUrl = `${protocol}//${CONFIG.NOTIFICATION_HOST}:${CONFIG.NOTIFICATION_PORT}/ws/notifications?userId=${STATE.userId}`;

        console.log('🔌 Connecting to WebSocket:', wsUrl);

        try {
            this.socket = new WebSocket(wsUrl);

            this.socket.onopen = () => {
                STATE.wsConnected = true;
                UI.updateWebSocketStatus(true);
                console.log('✅ WebSocket connected');
            };

            this.socket.onmessage = (event) => {
                this.handleMessage(event.data);
            };

            this.socket.onclose = () => {
                STATE.wsConnected = false;
                UI.updateWebSocketStatus(false);
                console.log('❌ WebSocket disconnected');
                // Переподключение через 5 секунд
                setTimeout(() => this.connect(), 5000);
            };

            this.socket.onerror = (error) => {
                console.error('❌ WebSocket error:', error);
                UI.updateWebSocketStatus(false);
            };
        } catch (e) {
            console.error('❌ WebSocket initialization error:', e);
            UI.updateWebSocketStatus(false);
        }
    }

    /**
     * Обработка входящего WebSocket сообщения
     */
    handleMessage(data) {
        try {
            const message = JSON.parse(data);
            console.log('📨 WebSocket message received:', message);

            // Проверяем что это сообщение для нашего бронирования
            if (message.type === 'BOOKING_UPDATE' && message.bookingId === STATE.pendingBookingId) {
                console.log('✅ Получено обновление для нашего бронирования');
                UI.displayBookingResult(message);
            }
        } catch (e) {
            console.error('❌ Error parsing WebSocket message:', e);
        }
    }

    /**
     * Отключиться от WebSocket
     */
    disconnect() {
        if (this.socket) {
            this.socket.close();
            this.socket = null;
        }
    }
}

// Инстанс WebSocket менеджера
const WS = new WebSocketManager();