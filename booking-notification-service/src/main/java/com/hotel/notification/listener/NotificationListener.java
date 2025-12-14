package com.hotel.notification.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.events.BookingProcessedEvent;
import com.hotel.notification.websocket.NotificationWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final NotificationWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    public NotificationListener(NotificationWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
        this.objectMapper = new ObjectMapper();
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "q.notification.orchestration", durable = "true"),
                    exchange = @Exchange(name = "booking-orchestration-fanout", type = "fanout")
            )
    )
    @Transactional
    public void handleBookingProcessed(BookingProcessedEvent event) {
        log.info(" NOTIFICATION: Получено событие для booking_id: {}, userId: {}",
                event.bookingId(), event.userId());

        try {
            if ("CONFIRMED".equals(event.status())) {
                sendConfirmationEmail(event);
                sendConfirmationPush(event);
            } else {
                sendRejectionEmail(event);
            }

            sendWebSocketNotification(event);

            log.info(" Все уведомления отправлены для booking_id: {}", event.bookingId());

        } catch (Exception e) {
            log.error(" Ошибка при обработке события NotificationListener", e);
        }
    }

    private void sendWebSocketNotification(BookingProcessedEvent event) {
        try {
            String message = buildWebSocketMessage(event);

            boolean sent = webSocketHandler.sendToUser(event.userId(), message);

            if (sent) {
                log.info(" WebSocket уведомление доставлено userId: {}", event.userId());
            } else {
                log.warn(" Пользователь userId: {} не подключен к WebSocket (нормально, если оффлайн)",
                        event.userId());
            }
        } catch (JsonProcessingException e) {
            log.error(" Ошибка формирования JSON для WebSocket", e);
        }
    }

    private String buildWebSocketMessage(BookingProcessedEvent event) throws JsonProcessingException {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "BOOKING_UPDATE");
        notification.put("bookingId", event.bookingId());
        notification.put("status", event.status());
        notification.put("userId", event.userId());
        notification.put("hotelId", event.hotelId());
        notification.put("finalPrice", event.finalPrice());
        notification.put("discountPercentage", event.discountPercentage());
        notification.put("timestamp", System.currentTimeMillis());

        if ("CONFIRMED".equals(event.status())) {
            notification.put("message", String.format(
                    " Ваше бронирование %s подтверждено! Финальная цена: %.2f (экономия: %.0f%%)",
                    event.bookingId(), event.finalPrice(), event.discountPercentage()
            ));

            if (event.recommendations() != null && !event.recommendations().isEmpty()) {
                notification.put("recommendations", event.recommendations());
            }
        } else {
            notification.put("message", String.format(
                    " Бронирование %s не может быть подтверждено. Причина: %s",
                    event.bookingId(), event.rejectionReason()
            ));
            notification.put("rejectionReason", event.rejectionReason());
        }

        return objectMapper.writeValueAsString(notification);
    }

    private void sendConfirmationEmail(BookingProcessedEvent event) {
        log.info("   EMAIL отправлен для booking_id: {}", event.bookingId());
        log.info("   Тема: Ваше бронирование {} подтверждено!", event.bookingId());
        log.info("   Финальная цена: {} (экономия: {}%)",
                event.finalPrice(), event.discountPercentage());
    }

    private void sendConfirmationPush(BookingProcessedEvent event) {
        log.info("   PUSH уведомление отправлено userId: {}", event.userId());
        log.info("   Сообщение: Бронирование {} в отеле {} подтверждено! Скидка: {}%",
                event.bookingId(), event.hotelId(), event.discountPercentage());
    }

    private void sendRejectionEmail(BookingProcessedEvent event) {
        log.info("   EMAIL (отклонение) отправлен для booking_id: {}", event.bookingId());
        log.info("   Причина: {}", event.rejectionReason());
    }
}
