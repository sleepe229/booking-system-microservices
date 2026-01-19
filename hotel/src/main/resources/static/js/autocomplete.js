/**
 * autocomplete.js - Autocomplete для городов
 */

const CityAutocomplete = {
    input: null,
    dropdown: null,
    cities: [],
    selectedIndex: -1,
    debounceTimer: null,

    /**
     * Инициализация autocomplete
     */
    init() {
        this.input = document.getElementById('searchCity');
        this.dropdown = document.getElementById('citySuggestions');

        if (!this.input || !this.dropdown) {
            console.warn('Autocomplete elements not found');
            return;
        }

        // Event listeners
        this.input.addEventListener('input', (e) => this.handleInput(e));
        this.input.addEventListener('keydown', (e) => this.handleKeydown(e));
        this.input.addEventListener('focus', (e) => this.handleFocus(e));

        // Закрытие при клике вне элемента
        document.addEventListener('click', (e) => {
            if (!this.input.contains(e.target) && !this.dropdown.contains(e.target)) {
                this.hideDropdown();
            }
        });

        console.log('✅ City autocomplete initialized');
    },

    /**
     * Обработка ввода текста
     */
    handleInput(e) {
        const query = e.target.value.trim();

        // Debounce для снижения нагрузки
        clearTimeout(this.debounceTimer);

        if (query.length < 1) {
            this.hideDropdown();
            return;
        }

        this.debounceTimer = setTimeout(() => {
            this.searchCities(query);
        }, 200);
    },

    /**
     * Обработка нажатий клавиш
     */
    handleKeydown(e) {
        if (!this.dropdown.classList.contains('show')) return;

        switch (e.key) {
            case 'ArrowDown':
                e.preventDefault();
                this.selectedIndex = Math.min(this.selectedIndex + 1, this.cities.length - 1);
                this.updateSelection();
                break;

            case 'ArrowUp':
                e.preventDefault();
                this.selectedIndex = Math.max(this.selectedIndex - 1, -1);
                this.updateSelection();
                break;

            case 'Enter':
                e.preventDefault();
                if (this.selectedIndex >= 0) {
                    this.selectCity(this.cities[this.selectedIndex]);
                }
                break;

            case 'Escape':
                this.hideDropdown();
                break;
        }
    },

    /**
     * Обработка фокуса - показываем популярные города
     */
    handleFocus(e) {
        const query = e.target.value.trim();
        if (query.length === 0) {
            this.searchCities(''); // Покажем все города
        }
    },

    /**
     * Запрос городов с сервера
     */
    async searchCities(query) {
        try {
            const url = query
                ? `${CONFIG.GATEWAY_URL}/hotels/cities?query=${encodeURIComponent(query)}`
                : `${CONFIG.GATEWAY_URL}/hotels/cities`;

            const response = await fetch(url);

            if (!response.ok) {
                throw new Error('Failed to fetch cities');
            }

            this.cities = await response.json();
            this.showDropdown();
        } catch (error) {
            console.error('❌ Error fetching cities:', error);
            this.cities = [];
            this.hideDropdown();
        }
    },

    /**
     * Показать dropdown с результатами
     */
    showDropdown() {
        if (this.cities.length === 0) {
            this.dropdown.innerHTML = `
                <div class="autocomplete-item autocomplete-empty">
                    No cities found
                </div>
            `;
            this.dropdown.classList.add('show');
            return;
        }

        this.dropdown.innerHTML = this.cities
            .slice(0, 10) // Показываем максимум 10
            .map((city, index) => `
                <div 
                    class="autocomplete-item" 
                    data-index="${index}"
                    onclick="CityAutocomplete.selectCity('${this.escapeHtml(city)}')"
                >
                    📍 ${this.escapeHtml(city)}
                </div>
            `)
            .join('');

        this.dropdown.classList.add('show');
        this.selectedIndex = -1;
    },

    /**
     * Скрыть dropdown
     */
    hideDropdown() {
        this.dropdown.classList.remove('show');
        this.selectedIndex = -1;
    },

    /**
     * Обновить выделение при навигации клавишами
     */
    updateSelection() {
        const items = this.dropdown.querySelectorAll('.autocomplete-item');

        items.forEach((item, index) => {
            if (index === this.selectedIndex) {
                item.classList.add('selected');
                item.scrollIntoView({ block: 'nearest' });
            } else {
                item.classList.remove('selected');
            }
        });
    },

    /**
     * Выбрать город
     */
    selectCity(city) {
        this.input.value = city;
        this.hideDropdown();
        this.input.blur();

        console.log('🏙️ Selected city:', city);
    },

    /**
     * Escape HTML
     */
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
};
