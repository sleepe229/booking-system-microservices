package com.hotel.events;

import java.io.Serializable;

public record BookingRejectedEvent(
        String bookingId,
        String customerEmail,
        String reason
) {}
