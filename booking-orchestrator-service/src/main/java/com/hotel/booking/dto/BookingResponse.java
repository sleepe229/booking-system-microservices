package com.hotel.booking.dto;

import com.hotel.booking.service.BookingOrchestratorService;

public record BookingResponse(
        String bookingId,
        String hotelId,
        String status,
        String customerName,
        String customerEmail,
        String checkIn,
        String checkOut,
        Integer guests,
        Double finalPrice,
        Double discount
) {}