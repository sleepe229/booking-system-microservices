package com.hotel.booking.service;

import com.hotel.booking.dto.BookingResult;
import com.hotel.booking.dto.enums.BookingStatus;
import com.hotel.events.BookingCreatedEvent;
import com.hotel.events.BookingProcessedEvent;
import com.hotel.grpc.discount.*;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class BookingOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(BookingOrchestratorService.class);
    private static final String FANOUT_EXCHANGE = "booking-orchestration-fanout";

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
        if (!idempotencyService.tryAcquire(event.bookingId())) {
            log.warn(" DUPLICATE EVENT IGNORED: bookingId={}", event.bookingId());
            return;
        }

        log.info("Получено событие BookingCreatedEvent: bookingId={}, hotelId={}, nights={}, basePrice={}",
                event.bookingId(), event.hotelId(), event.nights(), event.basePrice());

        try {
            if (!validateBookingEvent(event)) {
                log.warn(" Событие не прошло валидацию: {}", event.bookingId());
                BookingResult result = BookingResult.rejected(
                        event.bookingId(),
                        0.0,
                        "Невалидные данные события"
                );
                publishBookingProcessedEvent(event, result);
                return;
            }

            double basePrice = event.basePrice();
            int nights = event.nights();

            log.info(" Получены данные из Hotel Service: basePrice={}, nights={}, pricePerNight={}",
                    basePrice, nights, event.pricePerNight());

            DiscountRequest discountRequest = DiscountRequest.newBuilder()
                    .setBookingId(event.bookingId())
                    .setHotelId(event.hotelId())
                    .setNights(nights)
                    .setBasePrice(basePrice)
                    .setIsLoyalCustomer(false)
                    .build();

            log.info(" Запрос скидки для booking_id: {}", event.bookingId());

            DiscountResponse discountResponse = discountClient.calculateDiscount(discountRequest);

            if (!discountResponse.getApplied()) {
                log.info(" Скидка не применена: {}", discountResponse.getDiscountReason());
            }

            if (!validateDiscountResponse(discountResponse)) {
                log.error(" Невалидный DiscountResponse для booking_id: {}", event.bookingId());
                BookingResult result = BookingResult.rejected(
                        event.bookingId(),
                        basePrice,
                        "Невалидный ответ о скидке"
                );
                publishBookingProcessedEvent(event, result);
                return;
            }

            log.info(" Получена скидка: {}% ({}), финальная цена: {}",
                    discountResponse.getDiscountPercentage(),
                    discountResponse.getDiscountReason(),
                    discountResponse.getFinalPrice());

            RecommendationRequest recRequest = RecommendationRequest.newBuilder()
                    .setUserId(event.userId())
                    .setHotelId(event.hotelId())
                    .build();

            RecommendationResponse recommendations = discountClient.getRecommendations(recRequest);

            log.info("💡 Получены рекомендации: {} отелей",
                    recommendations.getRecommendedHotelIdsList().size());

            boolean confirmed = discountResponse.getFinalPrice() > 0
                    && discountResponse.getFinalPrice() <= basePrice * 1.5;

            BookingResult result;
            if (confirmed) {
                log.info(" Бронирование ПОДТВЕРЖДЕНО: bookingId={}, finalPrice={}, discount={}%",
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
                log.warn(" Бронирование ОТКЛОНЕНО: bookingId={}, finalPrice={} недопустима " +
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

            publishBookingProcessedEvent(event, result);


        } catch (StatusRuntimeException e) {
            log.error(" gRPC ошибка: status={}, message={}",
                    e.getStatus().getCode(), e.getMessage(), e);

            idempotencyService.release(event.bookingId());

            throw new RuntimeException("gRPC service unavailable, retry needed", e);

        } catch (Exception e) {
            log.error("❌ Неожиданная ошибка: {}", e.getMessage(), e);

            idempotencyService.release(event.bookingId());

            throw new RuntimeException("Unexpected error, retry needed", e);
        }
    }

    private boolean validateBookingEvent(BookingCreatedEvent event) {
        if (event == null) {
            log.warn(" Null event");
            return false;
        }

        if (event.bookingId() == null || event.bookingId().isEmpty()) {
            log.warn(" Пустой bookingId");
            return false;
        }

        if (event.hotelId() == null || event.hotelId().isEmpty()) {
            log.warn(" Пустой hotelId");
            return false;
        }

        if (event.userId() == null || event.userId().isEmpty()) {
            log.warn(" Пустой userId");
            return false;
        }

        if (event.nights() <= 0) {
            log.warn(" Невалидное количество ночей: {}", event.nights());
            return false;
        }

        if (event.basePrice() <= 0) {
            log.warn(" Невалидная базовая цена: {}", event.basePrice());
            return false;
        }

        if (event.pricePerNight() <= 0) {
            log.warn(" Невалидная цена за ночь: {}", event.pricePerNight());
            return false;
        }

        try {
            LocalDate checkIn = LocalDate.parse(event.checkIn());
            LocalDate checkOut = LocalDate.parse(event.checkOut());

            if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
                log.warn(" check-out должен быть после check-in: {} -> {}",
                        event.checkIn(), event.checkOut());
                return false;
            }

            long calculatedNights = ChronoUnit.DAYS.between(checkIn, checkOut);
            if (calculatedNights != event.nights()) {
                log.warn(" Несоответствие nights: в событии {}, рассчитано {}",
                        event.nights(), calculatedNights);
                return false;
            }

        } catch (Exception e) {
            log.warn(" Невалидные даты: checkIn={}, checkOut={}",
                    event.checkIn(), event.checkOut());
            return false;
        }

        return true;
    }

    private boolean validateDiscountResponse(DiscountResponse response) {
        if (response == null) {
            log.error(" Null DiscountResponse");
            return false;
        }

        if (response.getDiscountPercentage() < 0 || response.getDiscountPercentage() > 100) {
            log.error(" Невалидный процент скидки: {}%", response.getDiscountPercentage());
            return false;
        }

        if (response.getFinalPrice() < 0) {
            log.error(" Невалидная финальная цена: {}", response.getFinalPrice());
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
                        event.customerEmail(),
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
                        event.customerEmail(),
                        result.originalPrice(),
                        result.rejectionReason()
                );
            }

            rabbitTemplate.convertAndSend(FANOUT_EXCHANGE, "", processedEvent);

            log.info(" Опубликовано BookingProcessedEvent: bookingId={}, status={}, finalPrice={}, discount={}%",
                    processedEvent.bookingId(),
                    processedEvent.status(),
                    processedEvent.finalPrice(),
                    processedEvent.discountPercentage());

        } catch (Exception e) {
            log.error(" Критическая ошибка публикации BookingProcessedEvent для booking_id: {}",
                    result.bookingId(), e);
            // НЕ пробрасываем исключение - иначе idempotency не сработает
            // Можно добавить retry логику или DLQ
        }
    }

}
