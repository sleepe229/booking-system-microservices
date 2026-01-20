package com.hotel.booking.dto;

import com.hotel.booking.dto.enums.BookingStatus;

import java.util.List;

public record BookingResult(
        String bookingId,
        BookingStatus status,
        double originalPrice,
        double finalPrice,
        double discountPercentage,
        String discountReason,
        String rejectionReason,
        List<String> recommendations
) {
    public static BookingResult confirmed(
            String bookingId,
            double originalPrice,
            double finalPrice,
            double discountPercentage,
            String discountReason,
            List<String> recommendations
    ) {
        return new BookingResult(
                bookingId,
                BookingStatus.CONFIRMED,
                originalPrice,
                finalPrice,
                discountPercentage,
                discountReason,
                null,
                recommendations
        );
    }

    public static BookingResult rejected(
            String bookingId,
            double originalPrice,
            String rejectionReason
    ) {
        return new BookingResult(
                bookingId,
                BookingStatus.REJECTED,
                originalPrice,
                0.0,
                0.0,
                null,
                rejectionReason,
                null
        );
    }
}
