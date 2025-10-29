package com.hotel.events;

import java.io.Serializable;

public record DiscountCalculatedEvent(
        Long bookingId,
        Double discountAmount,
        String discountType
) implements Serializable {}
