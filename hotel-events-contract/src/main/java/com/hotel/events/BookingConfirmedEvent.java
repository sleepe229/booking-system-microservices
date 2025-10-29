package com.hotel.events;

import java.io.Serializable;

public record BookingConfirmedEvent(
        Long bookingId,
        Long hotelId,
        String customerEmail,
        Double finalPrice,
        Double discount
) implements Serializable {}
