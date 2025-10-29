package com.hotel.api.dto;

public record HotelSearchResponse(
        Long hotelId,
        String name,
        String city,
        String address,
        Double pricePerNight,
        Boolean available
) {}
