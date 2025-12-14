package com.hotel.booking.service;

import com.hotel.events.BookingCreatedEvent;
import com.hotel.events.BookingProcessedEvent;
import com.hotel.grpc.discount.*;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class BookingOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(BookingOrchestratorService.class);
    private static final String FANOUT_EXCHANGE = "booking-orchestration-fanout";
    private static final long GRPC_DEADLINE_SECONDS = 5;
    private static final Random random = new Random();

    @GrpcClient("discount-service")
    private DiscountServiceGrpc.DiscountServiceBlockingStub discountServiceStub;

    private final RabbitTemplate rabbitTemplate;

    public BookingOrchestratorService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "orchestrator-booking-created-queue")
    public void consumeBookingCreatedEvent(
            @Payload BookingCreatedEvent event,
            @Header("guests") int guests,
            @Header("userId") String userId) {

        log.info("Получено событие BookingCreatedEvent: bookingId={}, hotelId={}, guests={}",
                event.bookingId(), event.hotelId(), guests);

        try {
            // ← Валидация событие
            if (!validateBookingEvent(event, guests)) {
                log.warn("Событие не прошло валидацию: {}", event.bookingId());
                BookingResult result = BookingResult.rejected(
                        event.bookingId(),
                        0.0,
                        "Невалидные данные события"
                );
                publishBookingProcessedEvent(event, result);
                return;
            }

            // ← Рассчитываем ночи
            int nights = (int) ChronoUnit.DAYS.between(
                    LocalDate.parse(event.checkIn()),
                    LocalDate.parse(event.checkOut())
            );

            // ← ГЕНЕРИРУЕМ basePrice на оркестраторе
            double basePrice = generateBasePrice(nights, guests);
            log.info("Сгенерирована базовая цена: {} (ночи={}, гости={})",
                    basePrice, nights, guests);

            // ← Готовим gRPC запрос к discount-service
            DiscountRequest discountRequest = DiscountRequest.newBuilder()
                    .setBookingId(event.bookingId())
                    .setHotelId(event.hotelId())
                    .setNights(nights)
                    .setBasePrice(basePrice)
                    .setIsLoyalCustomer(false)  // ← можно добавить в headers
                    .build();

            log.info("Запрос скидки для booking_id: {}", event.bookingId());

            DiscountResponse discountResponse;
            try {
                discountResponse = discountServiceStub
                        .withDeadlineAfter(GRPC_DEADLINE_SECONDS, TimeUnit.SECONDS)
                        .calculateDiscount(discountRequest);
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() == io.grpc.Status.DEADLINE_EXCEEDED.getCode()) {
                    log.error("TIMEOUT: Discount service не ответил за {} сек", GRPC_DEADLINE_SECONDS);
                    BookingResult result = BookingResult.rejected(
                            event.bookingId(),
                            basePrice,
                            "Timeout при расчёте скидки"
                    );
                    publishBookingProcessedEvent(event, result);
                    return;
                }
                throw e;
            }

            // ← Валидируем ответ скидок
            if (!validateDiscountResponse(discountResponse)) {
                log.error("Невалидный DiscountResponse для booking_id: {}", event.bookingId());
                BookingResult result = BookingResult.rejected(
                        event.bookingId(),
                        basePrice,
                        "Невалидный ответ о скидке"
                );
                publishBookingProcessedEvent(event, result);
                return;
            }

            log.info("Получена скидка: {}% ({}), финальная цена: {}",
                    discountResponse.getDiscountPercentage(),
                    discountResponse.getDiscountReason(),
                    discountResponse.getFinalPrice());

            // ← Получаем рекомендации (опционально)
            RecommendationRequest recRequest = RecommendationRequest.newBuilder()
                    .setUserId(event.userId())
                    .setHotelId(event.hotelId())
                    .build();

            RecommendationResponse recommendations;
            try {
                recommendations = discountServiceStub
                        .withDeadlineAfter(GRPC_DEADLINE_SECONDS, TimeUnit.SECONDS)
                        .getRecommendations(recRequest);
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() == io.grpc.Status.DEADLINE_EXCEEDED.getCode()) {
                    log.warn("TIMEOUT при получении рекомендаций для booking_id: {}", event.bookingId());
                    recommendations = RecommendationResponse.getDefaultInstance();
                } else {
                    throw e;
                }
            }

            log.info("Получены рекомендации: {} отелей", recommendations.getRecommendedHotelIdsList().size());

            // ← Проверяем логику подтверждения
            boolean confirmed = discountResponse.getFinalPrice() > 0
                    && discountResponse.getFinalPrice() <= basePrice * 2;

            BookingResult result;
            if (confirmed) {
                log.info("✅ Бронирование ПОДТВЕРЖДЕНО: bookingId={}, finalPrice={}, discount={}%",
                        event.bookingId(),
                        discountResponse.getFinalPrice(),
                        discountResponse.getDiscountPercentage());

                result = BookingResult.confirmed(
                        event.bookingId(),
                        basePrice,
                        discountResponse.getFinalPrice(),
                        discountResponse.getDiscountPercentage(),
                        discountResponse.getDiscountReason(),
                        recommendations.getRecommendedHotelIdsList()
                );
            } else {
                log.warn("❌ Бронирование ОТКЛОНЕНО: bookingId={}, finalPrice={} недопустима",
                        event.bookingId(),
                        discountResponse.getFinalPrice());

                result = BookingResult.rejected(
                        event.bookingId(),
                        basePrice,
                        "Некорректная цена от сервиса скидок"
                );
            }

            publishBookingProcessedEvent(event, result);

        } catch (StatusRuntimeException e) {
            log.error("❌ gRPC ошибка: status={}, message={}", e.getStatus().getCode(), e.getMessage(), e);
            BookingResult result = BookingResult.rejected(
                    event.bookingId(),
                    0.0,
                    "Сервис скидок недоступен: " + e.getStatus().getCode()
            );
            publishBookingProcessedEvent(event, result);

        } catch (Exception e) {
            log.error("❌ Неожиданная ошибка: {}", e.getMessage(), e);
            BookingResult result = BookingResult.rejected(
                    event.bookingId(),
                    0.0,
                    "Внутренняя ошибка: " + e.getClass().getSimpleName()
            );
            publishBookingProcessedEvent(event, result);
        }
    }

    private double generateBasePrice(int nights, int guests) {
        int pricePerNightPerGuest = 50 + random.nextInt(100);  // [50, 150)
        double basePrice = nights * guests * pricePerNightPerGuest;
        log.debug("Генерация цены: {} ночей × {} гостей × {} = {}",
                nights, guests, pricePerNightPerGuest, basePrice);
        return basePrice;
    }

    private boolean validateDiscountResponse(DiscountResponse response) {
        if (response.getDiscountPercentage() < 0 || response.getDiscountPercentage() > 100) {
            log.error("Невалидный процент скидки: {}%", response.getDiscountPercentage());
            return false;
        }
        if (response.getFinalPrice() < 0) {
            log.error("Невалидная финальная цена: {}", response.getFinalPrice());
            return false;
        }
        return true;
    }

    private boolean validateBookingEvent(BookingCreatedEvent event, int guests) {
        if (event == null || event.bookingId() == null || event.bookingId().isEmpty()) {
            log.warn("Пустой bookingId");
            return false;
        }
        if (event.hotelId() == null || event.hotelId().isEmpty()) {
            log.warn("Пустой hotelId");
            return false;
        }
        if (guests <= 0) {
            log.warn("Невалидное количество гостей: {}", guests);
            return false;
        }
        try {
            LocalDate.parse(event.checkIn());
            LocalDate.parse(event.checkOut());
        } catch (Exception e) {
            log.warn("Невалидные даты: checkIn={}, checkOut={}", event.checkIn(), event.checkOut());
            return false;
        }
        return true;
    }

    private void publishBookingProcessedEvent(BookingCreatedEvent event, BookingResult result) {
        try {
            BookingProcessedEvent processedEvent;

            if (result.status() == BookingStatus.CONFIRMED) {
                processedEvent = BookingProcessedEvent.confirmed(
                        result.bookingId(),
                        event.userId(),
                        event.hotelId(),
                        result.originalPrice(),
                        result.finalPrice(),
                        result.discountPercentage(),
                        result.discountReason(),
                        result.recommendations()
                );
            } else {
                processedEvent = BookingProcessedEvent.rejected(
                        result.bookingId(),
                        event.userId(),  // ← userId нет, передаём null
                        event.hotelId(),
                        result.originalPrice(),
                        result.rejectionReason()
                );
            }

            rabbitTemplate.convertAndSend(FANOUT_EXCHANGE, "", processedEvent);

            log.info("✅ Опубликовано BookingProcessedEvent: bookingId={}, status={}, finalPrice={}, discount={}%",
                    processedEvent.bookingId(),
                    processedEvent.status(),
                    processedEvent.finalPrice(),
                    processedEvent.discountPercentage());

        } catch (Exception e) {
            log.error("❌ Ошибка публикации BookingProcessedEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish BookingProcessedEvent", e);
        }
    }

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

    public enum BookingStatus {
        CONFIRMED, REJECTED
    }
}
