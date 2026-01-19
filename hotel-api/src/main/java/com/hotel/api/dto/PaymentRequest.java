package com.hotel.api.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentRequest(
        @NotBlank(message = "Booking ID обязателен") String bookingId,
        @NotBlank(message = "Метод оплаты обязателен") String paymentMethod,
        String cardNumber,
        String cardHolder
) {}
