/**
 * websocket.js - WebSocket менеджер для real-time обновлений
 */

class WebSocketManager {
    constructor() {
        this.socket = null;
        this.reconnectTimeout = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 10;
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

                // Сбрасываем счетчик попыток
                this.reconnectAttempts = 0;

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

                // Переподключение с exponential backoff
                if (this.reconnectAttempts < this.maxReconnectAttempts) {
                    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
                    this.reconnectAttempts++;

                    console.log(`🔄 Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);

                    this.reconnectTimeout = setTimeout(() => this.connect(), delay);
                } else {
                    console.error('❌ Max reconnection attempts reached');
                    UI.showNotification('⚠️ WebSocket connection lost. Please refresh the page.', 'error');
                }
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
     * ✅ Обработка WebSocket сообщений (с буферизацией)
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

            // ✅ Обрабатываем обновление бронирования
            if (message.type === 'BOOKING_UPDATE') {
                // Добавляем timestamp если его нет
                if (!message.timestamp) {
                    message.timestamp = Date.now();
                }

                // ✅ Пробуем обработать сообщение
                const processed = handlePriceUpdate(message);

                // ✅ Если не обработалось (STATE.currentBooking еще не установлен) - БУФЕРИЗИРУЕМ
                if (!processed) {
                    console.warn('⚠️ Booking not ready yet, buffering message:', message.bookingId);

                    // Добавляем в буфер
                    STATE.pendingWebSocketMessages.push(message);

                    // ✅ Очищаем старые сообщения (>5 минут)
                    const now = Date.now();
                    STATE.pendingWebSocketMessages = STATE.pendingWebSocketMessages.filter(msg => {
                        const age = now - (msg.timestamp || now);
                        if (age > 300000) {
                            console.warn('🗑️ Removing old buffered message:', msg.bookingId, 'age:', age, 'ms');
                            return false;
                        }
                        return true;
                    });

                    console.log(`📦 Buffer size: ${STATE.pendingWebSocketMessages.length}`);
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
        console.log('🔌 Disconnecting WebSocket...');

        if (this.reconnectTimeout) {
            clearTimeout(this.reconnectTimeout);
            this.reconnectTimeout = null;
        }

        if (this.socket) {
            this.socket.close();
            this.socket = null;
        }

        STATE.wsConnected = false;
        UI.updateWebSocketStatus(false);
    }
}

// Глобальный инстанс
const WS = new WebSocketManager();
