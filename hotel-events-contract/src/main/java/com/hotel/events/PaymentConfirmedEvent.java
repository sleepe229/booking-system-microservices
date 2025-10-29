package com.hotel.events;

import java.io.Serializable;

public record PaymentConfirmedEvent(
        Long bookingId,
        Double amount,
        String paymentMethod
) implements Serializable {}
