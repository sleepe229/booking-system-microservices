/**
 * ui.js - Утилиты для UI
 */

const UI = {
    /**
     * Обновление статуса WebSocket
     */
    updateWebSocketStatus(connected) {
        const wsStatus = document.getElementById('wsStatus');
        const span = wsStatus.querySelector('span');

        if (connected) {
            wsStatus.classList.add('connected');
            span.textContent = '✅ Connected';
        } else {
            wsStatus.classList.remove('connected');
            span.textContent = '⏳ Connecting...';
        }
    },

    /**
     * ✅ Показать toast уведомление
     */
    showNotification(message, type = 'info') {
        // Удаляем старые уведомления
        const existing = document.querySelector('.toast-notification');
        if (existing) {
            existing.remove();
        }

        const toast = document.createElement('div');
        toast.className = `toast-notification toast-${type}`;
        toast.textContent = message;

        document.body.appendChild(toast);

        // Показываем с анимацией
        setTimeout(() => toast.classList.add('show'), 10);

        // Убираем через 4 секунды
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        }, 4000);
    },

    /**
     * Установка дат по умолчанию
     */
    setupDateDefaults() {
        const today = new Date();
        const tomorrow = new Date(today);
        tomorrow.setDate(tomorrow.getDate() + 1);

        const formatDate = (date) => date.toISOString().split('T')[0];

        document.getElementById('searchCheckIn').value = formatDate(today);
        document.getElementById('searchCheckOut').value = formatDate(tomorrow);

        STATE.searchParams.checkIn = formatDate(today);
        STATE.searchParams.checkOut = formatDate(tomorrow);
    }
};
