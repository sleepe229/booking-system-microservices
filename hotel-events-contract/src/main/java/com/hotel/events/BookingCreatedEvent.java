package com.hotel.events;

import java.io.Serializable;

public record BookingCreatedEvent(
        String bookingId,
        String userId,
        String hotelId,
        String customerName,
        String customerEmail,
        String checkIn,
        String checkOut
) {}