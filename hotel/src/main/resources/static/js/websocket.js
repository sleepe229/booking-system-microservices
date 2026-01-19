/**
 * websocket.js - WebSocket менеджер для real-time обновлений
 */

class WebSocketManager {
    constructor() {
        this.socket = null;
        this.reconnectTimeout = null;
    }

    /**
     * Инициализация userId и подключение
     */
    initialize() {
        this.initializeUserId();
        this.connect();
    }

    /**
     * Генерация или восстановление userId
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
     * Подключение к WebSocket
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

                // Очищаем таймер переподключения
                if (this.reconnectTimeout) {
                    clearTimeout(this.reconnectTimeout);
                    this.reconnectTimeout = null;
                }
            };

            this.socket.onmessage = (event) => {
                this.handleMessage(event.data);
            };

            this.socket.onclose = () => {
                STATE.wsConnected = false;
                UI.updateWebSocketStatus(false);
                console.log('❌ WebSocket disconnected');

                // Переподключение через 5 секунд
                this.reconnectTimeout = setTimeout(() => this.connect(), 5000);
            };

            this.socket.onerror = (error) => {
                console.error('❌ WebSocket error:', error);
                UI.updateWebSocketStatus(false);
            };
        } catch (e) {
            console.error('❌ WebSocket initialization error:', e);
            UI.updateWebSocketStatus(false);

            // Переподключение через 5 секунд
            this.reconnectTimeout = setTimeout(() => this.connect(), 5000);
        }
    }

    /**
     * ✅ Обработка WebSocket сообщений
     */
    handleMessage(data) {
        try {
            const message = JSON.parse(data);
            console.log('📨 WebSocket message received:', message);

            // Игнорируем сообщение о подключении
            if (message.type === 'CONNECTED') {
                console.log('✅ WebSocket handshake complete');
                return;
            }

            // ✅ КЛЮЧЕВОЕ: обрабатываем обновление цены
            if (message.type === 'BOOKING_UPDATE') {
                if (STATE.currentBooking && message.bookingId === STATE.currentBooking.bookingId) {
                    console.log('💰 Получено обновление цены для бронирования:', message.bookingId);
                    handlePriceUpdate(message);
                }
            }
        } catch (e) {
            console.error('❌ Error parsing WebSocket message:', e);
        }
    }

    /**
     * Отключение
     */
    disconnect() {
        if (this.reconnectTimeout) {
            clearTimeout(this.reconnectTimeout);
            this.reconnectTimeout = null;
        }

        if (this.socket) {
            this.socket.close();
            this.socket = null;
        }
    }
}

// Глобальный инстанс
const WS = new WebSocketManager();
