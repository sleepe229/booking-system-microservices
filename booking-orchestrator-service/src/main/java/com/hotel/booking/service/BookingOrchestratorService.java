package com.hotel.booking.service;

import com.hotel.grpc.discount.*;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BookingOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(BookingOrchestratorService.class);

    @GrpcClient("discount-service")
    private DiscountServiceGrpc.DiscountServiceBlockingStub discountServiceStub;

    public BookingResult processBooking(BookingRequest bookingRequest) {
        log.info("Начата обработка бронирования: {}", bookingRequest);

        try {
            if (!validateBooking(bookingRequest)) {
                log.warn("Бронирование не прошло валидацию: {}", bookingRequest.bookingId());
                return BookingResult.rejected(
                        bookingRequest.bookingId(),
                        bookingRequest.basePrice(),
                        "Бронирование не прошло валидацию"
                );
            }

            DiscountRequest discountRequest = DiscountRequest.newBuilder()
                    .setBookingId(bookingRequest.bookingId())
                    .setUserId(bookingRequest.userId())
                    .setHotelId(bookingRequest.hotelId())
                    .setNights(bookingRequest.nights())
                    .setBasePrice(bookingRequest.basePrice())
                    .setIsLoyalCustomer(bookingRequest.isLoyalCustomer())
                    .build();

            log.info("Запрос скидки для booking_id: {}", bookingRequest.bookingId());
            DiscountResponse discountResponse = discountServiceStub.calculateDiscount(discountRequest);

            log.info("Получен ответ о скидке: {}% ({}), финальная цена: {}",
                    discountResponse.getDiscountPercentage(),
                    discountResponse.getDiscountReason(),
                    discountResponse.getFinalPrice());

            RecommendationRequest recRequest = RecommendationRequest.newBuilder()
                    .setUserId(bookingRequest.userId())
                    .setHotelId(bookingRequest.hotelId())
                    .build();

            RecommendationResponse recommendations = discountServiceStub.getRecommendations(recRequest);
            log.info("Получены рекомендации: {}", recommendations.getRecommendedHotelIdsList());

            boolean confirmed = discountResponse.getFinalPrice() > 0;

            if (confirmed) {
                log.info("Бронирование подтверждено: {}", bookingRequest.bookingId());
                return BookingResult.confirmed(
                        bookingRequest.bookingId(),
                        bookingRequest.basePrice(),
                        discountResponse.getFinalPrice(),
                        discountResponse.getDiscountPercentage(),
                        discountResponse.getDiscountReason(),
                        recommendations.getRecommendedHotelIdsList()
                );
            } else {
                log.warn("Бронирование отклонено: {}", bookingRequest.bookingId());
                return BookingResult.rejected(
                        bookingRequest.bookingId(),
                        bookingRequest.basePrice(),
                        "Некорректная цена"
                );
            }

        } catch (StatusRuntimeException e) {
            log.error("Ошибка gRPC при обработке бронирования: {}", e.getStatus(), e);
            return BookingResult.rejected(
                    bookingRequest.bookingId(),
                    bookingRequest.basePrice(),
                    "Ошибка при расчете скидки: " + e.getStatus()
            );
        } catch (Exception e) {
            log.error("Неожиданная ошибка при обработке бронирования", e);
            return BookingResult.rejected(
                    bookingRequest.bookingId(),
                    bookingRequest.basePrice(),
                    "Внутренняя ошибка сервиса"
            );
        }
    }

    private boolean validateBooking(BookingRequest request) {
        return request.nights() > 0 && request.basePrice() > 0;
    }

    public record BookingRequest(
            String bookingId,
            String userId,
            String hotelId,
            int nights,
            double basePrice,
            boolean isLoyalCustomer
    ) {}

    public record BookingResult(
            String bookingId,
            BookingStatus status,
            double originalPrice,
            double finalPrice,
            double discountPercentage,
            String discountReason,
            String rejectionReason,
            java.util.List<String> recommendations
    ) {
        public static BookingResult confirmed(String bookingId, double originalPrice,
                                              double finalPrice, double discountPercentage,
                                              String discountReason,
                                              java.util.List<String> recommendations) {
            return new BookingResult(bookingId, BookingStatus.CONFIRMED, originalPrice,
                    finalPrice, discountPercentage, discountReason, null, recommendations);
        }

        public static BookingResult rejected(String bookingId, double originalPrice, String reason) {
            return new BookingResult(bookingId, BookingStatus.REJECTED, originalPrice,
                    0, 0, null, reason, null);
        }

        public String message() {
            if (status == BookingStatus.CONFIRMED) {
                return String.format("Бронирование подтверждено. Скидка: %.2f%%. %s",
                        discountPercentage, discountReason);
            } else {
                return String.format("Бронирование отклонено. Причина: %s", rejectionReason);
            }
        }
    }

    public enum BookingStatus {
        CONFIRMED, REJECTED
    }
}