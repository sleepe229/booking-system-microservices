/**
 * API Module - общие функции для работы с API
 */

const API = {
    HOTEL_BASE_URL: window.location.origin + '/api',
    NOTIFICATION_BASE_URL: 'http://' + window.location.hostname + ':8085/api',

    async get(endpoint, baseUrl = this.HOTEL_BASE_URL) {
        try {
            const response = await fetch(baseUrl + endpoint);
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            return await response.json();
        } catch (e) {
            console.error('GET Error:', e);
            throw e;
        }
    },

    async post(endpoint, data, baseUrl = this.HOTEL_BASE_URL) {
        try {
            const response = await fetch(baseUrl + endpoint, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            return await response.json();
        } catch (e) {
            console.error('POST Error:', e);
            throw e;
        }
    }
};

const HOTELS_API = {
    searchHotels: () => API.get('/hotels/search'),
    createBooking: (data) => API.post('/bookings', data),
    cancelBooking: (data) => API.post('/bookings/cancel', data),
    getBooking: (id) => API.get(`/bookings/${id}`),
    listBookings: (page = 0, size = 10) => API.get(`/bookings?page=${page}&size=${size}`)
};

// ✅ ИСПРАВЛЕНО: Отдельный BASE_URL для notification-service на 8085
const NOTIFICATION_API = {
    broadcast: (data) => API.post('/websocket/broadcast', data, API.NOTIFICATION_BASE_URL),
    sendToUser: (userId, data) => API.post(`/websocket/send/${userId}`, data, API.NOTIFICATION_BASE_URL),
    stats: () => API.get('/websocket/stats', API.NOTIFICATION_BASE_URL)
};

const AUDIT_API = {
    getLogs: (filters = {}) => {
        let url = '/audit/logs';
        const params = new URLSearchParams();
        if (filters.bookingId) params.append('bookingId', filters.bookingId);
        if (filters.eventType) params.append('eventType', filters.eventType);
        if (params.toString()) url += '?' + params.toString();
        return API.get(url);
    },
    exportCSV: (filters = {}) => {
        let url = '/audit/export/csv';
        const params = new URLSearchParams();
        if (filters.startDate) params.append('startDate', filters.startDate);
        if (filters.endDate) params.append('endDate', filters.endDate);
        if (filters.eventType) params.append('eventType', filters.eventType);
        if (params.toString()) url += '?' + params.toString();
        return API.HOTEL_BASE_URL + url;
    }
};