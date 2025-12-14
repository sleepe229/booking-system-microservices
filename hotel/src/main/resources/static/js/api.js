/**
 * api.js - API клиент для работы с сервером
 */

const API = {
    /**
     * Создать бронирование
     */
    async createBooking(booking) {
        const response = await fetch(`${CONFIG.GATEWAY_URL}/bookings`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(booking)
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        return await response.json();
    },

    /**
     * Получить бронирование по ID
     */
    async getBooking(bookingId) {
        const response = await fetch(`${CONFIG.GATEWAY_URL}/bookings/${bookingId}`);

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        return await response.json();
    },

    /**
     * Получить список бронирований
     */
    async listBookings(page = 0, size = 10) {
        const response = await fetch(`${CONFIG.GATEWAY_URL}/bookings?page=${page}&size=${size}`);

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        return await response.json();
    },

    /**
     * Отменить бронирование
     */
    async cancelBooking(bookingId) {
        const response = await fetch(`${CONFIG.GATEWAY_URL}/bookings/cancel`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ bookingId })
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        return await response.json();
    }
};