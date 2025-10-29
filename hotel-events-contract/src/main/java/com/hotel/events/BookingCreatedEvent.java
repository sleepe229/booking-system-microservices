package com.hotel.events;

import java.io.Serializable;

public record BookingCreatedEvent(
        Long bookingId,
        Long hotelId,
        String customerName,
        String customerEmail,
        String checkIn,
        String checkOut
) implements Serializable {}