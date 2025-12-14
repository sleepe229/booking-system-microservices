/**
 * bookings.js - Логика управления бронированиями
 */

const Bookings = {
    /**
     * Инициализировать обработчик формы
     */
    setupFormHandler() {
        document.getElementById('bookingForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.createBooking();
        });
    },

    /**
     * Создать новое бронирование
     */
    async createBooking() {
        const booking = {
            hotelId: document.getElementById('hotelId').value,
            customerName: document.getElementById('customerName').value,
            customerEmail: document.getElementById('customerEmail').value,
            checkIn: document.getElementById('checkIn').value,
            checkOut: document.getElementById('checkOut').value,
            guests: parseInt(document.getElementById('guests').value),
            userId: STATE.userId  // ← ПЕРЕДАЁМ userId!
        };

        UI.showStatus('⏳ Processing booking...', 'pending');

        try {
            console.log('📤 Отправка запроса:', booking);

            const result = await API.createBooking(booking);
            const bookingData = result.content || result;

            console.log('✅ Ответ от сервера:', bookingData);

            STATE.pendingBookingId = bookingData.bookingId;

            // Сохраняем данные для отображения
            STATE.currentBooking = {
                bookingId: bookingData.bookingId,
                hotelId: booking.hotelId,
                customerName: booking.customerName,
                checkIn: booking.checkIn,
                checkOut: booking.checkOut,
                guests: booking.guests
            };

            UI.showStatus('✅ Booking created! Waiting for price calculation...', 'success');
            document.getElementById('bookingForm').reset();

        } catch (e) {
            console.error('❌ Error:', e);
            UI.showStatus(`❌ Error: ${e.message}`, 'error');
            STATE.pendingBookingId = null;
        }
    }
};