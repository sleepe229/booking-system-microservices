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
    // ✅ Если бронирование в процессе - показываем предупреждение
    if (STATE.currentBooking && !STATE.currentBooking.finalPrice) {
        const confirmed = confirm(
            '⚠️ WARNING!\n\n' +
            'Your booking is still being processed.\n' +
            'You will LOSE the booking result if you close now.\n\n' +
            'Are you sure?'
        );

        if (!confirmed) {
            return; // Не закрываем
        }
    }

    const modal = document.getElementById('bookingModal');
    modal.classList.remove('show');

    // ✅ Очищаем все таймеры
    clearAllTimeouts();

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
 * ✅ Отправка бронирования с проверкой буфера
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

        // ✅ Сохраняем для WebSocket (с временем старта)
        STATE.currentBooking = {
            bookingId: result.bookingId,
            hotelName: hotel.name,
            startTime: Date.now(),
            ...bookingData
        };

        // ✅ ПРОВЕРЯЕМ БУФЕР: может сообщение уже пришло?
        const pendingMessage = STATE.pendingWebSocketMessages.find(
            msg => msg.bookingId === result.bookingId
        );

        if (pendingMessage) {
            console.log('⚡ Found pending WebSocket message in buffer! Processing immediately...');

            // Удаляем из буфера
            STATE.pendingWebSocketMessages = STATE.pendingWebSocketMessages.filter(
                msg => msg.bookingId !== result.bookingId
            );

            // Показываем Step 3
            showStep(3);
            document.getElementById('confirmBookingId').textContent = result.bookingId;

            const nights = calculateNights(checkIn, checkOut);
            const originalPrice = nights * hotel.pricePerNight;
            document.getElementById('confirmOriginalPrice').textContent = `$${originalPrice.toFixed(2)}`;

            // ⚡ МОМЕНТАЛЬНО обрабатываем буферизованное сообщение
            setTimeout(() => handlePriceUpdate(pendingMessage), 100);

        } else {
            // ✅ Обычный flow - ждем WebSocket
            console.log('⏳ Waiting for WebSocket message...');

            showStep(3);
            document.getElementById('confirmBookingId').textContent = result.bookingId;

            const nights = calculateNights(checkIn, checkOut);
            const originalPrice = nights * hotel.pricePerNight;
            document.getElementById('confirmOriginalPrice').textContent = `$${originalPrice.toFixed(2)}`;

            // ✅ Показываем нормальный loading
            showPriceWaitingState('normal');

            // ✅ Через 10 секунд - предупреждение "НЕ ЗАКРЫВАЙТЕ!"
            STATE.warningTimeout = setTimeout(() => {
                if (!STATE.currentBooking.finalPrice) {
                    console.warn('⚠️ 10 seconds elapsed, showing warning');
                    showPriceWaitingState('warning');
                }
            }, 10000);

            // ✅ Через 30 секунд - fallback polling + КРИТИЧЕСКОЕ предупреждение
            STATE.fallbackTimeout = setTimeout(() => {
                if (!STATE.currentBooking.finalPrice) {
                    console.warn('⏱️ 30 seconds elapsed, starting polling');
                    showPriceWaitingState('timeout');
                    startPricePolling(result.bookingId);
                }
            }, 30000);
        }

    } catch (error) {
        console.error('❌ Booking error:', error);
        showToast('Booking failed: ' + error.message, 'error');
        showStep(1);
    }
}

/**
 * ✅ Обработка обновления цены от WebSocket (с возвратом статуса)
 */
function handlePriceUpdate(message) {
    console.log('💰 Processing price update:', message);

    // ✅ Проверяем timestamp (игнорируем старые сообщения >5 минут)
    if (message.timestamp) {
        const age = Date.now() - message.timestamp;
        if (age > 300000) {
            console.warn('⚠️ Ignoring old message (age:', age, 'ms)');
            return false;
        }
    }

    if (!STATE.currentBooking) {
        console.warn('⚠️ No active booking yet');
        return false; // ✅ Возвращаем false = не обработано
    }

    if (message.bookingId !== STATE.currentBooking.bookingId) {
        console.warn('⚠️ Price update for different booking:', message.bookingId, 'vs', STATE.currentBooking.bookingId);
        return false; // ✅ Возвращаем false = не обработано
    }

    // ✅ Очищаем все таймеры
    clearAllTimeouts();

    if (message.status === 'CONFIRMED') {
        STATE.currentBooking.finalPrice = message.finalPrice;
        STATE.currentBooking.discountPercentage = message.discountPercentage || 0;
        STATE.currentBooking.recommendations = message.recommendations || [];

        document.getElementById('confirmFinalPrice').textContent = `$${message.finalPrice.toFixed(2)}`;

        if (message.discountPercentage > 0) {
            document.getElementById('confirmDiscount').textContent = `🎁 ${message.discountPercentage}% OFF`;
            document.getElementById('discountRow').style.display = 'flex';
        } else {
            document.getElementById('discountRow').style.display = 'none';
        }

        document.getElementById('priceWaitingState').style.display = 'none';
        document.getElementById('priceConfirmActions').style.display = 'flex';

        showToast(`Final price ready: $${message.finalPrice.toFixed(2)} 💰`, 'success');

        return true; // ✅ Успешно обработано

    } else if (message.status === 'REJECTED') {
        showToast('Booking rejected: ' + (message.message || 'Unknown error'), 'error');
        setTimeout(() => closeModal(), 2000);

        return true; // ✅ Обработано (rejected)
    }

    return false; // ✅ Неизвестный статус
}

/**
 * ✅ Показать состояние ожидания цены (WebSocket-only версия)
 */
function showPriceWaitingState(state) {
    const waitingDiv = document.getElementById('priceWaitingState');

    if (state === 'normal') {
        // 0-10 секунд - обычный процесс
        waitingDiv.innerHTML = `
            <div class="spinner"></div>
            <p style="margin-top: 1rem; color: var(--text-secondary); font-size: 0.95rem;">
                Calculating final price and checking availability...
            </p>
        `;
        waitingDiv.style.display = 'block';

    } else if (state === 'warning') {
        // 10-30 секунд - предупреждение
        waitingDiv.innerHTML = `
            <div class="spinner"></div>
            <div style="text-align: center; margin-top: 1rem;">
                <p style="color: var(--warning); font-weight: 700; font-size: 1.2rem; margin-bottom: 0.5rem;">
                    ⚠️ Processing is taking longer than expected
                </p>
                <div style="background: var(--warning-light); padding: 1rem; border-radius: 8px; border-left: 4px solid var(--warning); margin-top: 1rem;">
                    <p style="color: var(--text-primary); font-weight: 600; margin: 0;">
                        ⚡ IMPORTANT: Please keep this window open!
                    </p>
                    <p style="color: var(--text-secondary); font-size: 0.9rem; margin: 0.5rem 0 0 0;">
                        Closing it will lose your booking result.
                    </p>
                </div>
            </div>
        `;
        waitingDiv.style.display = 'block';

    } else if (state === 'timeout') {
        // 30+ секунд - критическое состояние + polling
        const bookingId = STATE.currentBooking.bookingId;
        const elapsed = Math.round((Date.now() - STATE.currentBooking.startTime) / 1000);

        waitingDiv.innerHTML = `
            <div style="text-align: center; padding: 2rem 1rem;">
                <div style="font-size: 3rem; margin-bottom: 1rem; animation: pulse 2s infinite;">⏱️</div>
                
                <h3 style="color: var(--text-primary); margin-bottom: 0.5rem;">
                    Still processing your booking...
                </h3>
                <p style="color: var(--text-secondary); font-size: 0.9rem; margin-bottom: 1.5rem;">
                    Elapsed time: <strong>${elapsed}s</strong>
                </p>
                
                <div style="background: var(--danger-light); padding: 1.5rem; border-radius: 12px; border: 2px solid var(--danger); margin-bottom: 1.5rem;">
                    <div style="font-size: 2rem; margin-bottom: 0.5rem;">🚨</div>
                    <p style="color: var(--danger); font-weight: 700; font-size: 1.1rem; margin: 0 0 0.5rem 0;">
                        DO NOT CLOSE THIS WINDOW!
                    </p>
                    <p style="color: var(--text-primary); font-size: 0.95rem; margin: 0;">
                        We're actively checking your booking status.<br>
                        You'll lose the result if you close this page.
                    </p>
                </div>
                
                <div style="background: var(--bg-secondary); padding: 1rem; border-radius: 8px; margin-bottom: 1.5rem;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
                        <span style="color: var(--text-secondary); font-size: 0.9rem;">Booking ID:</span>
                        <span style="font-family: monospace; font-weight: 600; font-size: 0.85rem;">${bookingId}</span>
                    </div>
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                        <span style="color: var(--text-secondary); font-size: 0.9rem;">Status:</span>
                        <span style="color: var(--warning); font-weight: 600; font-size: 0.9rem;">
                            <span class="spinner-small"></span> Checking...
                        </span>
                    </div>
                </div>
                
                <div style="margin-bottom: 1rem;">
                    <div style="background: var(--bg-secondary); height: 8px; border-radius: 4px; overflow: hidden;">
                        <div class="polling-progress" style="height: 100%; background: var(--primary); width: 0%; transition: width 0.3s;"></div>
                    </div>
                    <p class="polling-text" style="font-size: 0.85rem; color: var(--text-secondary); margin-top: 0.5rem;">
                        Checking status...
                    </p>
                </div>
                
                <div style="display: flex; gap: 0.5rem;">
                    <button class="btn-primary" onclick="retryPriceCheck('${bookingId}')" style="flex: 1;">
                        🔄 Check Now
                    </button>
                    <button class="btn-secondary" onclick="showCancelWarning('${bookingId}')" style="flex: 1;">
                        ❌ Cancel
                    </button>
                </div>
            </div>
        `;
        waitingDiv.style.display = 'block';

        // Показываем browser notification (если разрешено)
        showBrowserNotification('Booking in progress',
            'Please keep the window open to receive your booking confirmation.');
    }
}

/**
 * ✅ Polling с обновлением прогресс-бара
 */
function startPricePolling(bookingId) {
    const MAX_ATTEMPTS = 60; // 120 секунд (60 × 2 сек)
    let attempts = 0;

    STATE.pollingInterval = setInterval(async () => {
        attempts++;

        console.log(`🔄 Polling booking status (${attempts}/${MAX_ATTEMPTS})`);

        // Обновляем прогресс бар
        const progress = (attempts / MAX_ATTEMPTS) * 100;
        const progressBar = document.querySelector('.polling-progress');
        const progressText = document.querySelector('.polling-text');

        if (progressBar) {
            progressBar.style.width = progress + '%';
        }
        if (progressText) {
            progressText.textContent = `Checking status... (${attempts}/${MAX_ATTEMPTS})`;
        }

        try {
            const status = await API.getBookingStatus(bookingId);

            if (status.status === 'CONFIRMED' && status.finalPrice > 0) {
                clearInterval(STATE.pollingInterval);
                clearAllTimeouts();

                showToast('✅ Booking confirmed!', 'success');

                handlePriceUpdate({
                    bookingId: status.bookingId,
                    status: 'CONFIRMED',
                    finalPrice: status.finalPrice,
                    discountPercentage: status.discount,
                    recommendations: status.recommendations || [],
                    timestamp: Date.now()
                });

            } else if (status.status === 'REJECTED') {
                clearInterval(STATE.pollingInterval);
                clearAllTimeouts();

                showToast('❌ Booking rejected: ' + (status.rejectionReason || 'Unknown'), 'error');
                setTimeout(() => closeModal(), 2000);

            } else if (attempts >= MAX_ATTEMPTS) {
                // После 120 секунд - финальная ошибка
                clearInterval(STATE.pollingInterval);
                showBookingTimeout(bookingId);
            }

        } catch (error) {
            console.error('❌ Polling error:', error);

            if (attempts >= MAX_ATTEMPTS) {
                clearInterval(STATE.pollingInterval);
                showBookingTimeout(bookingId);
            }
        }

    }, 2000);
}

/**
 * ✅ Показать финальный таймаут (после 120 сек)
 */
function showBookingTimeout(bookingId) {
    const waitingDiv = document.getElementById('priceWaitingState');

    waitingDiv.innerHTML = `
        <div style="text-align: center; padding: 2rem;">
            <div style="font-size: 4rem; margin-bottom: 1rem;">⏱️</div>
            
            <h3 style="color: var(--danger); margin-bottom: 1rem;">
                Booking Processing Timeout
            </h3>
            
            <div style="background: var(--warning-light); padding: 1.5rem; border-radius: 8px; margin-bottom: 1.5rem;">
                <p style="color: var(--text-primary); margin: 0;">
                    Your booking <strong>${bookingId}</strong> is experiencing delays.<br>
                    This might be due to system issues.
                </p>
            </div>
            
            <p style="color: var(--text-secondary); font-size: 0.95rem; margin-bottom: 1.5rem;">
                <strong>What to do:</strong><br>
                • Contact support with booking ID: <code>${bookingId}</code><br>
                • Try refreshing the page and checking booking status<br>
                • Your booking data is saved
            </p>
            
            <div style="display: flex; gap: 0.5rem; flex-direction: column;">
                <button class="btn-primary" onclick="retryPriceCheck('${bookingId}')">
                    🔄 Try Again
                </button>
                <button class="btn-secondary" onclick="closeModal()">
                    Close
                </button>
            </div>
        </div>
    `;

    showToast('⚠️ Booking timeout. Please contact support.', 'error');
}

/**
 * ✅ Предупреждение при попытке отмены
 */
function showCancelWarning(bookingId) {
    const confirmed = confirm(
        '⚠️ WARNING!\n\n' +
        'Your booking is still being processed.\n' +
        'If you close this window, you will LOSE the booking result.\n\n' +
        'Booking ID: ' + bookingId + '\n\n' +
        'Are you sure you want to close?'
    );

    if (confirmed) {
        showToast('⚠️ Booking result will be lost!', 'warning');
        closeModal();
    }
}

/**
 * ✅ Browser notification (если разрешено)
 */
function showBrowserNotification(title, body) {
    if (!('Notification' in window)) {
        return;
    }

    if (Notification.permission === 'granted') {
        new Notification(title, {
            body: body,
            icon: '/favicon.ico',
            badge: '/favicon.ico',
            tag: 'booking-status'
        });
    } else if (Notification.permission !== 'denied') {
        Notification.requestPermission().then(permission => {
            if (permission === 'granted') {
                new Notification(title, { body: body });
            }
        });
    }
}

/**
 * ✅ Повторная проверка
 */
async function retryPriceCheck(bookingId) {
    showPriceWaitingState('normal');
    showToast('🔄 Checking booking status...', 'info');

    try {
        const status = await API.getBookingStatus(bookingId);

        if (status.status === 'CONFIRMED' && status.finalPrice > 0) {
            clearAllTimeouts();

            handlePriceUpdate({
                bookingId: status.bookingId,
                status: 'CONFIRMED',
                finalPrice: status.finalPrice,
                discountPercentage: status.discount,
                recommendations: status.recommendations || [],
                timestamp: Date.now()
            });

        } else if (status.status === 'REJECTED') {
            showToast('❌ Booking was rejected: ' + status.rejectionReason, 'error');
            setTimeout(() => closeModal(), 2000);

        } else {
            showToast('⏳ Still processing... Keep waiting.', 'info');
            showPriceWaitingState('timeout');
            startPricePolling(bookingId);
        }

    } catch (error) {
        showToast('❌ Failed to check status: ' + error.message, 'error');
        showPriceWaitingState('timeout');
        startPricePolling(bookingId);
    }
}

/**
 * ✅ Очистка всех таймаутов
 */
function clearAllTimeouts() {
    if (STATE.warningTimeout) {
        clearTimeout(STATE.warningTimeout);
        STATE.warningTimeout = null;
    }

    if (STATE.fallbackTimeout) {
        clearTimeout(STATE.fallbackTimeout);
        STATE.fallbackTimeout = null;
    }

    if (STATE.pollingInterval) {
        clearInterval(STATE.pollingInterval);
        STATE.pollingInterval = null;
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
