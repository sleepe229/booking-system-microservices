package com.hotel.events;

import java.io.Serializable;
import java.util.List;

/**
 * Событие, публикуемое после обработки бронирования оркестратором
 */
public record BookingProcessedEvent(
        String bookingId,
        String userId,
        String hotelId,
        String status,  // CONFIRMED или REJECTED
        double originalPrice,
        double finalPrice,
        double discountPercentage,
        String discountReason,
        String rejectionReason,
        List<String> recommendations,
        long timestamp
) implements Serializable {

    public static BookingProcessedEvent confirmed(
            String bookingId,
            String userId,
            String hotelId,
            double originalPrice,
            double finalPrice,
            double discountPercentage,
            String discountReason,
            List<String> recommendations) {
        return new BookingProcessedEvent(
                bookingId,
                userId,
                hotelId,
                "CONFIRMED",
                originalPrice,
                finalPrice,
                discountPercentage,
                discountReason,
                null,
                recommendations,
                System.currentTimeMillis()
        );
    }

    public static BookingProcessedEvent rejected(
            String bookingId,
            String userId,
            String hotelId,
            double originalPrice,
            String rejectionReason) {
        return new BookingProcessedEvent(
                bookingId,
                userId,
                hotelId,
                "REJECTED",
                originalPrice,
                0.0,
                0.0,
                null,
                rejectionReason,
                null,
                System.currentTimeMillis()
        );
    }
}