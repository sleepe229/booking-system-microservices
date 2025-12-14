package com.hotel.events;

import java.io.Serializable;

public record BookingConfirmedEvent(
        String bookingId,
        String hotelId,
        String customerEmail,
        Double finalPrice,
        Double discount
){}
