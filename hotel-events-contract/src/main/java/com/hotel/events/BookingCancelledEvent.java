package com.hotel.events;

import java.io.Serializable;

public record BookingCancelledEvent(
        Long bookingId,
        String customerEmail
) implements Serializable {}