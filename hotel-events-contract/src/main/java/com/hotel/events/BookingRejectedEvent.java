package com.hotel.events;

import java.io.Serializable;

public record BookingRejectedEvent(
        Long bookingId,
        String customerEmail,
        String reason
) {}
