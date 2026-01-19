package com.hotel.listener;

import com.hotel.events.BookingProcessedEvent;
import com.hotel.entity.Booking;
import com.hotel.repo.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class OrchestrationEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrchestrationEventListener.class);
    private final BookingRepository bookingRepository;

    public OrchestrationEventListener(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "q.hotel.orchestration", durable = "true",
                            arguments = @Argument(name = "x-message-ttl", value = "300000", type = "java.lang.Integer")),
                    exchange = @Exchange(name = "booking-orchestration-fanout", type = "fanout")
            )
    )
    @Transactional
    public void handleBookingProcessed(BookingProcessedEvent event) {
        log.info("HOTEL SERVICE: Обработка результата оркестрации для booking_id: {}",
                event.bookingId());
        if (event.bookingId() == null || event.bookingId().isEmpty()) {
            log.error("Пустой bookingId в событии, пропускаем");
            return;
        }
        try {
            Optional<Booking> bookingOpt = bookingRepository.findById(event.bookingId());

            if (bookingOpt.isEmpty()) {
                log.warn("Бронирование {} не найдено в БД.", event.bookingId());
                return;
            }

            Booking booking = bookingOpt.get();
            String oldStatus = booking.getStatus();

            if ("CONFIRMED".equals(event.status())) {
                booking.setStatus("CONFIRMED");
                booking.setFinalPrice(event.finalPrice());
                booking.setDiscount(event.discountPercentage());
                booking.setUserId(event.userId());

                if (event.recommendations() != null && !event.recommendations().isEmpty()) {
                    try {
                        booking.setRecommendations(new com.fasterxml.jackson.databind.ObjectMapper()
                                .writeValueAsString(event.recommendations()));
                    } catch (Exception e) {
                        log.warn("Ошибка сериализации recommendations", e);
                    }
                }

                log.info("Статус: {} → CONFIRMED, цена: {}, скидка: {}%",
                        oldStatus, event.finalPrice(), event.discountPercentage());
            } else if ("REJECTED".equals(event.status())) {
                booking.setStatus("REJECTED");
                booking.setRejectionReason(event.rejectionReason());
                log.info("Статус: {} → REJECTED, причина: {}", oldStatus, event.rejectionReason());
            }

            bookingRepository.save(booking);
            log.info("Бронирование {} успешно обновлено в БД", event.bookingId());

        } catch (Exception e) {
            log.error("Ошибка при обновлении статуса бронирования {}", event.bookingId(), e);
            throw new RuntimeException("Retry this message", e);
        }
    }
}
