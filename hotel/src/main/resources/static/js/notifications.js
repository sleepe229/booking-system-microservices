/**
 * Notifications Module - WebSocket уведомления
 */

let wsManager = null;

class WebSocketManager {
    constructor() {
        this.socket = null;
        this.userId = null;
        this.statusDiv = document.getElementById('wsStatus');
        this.notificationsList = document.getElementById('notificationsList');
    }

    connect(userId) {
        this.userId = userId || 'anonymous';
        const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';

        // ✅ ИСПРАВЛЕНО: Используем динамический host вместо hardcoded localhost
        const host = window.location.hostname;
        const port = 8085;
        const url = userId
            ? `${protocol}//${host}:${port}/ws/notifications?userId=${userId}`
            : `${protocol}//${host}:${port}/ws/notifications`;

        console.log('🔌 Connecting to WebSocket:', url);
        this.socket = new WebSocket(url);

        this.socket.onopen = () => {
            this.updateStatus('Connected', 'connected', `User: ${this.userId}`);
            this.loadStats();
            setInterval(() => this.loadStats(), 5000);
        };

        this.socket.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                this.displayNotification(data);
            } catch (e) {
                console.error('Parse error:', e);
            }
        };

        this.socket.onclose = () => {
            this.updateStatus('Disconnected', 'disconnected');
        };

        this.socket.onerror = (error) => {
            console.error('WebSocket error:', error);
            this.updateStatus('Error', 'disconnected');
        };
    }

    disconnect() {
        if (this.socket) {
            this.socket.close();
            this.socket = null;
        }
    }

    updateStatus(text, status, info = '') {
        this.statusDiv.textContent = `Status: ${text} ${info}`;
        this.statusDiv.className = `status-box ${status}`;
    }

    displayNotification(data) {
        const item = document.createElement('div');
        item.className = `notification-item ${data.status?.toLowerCase() || ''}`;
        item.innerHTML = `
            <div class="notification-timestamp">${new Date(data.timestamp).toLocaleString()}</div>
            <div class="notification-message">${data.message}</div>
            <div class="notification-details">
                <p>Booking ID: ${data.bookingId}</p>
                <p>Hotel ID: ${data.hotelId}</p>
                <p>Final Price: $${data.finalPrice || 0}</p>
                ${data.discountPercentage ? `<p>Discount: ${data.discountPercentage}%</p>` : ''}
            </div>
        `;
        this.notificationsList.insertBefore(item, this.notificationsList.firstChild);
    }

    async loadStats() {
        try {
            const stats = await NOTIFICATION_API.stats();
            document.getElementById('wsStats').innerHTML = `
                <div class="stat-item">
                    <div class="stat-item-value">${stats.activeUsers || 0}</div>
                    <div class="stat-item-label">Active Users</div>
                </div>
                <div class="stat-item">
                    <div class="stat-item-value">${stats.totalSessions || 0}</div>
                    <div class="stat-item-label">Total Sessions</div>
                </div>
            `;
        } catch (e) {
            console.error('Error loading stats:', e);
            // ✅ Не падаем при ошибке
        }
    }
}

function connectWebSocket() {
    const userId = document.getElementById('userId').value.trim();
    if (!wsManager) {
        wsManager = new WebSocketManager();
    }
    wsManager.connect(userId);
}

function disconnectWebSocket() {
    if (wsManager) {
        wsManager.disconnect();
    }
}

async function broadcastMessage() {
    const message = document.getElementById('broadcastMsg').value.trim();
    if (!message) {
        alert('Please enter a message');
        return;
    }

    try {
        await NOTIFICATION_API.broadcast({ message });
        alert('✅ Message broadcast sent!');
        document.getElementById('broadcastMsg').value = '';
    } catch (e) {
        alert('❌ Error: ' + e.message);
    }
}