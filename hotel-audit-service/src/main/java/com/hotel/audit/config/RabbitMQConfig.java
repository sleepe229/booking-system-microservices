package com.hotel.audit.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "hotel-bookings-exchange";
    public static final String QUEUE_BOOKING_CREATED = "audit-booking-created-queue";
    public static final String QUEUE_BOOKING_CANCELLED = "audit-booking-cancelled-queue";
    public static final String QUEUE_BOOKING_PAID = "audit-booking-paid-queue";
    public static final String ROUTING_KEY_BOOKING_CREATED = "booking.created";
    public static final String ROUTING_KEY_BOOKING_CANCELLED = "booking.cancelled";
    public static final String ROUTING_KEY_BOOKING_PAID = "booking.paid";

    public static final String ORCHESTRATION_EXCHANGE = "booking-orchestration-fanout";
    public static final String ORCHESTRATION_QUEUE_AUDIT = "q.audit.orchestration";

    @Bean
    public TopicExchange bookingsExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue queueBookingCreated() {
        return QueueBuilder.durable(QUEUE_BOOKING_CREATED).build();
    }

    @Bean
    public Queue queueBookingCancelled() {
        return QueueBuilder.durable(QUEUE_BOOKING_CANCELLED).build();
    }

    @Bean
    public Queue queueBookingPaid() {
        return QueueBuilder.durable(QUEUE_BOOKING_PAID).build();
    }

    @Bean
    public Binding bindingBookingCreated(TopicExchange bookingsExchange) {
        return BindingBuilder.bind(queueBookingCreated())
                .to(bookingsExchange)
                .with(ROUTING_KEY_BOOKING_CREATED);
    }

    @Bean
    public Binding bindingBookingCancelled(TopicExchange bookingsExchange) {
        return BindingBuilder.bind(queueBookingCancelled())
                .to(bookingsExchange)
                .with(ROUTING_KEY_BOOKING_CANCELLED);
    }

    @Bean
    public Binding bindingBookingPaid(TopicExchange bookingsExchange) {
        return BindingBuilder.bind(queueBookingPaid())
                .to(bookingsExchange)
                .with(ROUTING_KEY_BOOKING_PAID);
    }

    @Bean
    public FanoutExchange orchestrationExchange() {
        return new FanoutExchange(ORCHESTRATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue orchestrationAuditQueue() {
        return QueueBuilder.durable(ORCHESTRATION_QUEUE_AUDIT).build();
    }

    @Bean
    public Binding bindAuditFanout() {
        return BindingBuilder.bind(orchestrationAuditQueue())
                .to(orchestrationExchange());
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
