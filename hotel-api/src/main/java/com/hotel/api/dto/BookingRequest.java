package com.hotel.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookingRequest(
        @NotNull String hotelId,
        @NotNull String checkIn,
        @NotNull String checkOut,
        @NotNull Integer guests,
        @NotBlank String customerName,
        @NotBlank String customerEmail
) {}
