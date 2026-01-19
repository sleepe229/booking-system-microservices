package com.hotel.events;

import java.io.Serializable;

public record BookingPaidEvent(
        String bookingId,
        String customerEmail,
        String customerName,
        double finalPrice,
        String paymentMethod,  // "card", "paypal", etc.
        long timestamp
) implements Serializable {
}
