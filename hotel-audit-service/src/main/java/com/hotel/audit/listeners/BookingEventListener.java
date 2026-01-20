package com.hotel.audit.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.audit.config.RabbitMQConfig;
import com.hotel.audit.entity.AuditLog;
import com.hotel.audit.repo.AuditLogRepository;
import com.hotel.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class BookingEventListener {

    private static final Logger log = LoggerFactory.getLogger(BookingEventListener.class);
    private final AuditLogRepository repo;
    private final ObjectMapper mapper;

    public BookingEventListener(AuditLogRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_BOOKING_CREATED)
    public void onBookingCreated(BookingCreatedEvent event) {
        log.info("Audit: booking created -> {}", event);
        save("BOOKING_CREATED", event, event.bookingId(), event.customerEmail());
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_BOOKING_CANCELLED)
    public void onBookingCancelled(BookingCancelledEvent event) {
        log.info("Audit: booking cancelled -> {}", event);
        save("BOOKING_CANCELLED", event, event.bookingId(), event.customerEmail());
    }

    @RabbitListener(queues = RabbitMQConfig.ORCHESTRATION_QUEUE_AUDIT)
    public void onBookingProcessed(BookingProcessedEvent event) {

        String time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(event.timestamp()));

        log.info("AUDIT ORCHESTRATION: booking processed -> bookingId={}, status={}, time={}",
                event.bookingId(), event.status(), time);

        String type = event.status().equals("CONFIRMED")
                ? "BOOKING_CONFIRMED"
                : "BOOKING_REJECTED";

        save(type, event, event.bookingId(), event.customerEmail());
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_BOOKING_PAID)
    public void onBookingPaid(BookingPaidEvent event) {
        log.info(" Audit: booking paid -> bookingId={}, amount={}, method={}",
                event.bookingId(), event.finalPrice(), event.paymentMethod());
        save("BOOKING_PAID", event, event.bookingId(), event.customerEmail());
    }



    private void save(String type, Object event, String id, String emailOrUserId) {
        try {
            AuditLog log = new AuditLog();
            log.setEventType(type);
            log.setTimestamp(LocalDateTime.now());
            log.setBookingId(id);

            if (emailOrUserId != null && !emailOrUserId.isEmpty()) {
                log.setCustomerEmail(emailOrUserId);
            }

            log.setEventData(mapper.writeValueAsString(event));
            repo.save(log);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit event", e);
        }
    }
}