/**
 * app.js - Инициализация приложения
 */

document.addEventListener('DOMContentLoaded', () => {
    console.log('🚀 Application starting...');

    // 1. Инициализация WebSocket
    WS.initialize();

    // 2. Инициализация autocomplete
    CityAutocomplete.init();  // ✅ ДОБАВИЛИ

    // 3. Установка дефолтных дат
    UI.setupDateDefaults();

    // 4. Обработчик формы поиска
    document.getElementById('searchForm').addEventListener('submit', async (e) => {
        e.preventDefault();

        const city = document.getElementById('searchCity').value.trim();
        const checkIn = document.getElementById('searchCheckIn').value;
        const checkOut = document.getElementById('searchCheckOut').value;
        const guests = parseInt(document.getElementById('searchGuests').value);

        if (!city) {
            UI.showNotification('❌ Пожалуйста, укажите город', 'error');
            document.getElementById('searchCity').focus();
            return;
        }

        if (!checkIn) {
            UI.showNotification('❌ Пожалуйста, укажите дату заезда', 'error');
            document.getElementById('searchCheckIn').focus();
            return;
        }

        if (!checkOut) {
            UI.showNotification('❌ Пожалуйста, укажите дату выезда', 'error');
            document.getElementById('searchCheckOut').focus();
            return;
        }

        const checkInDate = new Date(checkIn);
        const checkOutDate = new Date(checkOut);

        if (checkOutDate <= checkInDate) {
            UI.showNotification('❌ Дата выезда должна быть позже даты заезда', 'error');
            document.getElementById('searchCheckOut').focus();
            return;
        }

        if (!guests || guests < 1 || guests > 10) {
            UI.showNotification('❌ Количество гостей должно быть от 1 до 10', 'error');
            document.getElementById('searchGuests').focus();
            return;
        }

        await Hotels.loadHotels(city, checkIn, checkOut, guests);
    });

    // 5. Загружаем отели с дефолтными параметрами
    const defaultCity = 'Moscow';  // ✅ Дефолтный город
    const defaultCheckIn = document.getElementById('searchCheckIn').value;
    const defaultCheckOut = document.getElementById('searchCheckOut').value;
    const defaultGuests = parseInt(document.getElementById('searchGuests').value);

    document.getElementById('searchCity').value = defaultCity;
    Hotels.loadHotels(defaultCity, defaultCheckIn, defaultCheckOut, defaultGuests);

    console.log('✅ Application initialized');
});
