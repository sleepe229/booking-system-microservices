package com.hotel.api.dto;

import jakarta.validation.constraints.NotNull;

public record CancelBookingRequest(
        @NotNull String bookingId
) {}

