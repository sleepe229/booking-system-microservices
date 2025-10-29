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

import java.time.LocalDateTime;

@Component
public class BookingEventListener {

    private static final Logger log = LoggerFactory.getLogger(BookingEventListener.class);
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public BookingEventListener(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_BOOKING_CREATED)
    public void onBookingCreated(BookingCreatedEvent event) {
        log.info("Audit: booking created -> bookingId={}, hotelId={}, customer={}, email={}",
                event.bookingId(), event.hotelId(), event.customerName(), event.customerEmail());
        saveAuditLog("BOOKING_CREATED", event, event.bookingId(), event.customerEmail());
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_BOOKING_CANCELLED)
    public void onBookingCancelled(BookingCancelledEvent event) {
        log.info("Audit: booking cancelled -> bookingId={}, customerEmail={}",
                event.bookingId(), event.customerEmail());
        saveAuditLog("BOOKING_CANCELLED", event, event.bookingId(), event.customerEmail());
    }

    @RabbitListener(queues = "audit-booking-confirmed-queue")
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        log.info("Audit: booking confirmed -> bookingId={}, finalPrice={}, discount={}",
                event.bookingId(), event.finalPrice(), event.discount());
        saveAuditLog("BOOKING_CONFIRMED", event, event.bookingId(), event.customerEmail());
    }

    @RabbitListener(queues = "audit-booking-rejected-queue")
    public void onBookingRejected(BookingRejectedEvent event) {
        log.info("Audit: booking rejected -> bookingId={}, reason={}",
                event.bookingId(), event.reason());
        saveAuditLog("BOOKING_REJECTED", event, event.bookingId(), event.customerEmail());
    }

    private void saveAuditLog(String eventType, Object event, Long bookingId, String customerEmail) {
        try {
            AuditLog log = new AuditLog();
            log.setEventType(eventType);
            log.setTimestamp(LocalDateTime.now());
            log.setEventData(objectMapper.writeValueAsString(event));
            log.setBookingId(bookingId);
            log.setCustomerEmail(customerEmail);
            auditLogRepository.save(log);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event data", e);
        }
    }
}