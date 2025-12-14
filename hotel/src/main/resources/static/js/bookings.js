/**
 * Bookings Module - управление бронированиями
 */

let currentPage = 0;
const pageSize = 10;

function initBookings() {
    document.getElementById('bookingForm').addEventListener('submit', async (e) => {
        e.preventDefault();

        const booking = {
            hotelId: document.getElementById('hotelId').value,
            customerName: document.getElementById('customerName').value,
            customerEmail: document.getElementById('customerEmail').value,
            checkIn: document.getElementById('checkIn').value,
            checkOut: document.getElementById('checkOut').value,
            guests: parseInt(document.getElementById('guests').value)
        };

        try {
            const result = await HOTELS_API.createBooking(booking);
            showStatus('✅ Booking created successfully! ID: ' + result.bookingId, 'success');
            document.getElementById('bookingForm').reset();
            loadBookings();
        } catch (e) {
            showStatus('❌ Error: ' + e.message, 'error');
        }
    });

    loadBookings();
}

async function loadBookings() {
    try {
        const result = await HOTELS_API.listBookings(currentPage, pageSize);

        const embedded = result._embedded || {};
        const items = embedded.bookingResponseList || embedded.bookings || result.content || [];

        displayBookings(items);  // ⬅ сюда отправляем items, не result
        updatePagination(result);
    } catch (e) {
        console.error('Error loading bookings:', e);
        document.getElementById('bookingsList').innerHTML = '<p>Error loading bookings</p>';
    }
}


function displayBookings(bookings) {
    const container = document.getElementById('bookingsList');
    container.innerHTML = '';

    if (!bookings || bookings.length === 0) {
        container.innerHTML = '<p>No bookings found</p>';
        return;
    }

    bookings.forEach(raw => {
        const booking = raw.content || raw;

        const checkIn = booking.checkIn || '-';
        const checkOut = booking.checkOut || '-';
        const guests = booking.guests ?? '-';
        const price = booking.finalPrice ?? booking.basePrice ?? 0;
        const discount = booking.discount ?? 0;
        const status = booking.status || 'PENDING';
        const statusClass = status.toLowerCase();

        const card = document.createElement('div');
        card.className = `booking-card ${statusClass}`;
        card.innerHTML = `
            <h4>Booking #${booking.bookingId}</h4>
            <p><strong>Hotel:</strong> ${booking.hotelId}</p>
            <p><strong>Customer:</strong> ${booking.customerName}</p>
            <p><strong>Email:</strong> ${booking.customerEmail}</p>
            <p><strong>Check-in:</strong> ${checkIn}</p>
            <p><strong>Check-out:</strong> ${checkOut}</p>
            <p><strong>Guests:</strong> ${guests}</p>
            <p><strong>Price:</strong> $${price}</p>
            ${discount ? `<p><strong>Discount:</strong> ${discount}%</p>` : ''}
            <span class="badge ${statusClass}">${status}</span>
            <button class="btn-secondary" onclick="cancelBooking('${booking.bookingId}')" style="margin-top: 10px; width: 100%;">
                Cancel Booking
            </button>
        `;
        container.appendChild(card);
    });
}


async function cancelBooking(bookingId) {
    if (!confirm('Are you sure you want to cancel this booking?')) return;

    try {
        await HOTELS_API.cancelBooking({bookingId});
        showStatus('✅ Booking cancelled successfully!', 'success');
        loadBookings();
    } catch (e) {
        showStatus('❌ Error: ' + e.message, 'error');
    }
}

async function searchBooking() {
    const bookingId = document.getElementById('searchBookingId').value.trim();
    if (!bookingId) {
        alert('Please enter a booking ID');
        return;
    }

    try {
        const booking = await HOTELS_API.getBooking(bookingId);
        const checkIn = booking.checkIn || '-';
        const checkOut = booking.checkOut || '-';
        const guests = booking.guests ?? '-';
        const price = booking.finalPrice ?? 0;

        resultDiv.innerHTML = `
            <div class="booking-card">
                <h4>Booking #${booking.bookingId}</h4>
                <p><strong>Customer:</strong> ${booking.customerName}</p>
                <p><strong>Email:</strong> ${booking.customerEmail}</p>
                <p><strong>Status:</strong> ${booking.status}</p>
                <p><strong>Check-in:</strong> ${checkIn}</p>
                <p><strong>Check-out:</strong> ${checkOut}</p>
                <p><strong>Guests:</strong> ${guests}</p>
                <p><strong>Price:</strong> $${price}</p>
            </div>
        `;

    } catch (e) {
        document.getElementById('searchResult').innerHTML = `<p style="color: red;">❌ Not found: ${e.message}</p>`;
    }
}

function updatePagination(result) {
    const pageInfo = document.getElementById('pageInfo');
    const totalPages = result.page?.totalPages || 1;
    pageInfo.textContent = `Page ${currentPage + 1} of ${totalPages}`;

    document.getElementById('prevPage').disabled = currentPage === 0;
    document.getElementById('nextPage').disabled = currentPage >= totalPages - 1;

    document.getElementById('prevPage').onclick = () => {
        if (currentPage > 0) {
            currentPage--;
            loadBookings();
        }
    };

    document.getElementById('nextPage').onclick = () => {
        if (currentPage < totalPages - 1) {
            currentPage++;
            loadBookings();
        }
    };
}

function showStatus(message, type) {
    const statusDiv = document.getElementById('bookingStatus');
    statusDiv.textContent = message;
    statusDiv.className = `status-message ${type}`;
    setTimeout(() => {
        statusDiv.className = 'status-message';
    }, 5000);
}
