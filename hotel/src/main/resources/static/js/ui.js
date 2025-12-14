/**
 * ui.js - Управление интерфейсом и отображением данных
 */

const UI = {
    /**
     * Обновить статус WebSocket
     */
    updateWebSocketStatus(connected) {
        const wsStatus = document.getElementById('wsStatus');
        const text = wsStatus.querySelector('span');

        if (connected) {
            wsStatus.classList.add('connected');
            text.textContent = '✅ WebSocket: Connected';
        } else {
            wsStatus.classList.remove('connected');
            text.textContent = '⏳ WebSocket: Connecting...';
        }
    },

    /**
     * Показать статус сообщение
     */
    showStatus(message, type) {
        const statusBox = document.getElementById('formStatus');
        statusBox.textContent = message;
        statusBox.className = `status-box ${type} show`;

        if (type !== 'pending') {
            setTimeout(() => {
                statusBox.classList.remove('show');
            }, 5000);
        }
    },

    /**
     * Отобразить результат бронирования
     */
    displayBookingResult(data) {
        const resultBox = document.getElementById('resultBox');

        // Заполняем данные
        document.getElementById('resultBookingId').textContent = STATE.currentBooking.bookingId;
        document.getElementById('resultHotel').textContent = STATE.currentBooking.hotelId;
        document.getElementById('resultCustomer').textContent = STATE.currentBooking.customerName;
        document.getElementById('resultCheckIn').textContent = STATE.currentBooking.checkIn;
        document.getElementById('resultCheckOut').textContent = STATE.currentBooking.checkOut;
        document.getElementById('resultGuests').textContent = STATE.currentBooking.guests;

        // Финальная цена
        const finalPrice = data.finalPrice || 0;
        document.getElementById('resultPrice').textContent = `$${finalPrice.toFixed(2)}`;

        // Скидка
        if (data.discountPercentage && data.discountPercentage > 0) {
            document.getElementById('resultDiscount').textContent =
                `🎁 Discount: ${data.discountPercentage}%`;
        }

        resultBox.classList.add('show');
        this.showStatus('✅ Booking confirmed with final price!', 'success');
    },

    /**
     * Установить значения дат по умолчанию
     */
    setupDateDefaults() {
        const today = new Date();
        const tomorrow = new Date(today);
        tomorrow.setDate(tomorrow.getDate() + 1);

        const formatDate = (date) => date.toISOString().split('T')[0];

        document.getElementById('checkIn').value = formatDate(today);
        document.getElementById('checkOut').value = formatDate(tomorrow);
    }
};