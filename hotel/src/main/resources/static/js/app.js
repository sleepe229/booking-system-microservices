/**
 * Main App - инициализация и управление вкладками
 */

document.addEventListener('DOMContentLoaded', () => {
    // Инициализация модулей
    initBookings();

    // Управление вкладками
    const navButtons = document.querySelectorAll('.nav-btn');
    navButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const tabName = btn.dataset.tab;

            // Скрыть все вкладки
            document.querySelectorAll('.tab-content').forEach(tab => {
                tab.classList.remove('active');
            });

            // Убрать активность с всех кнопок
            navButtons.forEach(b => b.classList.remove('active'));

            // Показать выбранную вкладку
            document.getElementById(tabName).classList.add('active');
            btn.classList.add('active');

            // Загрузить данные при открытии аудит-вкладки
            if (tabName === 'audit') {
                loadAuditLogs();
            }
        });
    });

    console.log('✅ Application initialized');
});
