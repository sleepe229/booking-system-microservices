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

    private final RabbitTemplate rabbitTemplate;
    private final IdempotencyService idempotencyService;
    private final DiscountClientService discountClient;

    public BookingOrchestratorService(
            RabbitTemplate rabbitTemplate,
            IdempotencyService idempotencyService, DiscountClientService discountClientService) {
        this.rabbitTemplate = rabbitTemplate;
        this.idempotencyService = idempotencyService;
        this.discountClient = discountClientService;
    }

    @RabbitListener(queues = "orchestrator-booking-created-queue")
    public void consumeBookingCreatedEvent(@Payload BookingCreatedEvent event) {
        // ✅ 1. ПЕРВЫМ ДЕЛОМ - проверка на дубликаты
        if (!idempotencyService.tryAcquire(event.bookingId())) {
            log.warn("⚠️ DUPLICATE EVENT IGNORED: bookingId={}", event.bookingId());
            return; // пропускаем повторную обработку
        }

        log.info("Получено событие BookingCreatedEvent: bookingId={}, hotelId={}, nights={}, basePrice={}",
                event.bookingId(), event.hotelId(), event.nights(), event.basePrice());

        try {
            // ✅ 2. Валидация события (теперь проверяем поля из самого события)
            if (!validateBookingEvent(event)) {
                log.warn("❌ Событие не прошло валидацию: {}", event.bookingId());
                BookingResult result = BookingResult.rejected(
                        event.bookingId(),
                        0.0,
                        "Невалидные данные события"
                );
                publishBookingProcessedEvent(event, result);
                return; // ✅ НЕ освобождаем idempotency - событие невалидно навсегда
            }

            // ✅ 3. Используем РЕАЛЬНУЮ цену из события (не генерируем!)
            double basePrice = event.basePrice();
            int nights = event.nights();

            log.info("📦 Получены данные из Hotel Service: basePrice={}, nights={}, pricePerNight={}",
                    basePrice, nights, event.pricePerNight());

            // ✅ 4. Готовим gRPC запрос к discount-service
            DiscountRequest discountRequest = DiscountRequest.newBuilder()
                    .setBookingId(event.bookingId())
                    .setHotelId(event.hotelId())
                    .setNights(nights)                // ✅ из события
                    .setBasePrice(basePrice)          // ✅ реальная цена из БД
                    .setIsLoyalCustomer(false)        // TODO: передавать из event или user service
                    .build();

            log.info("🔄 Запрос скидки для booking_id: {}", event.bookingId());

            DiscountResponse discountResponse = discountClient.calculateDiscount(discountRequest);

            // ✅ Если вернулся fallback, это нормально
            if (!discountResponse.getApplied()) {
                log.info("ℹ️ Скидка не применена: {}", discountResponse.getDiscountReason());
            }

            // ✅ 6. Валидируем ответ от Discount Service
            if (!validateDiscountResponse(discountResponse)) {
                log.error("❌ Невалидный DiscountResponse для booking_id: {}", event.bookingId());
                BookingResult result = BookingResult.rejected(
                        event.bookingId(),
                        basePrice,
                        "Невалидный ответ о скидке"
                );
                publishBookingProcessedEvent(event, result);
                return; // ✅ НЕ освобождаем idempotency
            }

            log.info("✅ Получена скидка: {}% ({}), финальная цена: {}",
                    discountResponse.getDiscountPercentage(),
                    discountResponse.getDiscountReason(),
                    discountResponse.getFinalPrice());

            // ✅ 7. Получаем рекомендации (опционально, без критичности)
            RecommendationRequest recRequest = RecommendationRequest.newBuilder()
                    .setUserId(event.userId())
                    .setHotelId(event.hotelId())
                    .build();

            RecommendationResponse recommendations = discountClient.getRecommendations(recRequest);

            log.info("💡 Получены рекомендации: {} отелей",
                    recommendations.getRecommendedHotelIdsList().size());

            // ✅ 8. Проверяем логику подтверждения
            boolean confirmed = discountResponse.getFinalPrice() > 0
                    && discountResponse.getFinalPrice() <= basePrice * 1.5; // ✅ защита от багов

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
                log.warn("❌ Бронирование ОТКЛОНЕНО: bookingId={}, finalPrice={} недопустима " +
                                "(basePrice={}, превышение допустимого)",
                        event.bookingId(),
                        discountResponse.getFinalPrice(),
                        basePrice);

                result = BookingResult.rejected(
                        event.bookingId(),
                        basePrice,
                        "Некорректная цена от сервиса скидок"
                );
            }

            // ✅ 9. Публикуем результат
            publishBookingProcessedEvent(event, result);

            // ✅ idempotency key остаётся в Redis (TTL 10 мин) - защита от повторной обработки

        } catch (StatusRuntimeException e) {
            log.error("❌ gRPC ошибка: status={}, message={}",
                    e.getStatus().getCode(), e.getMessage(), e);

            // ✅ При gRPC ошибках ОСВОБОЖДАЕМ idempotency для retry
            idempotencyService.release(event.bookingId());

            // Пробрасываем исключение для RabbitMQ retry
            throw new RuntimeException("gRPC service unavailable, retry needed", e);

        } catch (Exception e) {
            log.error("❌ Неожиданная ошибка: {}", e.getMessage(), e);

            // ✅ При неожиданных ошибках ОСВОБОЖДАЕМ idempotency для retry
            idempotencyService.release(event.bookingId());

            // Пробрасываем для retry
            throw new RuntimeException("Unexpected error, retry needed", e);
        }
    }

    /**
     * ✅ ОБНОВЛЁННАЯ валидация - проверяем поля из события
     */
    private boolean validateBookingEvent(BookingCreatedEvent event) {
        if (event == null) {
            log.warn("❌ Null event");
            return false;
        }

        if (event.bookingId() == null || event.bookingId().isEmpty()) {
            log.warn("❌ Пустой bookingId");
            return false;
        }

        if (event.hotelId() == null || event.hotelId().isEmpty()) {
            log.warn("❌ Пустой hotelId");
            return false;
        }

        if (event.userId() == null || event.userId().isEmpty()) {
            log.warn("❌ Пустой userId");
            return false;
        }

        // ✅ Валидация новых полей
        if (event.nights() <= 0) {
            log.warn("❌ Невалидное количество ночей: {}", event.nights());
            return false;
        }

        if (event.basePrice() <= 0) {
            log.warn("❌ Невалидная базовая цена: {}", event.basePrice());
            return false;
        }

        if (event.pricePerNight() <= 0) {
            log.warn("❌ Невалидная цена за ночь: {}", event.pricePerNight());
            return false;
        }

        // Проверка дат
        try {
            LocalDate checkIn = LocalDate.parse(event.checkIn());
            LocalDate checkOut = LocalDate.parse(event.checkOut());

            if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
                log.warn("❌ check-out должен быть после check-in: {} -> {}",
                        event.checkIn(), event.checkOut());
                return false;
            }

            // ✅ Проверяем соответствие nights расчёту из дат
            long calculatedNights = ChronoUnit.DAYS.between(checkIn, checkOut);
            if (calculatedNights != event.nights()) {
                log.warn("❌ Несоответствие nights: в событии {}, рассчитано {}",
                        event.nights(), calculatedNights);
                return false;
            }

        } catch (Exception e) {
            log.warn("❌ Невалидные даты: checkIn={}, checkOut={}",
                    event.checkIn(), event.checkOut());
            return false;
        }

        return true;
    }

    private boolean validateDiscountResponse(DiscountResponse response) {
        if (response == null) {
            log.error("❌ Null DiscountResponse");
            return false;
        }

        if (response.getDiscountPercentage() < 0 || response.getDiscountPercentage() > 100) {
            log.error("❌ Невалидный процент скидки: {}%", response.getDiscountPercentage());
            return false;
        }

        if (response.getFinalPrice() < 0) {
            log.error("❌ Невалидная финальная цена: {}", response.getFinalPrice());
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
                        event.userId(),
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
            log.error("❌ Критическая ошибка публикации BookingProcessedEvent для booking_id: {}",
                    result.bookingId(), e);
            // ✅ НЕ пробрасываем исключение - иначе idempotency не сработает
            // Можно добавить retry логику или DLQ
        }
    }

    // ✅ УДАЛЯЕМ метод generateBasePrice - больше не нужен!
    // private double generateBasePrice(int nights, int guests) { ... }

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
