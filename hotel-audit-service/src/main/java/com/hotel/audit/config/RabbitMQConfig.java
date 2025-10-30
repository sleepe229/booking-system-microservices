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
    public static final String QUEUE_BOOKING_CONFIRMED = "audit-booking-confirmed-queue";
    public static final String QUEUE_BOOKING_REJECTED = "audit-booking-rejected-queue";

    public static final String ROUTING_KEY_BOOKING_CREATED = "booking.created";
    public static final String ROUTING_KEY_BOOKING_CANCELLED = "booking.cancelled";
    public static final String ROUTING_KEY_BOOKING_CONFIRMED = "booking.confirmed";
    public static final String ROUTING_KEY_BOOKING_REJECTED = "booking.rejected";

    @Bean
    public TopicExchange bookingsExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue queueBookingCreated() {
        return new Queue(QUEUE_BOOKING_CREATED, true);
    }

    @Bean
    public Queue queueBookingCancelled() {
        return new Queue(QUEUE_BOOKING_CANCELLED, true);
    }

    @Bean
    public Queue queueBookingConfirmed() {
        return new Queue(QUEUE_BOOKING_CONFIRMED, true);
    }

    @Bean
    public Queue queueBookingRejected() {
        return new Queue(QUEUE_BOOKING_REJECTED, true);
    }

    @Bean
    public Binding bindingBookingCreated(TopicExchange bookingsExchange) {
        return BindingBuilder.bind(queueBookingCreated()).to(bookingsExchange).with(ROUTING_KEY_BOOKING_CREATED);
    }

    @Bean
    public Binding bindingBookingCancelled(TopicExchange bookingsExchange) {
        return BindingBuilder.bind(queueBookingCancelled()).to(bookingsExchange).with(ROUTING_KEY_BOOKING_CANCELLED);
    }

    @Bean
    public Binding bindingBookingConfirmed(TopicExchange bookingsExchange) {
        return BindingBuilder.bind(queueBookingConfirmed()).to(bookingsExchange).with(ROUTING_KEY_BOOKING_CONFIRMED);
    }

    @Bean
    public Binding bindingBookingRejected(TopicExchange bookingsExchange) {
        return BindingBuilder.bind(queueBookingRejected()).to(bookingsExchange).with(ROUTING_KEY_BOOKING_REJECTED);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}