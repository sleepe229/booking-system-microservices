package com.hotel.events;

import java.io.Serializable;

public record BookingCancelledEvent(
        String bookingId,
        String customerEmail
){}