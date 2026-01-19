/**
 * hotels.js - Управление каталогом отелей
 */

const Hotels = {
    /**
     * Загрузка отелей по параметрам поиска
     */
    async loadHotels(city, checkIn, checkOut, guests) {
        const loadingEl = document.getElementById('loadingHotels');
        const gridEl = document.getElementById('hotelsGrid');
        const noHotelsEl = document.getElementById('noHotels');

        loadingEl.style.display = 'block';
        gridEl.innerHTML = '';
        noHotelsEl.style.display = 'none';

        try {
            console.log('🔍 Searching hotels:', { city, checkIn, checkOut, guests });

            const hotels = await API.searchHotels(city, checkIn, checkOut, guests);

            console.log('✅ Found hotels:', hotels.length);

            STATE.hotels = hotels;
            STATE.searchParams = { city, checkIn, checkOut, guests };

            loadingEl.style.display = 'none';

            if (hotels.length === 0) {
                noHotelsEl.style.display = 'block';
                document.getElementById('hotelsCount').textContent = '0 hotels';
                return;
            }

            document.getElementById('hotelsCount').textContent =
                `${hotels.length} hotel${hotels.length > 1 ? 's' : ''}`;
            this.renderHotels(hotels);

        } catch (error) {
            console.error('❌ Error loading hotels:', error);
            loadingEl.style.display = 'none';

            // ✅ Красивое отображение ошибки
            if (error instanceof ApiError) {
                gridEl.innerHTML = this.renderErrorState(error);
            } else {
                gridEl.innerHTML = this.renderErrorState({
                    getUserMessage: () => 'Не удалось загрузить отели. Проверьте подключение к интернету.'
                });
            }
        }
    },

    /**
     * ✅ Красивое отображение ошибки
     */
    renderErrorState(error) {
        const message = error.getUserMessage();
        return `
            <div class="error-state" style="
                grid-column: 1 / -1;
                padding: 3rem;
                text-align: center;
                background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
                border-radius: 12px;
                border: 2px solid #ef4444;
            ">
                <div style="font-size: 3rem; margin-bottom: 1rem;">😕</div>
                <h3 style="color: #dc2626; margin-bottom: 1rem; font-size: 1.25rem;">
                    Что-то пошло не так
                </h3>
                <p style="color: #991b1b; white-space: pre-line; line-height: 1.6;">
                    ${this.escapeHtml(message)}
                </p>
                <button 
                    onclick="location.reload()" 
                    style="
                        margin-top: 1.5rem;
                        padding: 0.75rem 1.5rem;
                        background: #dc2626;
                        color: white;
                        border: none;
                        border-radius: 8px;
                        font-size: 1rem;
                        cursor: pointer;
                        transition: all 0.2s;
                    "
                    onmouseover="this.style.background='#b91c1c'"
                    onmouseout="this.style.background='#dc2626'"
                >
                    🔄 Обновить страницу
                </button>
            </div>
        `;
    },

    /**
     * Отрисовка карточек отелей
     */
    renderHotels(hotels) {
        const gridEl = document.getElementById('hotelsGrid');

        gridEl.innerHTML = hotels.map(hotel => `
            <div class="hotel-card" onclick="Hotels.selectHotel('${hotel.hotelId}')">
                <div class="hotel-image">🏨</div>
                <div class="hotel-content">
                    <h3 class="hotel-name">${this.escapeHtml(hotel.name)}</h3>
                    <div class="hotel-location">📍 ${this.escapeHtml(hotel.city)}, ${this.escapeHtml(hotel.address)}</div>
                    <div class="hotel-price">
                        <div>
                            <div class="price-value">$${hotel.pricePerNight.toFixed(0)}</div>
                            <div class="price-label">per night</div>
                        </div>
                        <button class="btn-book" onclick="event.stopPropagation(); Hotels.selectHotel('${hotel.hotelId}')">
                            Book Now
                        </button>
                    </div>
                </div>
            </div>
        `).join('');
    },

    /**
     * Выбор отеля для бронирования
     */
    selectHotel(hotelId) {
        const hotel = STATE.hotels.find(h => h.hotelId === hotelId);
        if (!hotel) {
            console.error('Hotel not found:', hotelId);
            return;
        }

        console.log('🏨 Selected hotel:', hotel);
        STATE.selectedHotel = hotel;

        openModal(hotel);
    },

    /**
     * Escape HTML для безопасности
     */
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
};
