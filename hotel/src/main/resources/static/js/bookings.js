/**
 * bookings.js - Управление процессом бронирования
 */

/**
 * Открытие модального окна
 */
function openModal(hotel) {
    const modal = document.getElementById('bookingModal');
    STATE.selectedHotel = hotel;

    console.log('🏨 Opening modal for:', hotel);

    // Показываем шаг 1
    showStep(1);

    // Заполняем информацию об отеле
    const { checkIn, checkOut, guests } = STATE.searchParams;
    const nights = calculateNights(checkIn, checkOut);
    const totalPrice = nights * hotel.pricePerNight;

    document.getElementById('modalHotelName').textContent = hotel.name;
    document.getElementById('modalHotelLocation').textContent = `📍 ${hotel.city}, ${hotel.address}`;
    document.getElementById('modalHotelPrice').textContent = `$${hotel.pricePerNight}/night`;

    document.getElementById('modalCheckIn').textContent = checkIn;
    document.getElementById('modalCheckOut').textContent = checkOut;
    document.getElementById('modalGuests').textContent = guests;
    document.getElementById('modalTotalPrice').textContent = `$${totalPrice.toFixed(2)} (${nights} nights)`;

    modal.classList.add('show');
}

/**
 * Закрытие модального окна
 */
function closeModal() {
    const modal = document.getElementById('bookingModal');
    modal.classList.remove('show');

    STATE.currentBooking = null;
    STATE.selectedHotel = null;

    // Очищаем форму
    const form = document.getElementById('bookingForm');
    if (form) {
        form.reset();
    }

    console.log('❌ Modal closed');
}

/**
 * Отправка бронирования
 */
async function submitBooking(event) {
    event.preventDefault();

    const hotel = STATE.selectedHotel;
    const { checkIn, checkOut, guests } = STATE.searchParams;

    const bookingData = {
        hotelId: hotel.hotelId,
        customerName: document.getElementById('customerName').value,
        customerEmail: document.getElementById('customerEmail').value,
        customerPhone: document.getElementById('customerPhone').value,
        checkIn: checkIn,
        checkOut: checkOut,
        guests: guests,
        userId: STATE.userId
    };

    console.log('📤 Submitting booking:', bookingData);

    // Показываем loading
    showStep(2);

    try {
        const result = await API.createBooking(bookingData);
        console.log('✅ Booking created:', result);

        // Сохраняем для WebSocket
        STATE.currentBooking = {
            bookingId: result.bookingId,
            hotelName: hotel.name,
            ...bookingData
        };

        // ⭐ Переходим на Step 3 - ждем цену от WebSocket
        showStep(3);

        // Заполняем ID букинга
        document.getElementById('confirmBookingId').textContent = result.bookingId;

        // Показываем loading цены
        document.getElementById('priceWaitingState').style.display = 'block';
        document.getElementById('priceConfirmActions').style.display = 'none';

        // Рассчитываем оригинальную цену
        const nights = calculateNights(checkIn, checkOut);
        const originalPrice = nights * hotel.pricePerNight;
        document.getElementById('confirmOriginalPrice').textContent = `$${originalPrice.toFixed(2)}`;

        // WebSocket обновит цену через handlePriceUpdate()

    } catch (error) {
        console.error('❌ Booking error:', error);
        showToast('Booking failed: ' + error.message, 'error');
        showStep(1);
    }
}

/**
 * ⭐ Обработка обновления цены от WebSocket
 */
function handlePriceUpdate(message) {
    console.log('💰 Price update received:', message);

    if (!STATE.currentBooking) {
        console.warn('⚠️ No active booking, ignoring price update');
        return;
    }

    if (message.bookingId !== STATE.currentBooking.bookingId) {
        console.warn('⚠️ Price update for different booking, ignoring');
        return;
    }

    if (message.status === 'CONFIRMED') {
        // Сохраняем финальную цену
        STATE.currentBooking.finalPrice = message.finalPrice;
        STATE.currentBooking.discountPercentage = message.discountPercentage || 0;
        STATE.currentBooking.recommendations = message.recommendations || [];

        // Обновляем UI на Step 3
        document.getElementById('confirmFinalPrice').textContent = `$${message.finalPrice.toFixed(2)}`;

        if (message.discountPercentage > 0) {
            document.getElementById('confirmDiscount').textContent = `🎁 ${message.discountPercentage}% OFF`;
            document.getElementById('discountRow').style.display = 'flex';
        } else {
            document.getElementById('discountRow').style.display = 'none';
        }

        // Прячем loading, показываем кнопки
        document.getElementById('priceWaitingState').style.display = 'none';
        document.getElementById('priceConfirmActions').style.display = 'flex';

        showToast(`Final price ready: $${message.finalPrice.toFixed(2)} 💰`, 'success');

    } else if (message.status === 'REJECTED') {
        showToast('Booking rejected: ' + (message.message || 'Unknown error'), 'error');
        setTimeout(() => closeModal(), 2000);
    }
}

/**
 * ⭐ Подтверждение оплаты (Step 3 → Step 4)
 */
function confirmPayment() {
    console.log('💳 User confirmed payment');

    // Переходим на Step 4 - processing payment
    showStep(4);

    // Запускаем оплату
    setTimeout(() => processPayment(STATE.currentBooking.bookingId), 500);
}

/**
 * ⭐ Отмена оплаты
 */
function cancelPayment() {
    console.log('❌ User cancelled payment');

    showToast('Booking cancelled', 'info');
    closeModal();

    // Опционально: можно отменить бронирование на backend
    // API.cancelBooking(STATE.currentBooking.bookingId);
}

/**
 * Обработка оплаты (Step 4)
 */
async function processPayment(bookingId) {
    console.log('💳 Processing payment for:', bookingId);

    try {
        const result = await API.payBooking(bookingId, 'card');
        console.log('✅ Payment successful:', result);

        // Показываем успех на Step 5
        showPaymentSuccess(result);

        showToast('Payment successful! 🎉', 'success');

    } catch (error) {
        console.error('❌ Payment error:', error);
        showToast('Payment failed: ' + error.message, 'error');

        // Возвращаемся к Step 3
        setTimeout(() => {
            showStep(3);
            document.getElementById('priceWaitingState').style.display = 'none';
            document.getElementById('priceConfirmActions').style.display = 'flex';
        }, 2000);
    }
}

/**
 * Показать успешную оплату (Step 5)
 */
function showPaymentSuccess(result) {
    showStep(5);

    const booking = STATE.currentBooking;
    const nights = calculateNights(booking.checkIn, booking.checkOut);
    const originalPrice = nights * STATE.selectedHotel.pricePerNight;

    document.getElementById('confirmedBookingId').textContent = result.bookingId;
    document.getElementById('confirmedHotelName').textContent = STATE.selectedHotel.name;
    document.getElementById('confirmedCheckIn').textContent = formatDate(booking.checkIn);
    document.getElementById('confirmedCheckOut').textContent = formatDate(booking.checkOut);
    document.getElementById('confirmedCustomerName').textContent = booking.customerName;

    document.getElementById('confirmedOriginalPrice').textContent = `$${originalPrice.toFixed(2)}`;
    document.getElementById('confirmedFinalPrice').textContent = `$${result.finalPrice.toFixed(2)}`;

    const discountBadge = document.getElementById('confirmedDiscount');
    if (result.discountPercentage > 0) {
        discountBadge.textContent = `🎁 ${result.discountPercentage}% OFF`;
        discountBadge.style.display = 'inline-block';
    } else {
        discountBadge.style.display = 'none';
    }

    // Показываем рекомендации
    if (booking.recommendations && booking.recommendations.length > 0) {
        showRecommendations(booking.recommendations);
    } else {
        document.getElementById('recommendationsSection').style.display = 'none';
    }
}

/**
 * Показать рекомендации
 */
function showRecommendations(hotelIds) {
    const section = document.getElementById('recommendationsSection');
    const grid = document.getElementById('recommendationsGrid');

    if (!hotelIds || hotelIds.length === 0) {
        section.style.display = 'none';
        return;
    }

    const recommendations = hotelIds
        .map(id => STATE.hotels.find(h => h.hotelId === id))
        .filter(h => h);

    if (recommendations.length === 0) {
        section.style.display = 'none';
        return;
    }

    grid.innerHTML = recommendations.map(hotel => `
        <div class="recommendation-card" onclick="closeModal(); setTimeout(() => Hotels.selectHotel('${hotel.hotelId}'), 300)">
            <div style="font-size: 2rem; text-align: center; margin-bottom: 0.5rem;">🏨</div>
            <h4 style="font-size: 0.95rem; margin-bottom: 0.25rem; font-weight: 600;">
                ${escapeHtml(hotel.name)}
            </h4>
            <p style="font-size: 0.8rem; color: var(--text-secondary); margin-bottom: 0.5rem;">
                ${escapeHtml(hotel.city)}
            </p>
            <p style="font-size: 1.1rem; font-weight: 700; color: var(--primary);">
                $${hotel.pricePerNight}/night
            </p>
        </div>
    `).join('');

    section.style.display = 'block';
}

/**
 * Переключение шагов
 */
function showStep(step) {
    console.log('📍 Showing step:', step);

    // Прячем все шаги
    for (let i = 1; i <= 5; i++) {
        const el = document.getElementById(`step${i}`);
        if (el) {
            el.style.display = 'none';
        }
    }

    // Показываем нужный
    const currentStep = document.getElementById(`step${step}`);
    if (currentStep) {
        currentStep.style.display = 'block';
    } else {
        console.error(`❌ Step ${step} element not found!`);
    }
}

/**
 * Расчёт количества ночей
 */
function calculateNights(checkIn, checkOut) {
    const d1 = new Date(checkIn);
    const d2 = new Date(checkOut);
    const nights = Math.ceil((d2 - d1) / (1000 * 60 * 60 * 24));
    return nights > 0 ? nights : 1;
}

/**
 * Форматирование даты
 */
function formatDate(dateStr) {
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}

/**
 * Escape HTML
 */
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * Показать toast
 */
function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast-notification toast-${type}`;
    toast.textContent = message;

    document.body.appendChild(toast);

    setTimeout(() => toast.classList.add('show'), 100);

    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

console.log('✅ Bookings module loaded');
