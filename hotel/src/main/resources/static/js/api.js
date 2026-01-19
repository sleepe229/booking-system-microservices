/**
 * api.js - API клиент для работы с backend
 */

const API = {
    /**
     * Поиск отелей
     */
    async searchHotels(city, checkIn, checkOut, guests) {
        try {
            const response = await fetch(`${CONFIG.GATEWAY_URL}/hotels/search`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ city, checkIn, checkOut, guests })
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new ApiError(errorData, response.status);
            }

            const data = await response.json();
            return data._embedded?.hotelSearchResponseList || data.content || [];
        } catch (error) {
            if (error instanceof ApiError) {
                throw error;
            }
            throw new ApiError({
                status: 'error',
                message: 'Не удалось подключиться к серверу'
            }, 0);
        }
    },

    /**
     * Создать бронирование
     */
    async createBooking(bookingData) {
        try {
            const response = await fetch(`${CONFIG.GATEWAY_URL}/bookings`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(bookingData)
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new ApiError(errorData, response.status);
            }

            const data = await response.json();
            return data.content || data;
        } catch (error) {
            if (error instanceof ApiError) {
                throw error;
            }
            throw new ApiError({
                status: 'error',
                message: 'Не удалось создать бронирование'
            }, 0);
        }
    },

    /**
     * Получить бронирование по ID
     */
    async getBooking(bookingId) {
        try {
            const response = await fetch(`${CONFIG.GATEWAY_URL}/bookings/${bookingId}`);

            if (!response.ok) {
                const errorData = await response.json();
                throw new ApiError(errorData, response.status);
            }

            const data = await response.json();
            return data.content || data;
        } catch (error) {
            if (error instanceof ApiError) {
                throw error;
            }
            throw new ApiError({
                status: 'error',
                message: 'Не удалось загрузить бронирование'
            }, 0);
        }
    },


    /**
     * Оплатить бронирование
     */
    async payBooking(bookingId, paymentMethod) {
        try {
            const response = await fetch(`${CONFIG.GATEWAY_URL}/bookings/pay`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    bookingId,
                    paymentMethod,
                    cardNumber: '****-****-****-1234',  // Демо
                    cardHolder: 'John Doe'
                })
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new ApiError(errorData, response.status);
            }

            const data = await response.json();
            return data.content || data;
        } catch (error) {
            if (error instanceof ApiError) {
                throw error;
            }
            throw new ApiError({
                status: 'error',
                message: 'Не удалось обработать платеж'
            }, 0);
        }
    }

};

/**
 * Кастомный класс ошибки API
 */
class ApiError extends Error {
    constructor(errorData, statusCode) {
        super(errorData.message || 'Произошла ошибка');
        this.name = 'ApiError';
        this.statusCode = statusCode;
        this.errors = errorData.errors || {};
        this.data = errorData;
    }

    /**
     * Получить читаемое сообщение об ошибке
     */
    getUserMessage() {
        if (this.data.errors && Object.keys(this.data.errors).length > 0) {
            // Валидационные ошибки
            const errorMessages = Object.entries(this.data.errors)
                .map(([field, msg]) => {
                    const fieldNames = {
                        city: 'Город',
                        checkIn: 'Дата заезда',
                        checkOut: 'Дата выезда',
                        guests: 'Количество гостей',
                        customerName: 'Имя',
                        customerEmail: 'Email',
                        hotelId: 'Отель'
                    };
                    return `${fieldNames[field] || field}: ${msg}`;
                })
                .join('\n');
            return errorMessages;
        }

        return this.data.message || 'Произошла неизвестная ошибка';
    }
}
