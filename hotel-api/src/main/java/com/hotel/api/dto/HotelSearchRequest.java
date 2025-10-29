package com.hotel.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HotelSearchRequest(
        @NotBlank(message = "Город обязателен") String city,
        @NotNull(message = "Дата заезда обязательна") String checkIn,
        @NotNull(message = "Дата выезда обязательна") String checkOut,
        @NotNull(message = "Количество гостей обязательно") Integer guests
) {}
