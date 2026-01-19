/**
 * audit.js - Audit Dashboard
 */

const AUDIT_API = {
    /**
     * Получить логи с фильтрами
     */
    async getLogs(filters = {}) {
        const params = new URLSearchParams();

        if (filters.bookingId) params.append('bookingId', filters.bookingId);
        if (filters.eventType) params.append('eventType', filters.eventType);

        const url = `${CONFIG.AUDIT_URL}/logs${params.toString() ? '?' + params.toString() : ''}`;

        const response = await fetch(url);
        if (!response.ok) {
            throw new Error('Failed to load audit logs');
        }

        return response.json();
    },

    /**
     * Экспорт в CSV
     */
    exportCSV(filters = {}) {
        const params = new URLSearchParams();

        if (filters.startDate) params.append('startDate', filters.startDate);
        if (filters.endDate) params.append('endDate', filters.endDate);
        if (filters.eventType) params.append('eventType', filters.eventType);

        return `${CONFIG.AUDIT_URL}/export/csv${params.toString() ? '?' + params.toString() : ''}`;
    }
};

/**
 * Переключение между режимами
 */
function toggleMode() {
    const bookingScreen = document.getElementById('bookingScreen');
    const auditScreen = document.getElementById('auditScreen');
    const switchBtn = document.getElementById('switchToAudit');

    if (STATE.currentMode === 'booking') {
        // Переключаемся на аудит
        bookingScreen.classList.remove('active');
        auditScreen.classList.add('active');
        switchBtn.innerHTML = '<span class="mode-icon">🏨</span><span class="mode-text">Hotels</span>';
        switchBtn.title = 'Switch to Booking';
        STATE.currentMode = 'audit';

        // Загружаем логи
        loadAuditLogs();
    } else {
        // Переключаемся обратно на бронирование
        auditScreen.classList.remove('active');
        bookingScreen.classList.add('active');
        switchBtn.innerHTML = '<span class="mode-icon">📊</span><span class="mode-text">Audit</span>';
        switchBtn.title = 'Switch to Audit Dashboard';
        STATE.currentMode = 'booking';
    }
}

/**
 * Загрузка логов аудита
 */
async function loadAuditLogs() {
    const loadingEl = document.getElementById('auditLoading');
    const listEl = document.getElementById('auditList');

    loadingEl.style.display = 'block';
    listEl.innerHTML = '';

    try {
        const filters = {
            eventType: document.getElementById('auditEventType').value || null,
            bookingId: document.getElementById('auditBookingId').value || null
        };

        const logs = await AUDIT_API.getLogs(filters);

        loadingEl.style.display = 'none';
        displayAuditLogs(logs);
        updateAuditStats(logs);

    } catch (error) {
        console.error('❌ Error loading audit logs:', error);
        loadingEl.style.display = 'none';
        listEl.innerHTML = `
            <div class="error-state">
                <h3>❌ Failed to load audit logs</h3>
                <p>${error.message}</p>
            </div>
        `;
    }
}

/**
 * Отображение логов
 */
function displayAuditLogs(logs) {
    const container = document.getElementById('auditList');

    if (!logs || logs.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">📋</div>
                <h3>No audit logs found</h3>
                <p>Try adjusting your filters</p>
            </div>
        `;
        return;
    }

    let html = '<div class="audit-table-wrapper"><table class="audit-table">';
    html += `
        <thead>
            <tr>
                <th>ID</th>
                <th>Event Type</th>
                <th>Timestamp</th>
                <th>Booking ID</th>
                <th>Customer</th>
                <th>Details</th>
            </tr>
        </thead>
        <tbody>
    `;

    logs.forEach(log => {
        const eventClass = getEventClass(log.eventType);
        const eventIcon = getEventIcon(log.eventType);

        html += `
            <tr class="audit-row ${eventClass}">
                <td><code>${log.id}</code></td>
                <td>
                    <span class="event-badge ${eventClass}">
                        ${eventIcon} ${log.eventType}
                    </span>
                </td>
                <td>${formatTimestamp(log.timestamp)}</td>
                <td><code>${log.bookingId || '-'}</code></td>
                <td>${escapeHtml(log.customerEmail || '-')}</td>
                <td>
                    <button class="btn-details" onclick="showEventDetails(${log.id})">
                        👁️ View
                    </button>
                </td>
            </tr>
        `;
    });

    html += '</tbody></table></div>';
    container.innerHTML = html;

    // Сохраняем логи для просмотра деталей
    window.auditLogsCache = logs;
}

/**
 * Обновление статистики
 */
function updateAuditStats(logs) {
    document.getElementById('statTotal').textContent = logs.length;

    const confirmed = logs.filter(l => l.eventType === 'BOOKING_CONFIRMED').length;
    const paid = logs.filter(l => l.eventType === 'BOOKING_PAID').length;
    const cancelled = logs.filter(l => l.eventType === 'BOOKING_CANCELLED').length;

    document.getElementById('statConfirmed').textContent = confirmed;
    document.getElementById('statPaid').textContent = paid;
    document.getElementById('statCancelled').textContent = cancelled;
}

/**
 * Показать детали события
 */
function showEventDetails(logId) {
    const log = window.auditLogsCache?.find(l => l.id === logId);
    if (!log) return;

    const eventData = JSON.stringify(JSON.parse(log.eventData), null, 2);

    const modal = document.createElement('div');
    modal.className = 'modal show';
    modal.innerHTML = `
        <div class="modal-content" style="max-width: 600px;">
            <span class="modal-close" onclick="this.parentElement.parentElement.remove()">&times;</span>
            <h2>Event Details</h2>
            
            <div style="margin: 1.5rem 0;">
                <p><strong>ID:</strong> ${log.id}</p>
                <p><strong>Type:</strong> <span class="event-badge ${getEventClass(log.eventType)}">${log.eventType}</span></p>
                <p><strong>Timestamp:</strong> ${formatTimestamp(log.timestamp)}</p>
                <p><strong>Booking ID:</strong> <code>${log.bookingId || '-'}</code></p>
                <p><strong>Customer:</strong> ${escapeHtml(log.customerEmail || '-')}</p>
            </div>
            
            <h3>Event Data:</h3>
            <pre style="background: var(--bg); padding: 1rem; border-radius: 8px; overflow-x: auto; max-height: 400px;"><code>${escapeHtml(eventData)}</code></pre>
            
            <button onclick="this.parentElement.parentElement.remove()" class="btn-primary" style="margin-top: 1rem; width: 100%;">
                Close
            </button>
        </div>
    `;

    document.body.appendChild(modal);
}

/**
 * Экспорт в CSV
 */
function exportAuditCSV() {
    const startDate = document.getElementById('auditStartDate').value;
    const endDate = document.getElementById('auditEndDate').value;
    const eventType = document.getElementById('auditEventType').value;

    const url = AUDIT_API.exportCSV({
        startDate: startDate ? startDate + 'T00:00:00' : null,
        endDate: endDate ? endDate + 'T23:59:59' : null,
        eventType: eventType || null
    });

    window.open(url, '_blank');
}

/**
 * Экспорт в JSON
 */
async function exportAuditJSON() {
    const filters = {
        eventType: document.getElementById('auditEventType').value || null,
        bookingId: document.getElementById('auditBookingId').value || null
    };

    try {
        const logs = await AUDIT_API.getLogs(filters);
        const json = JSON.stringify(logs, null, 2);
        const blob = new Blob([json], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `audit_logs_${Date.now()}.json`;
        link.click();
        URL.revokeObjectURL(url);
    } catch (error) {
        alert('❌ Export failed: ' + error.message);
    }
}

/**
 * Очистить фильтры
 */
function clearAuditFilters() {
    document.getElementById('auditEventType').value = '';
    document.getElementById('auditStartDate').value = '';
    document.getElementById('auditEndDate').value = '';
    document.getElementById('auditBookingId').value = '';
    loadAuditLogs();
}

/**
 * Вспомогательные функции
 */
function getEventClass(eventType) {
    const map = {
        'BOOKING_CREATED': 'event-info',
        'BOOKING_CONFIRMED': 'event-success',
        'BOOKING_PAID': 'event-success',
        'BOOKING_CANCELLED': 'event-danger',
        'BOOKING_REJECTED': 'event-danger'
    };
    return map[eventType] || 'event-info';
}

function getEventIcon(eventType) {
    const map = {
        'BOOKING_CREATED': '📝',
        'BOOKING_CONFIRMED': '✅',
        'BOOKING_PAID': '💳',
        'BOOKING_CANCELLED': '❌',
        'BOOKING_REJECTED': '🚫'
    };
    return map[eventType] || '📋';
}

function formatTimestamp(timestamp) {
    return new Date(timestamp).toLocaleString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
