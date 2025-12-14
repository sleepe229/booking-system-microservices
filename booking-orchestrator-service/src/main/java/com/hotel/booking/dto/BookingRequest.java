package com.hotel.booking.dto;

public record BookingRequest(
        String bookingId,
        String userId,
        String hotelId,
        int nights,
        double basePrice,
        boolean isLoyalCustomer
) {}
