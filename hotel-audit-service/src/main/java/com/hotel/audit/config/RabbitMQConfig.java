package com.hotel.audit.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "hotel-bookings-exchange";

    public static final String QUEUE_BOOKING_CREATED = "audit-booking-created-queue";
    public static final String QUEUE_BOOKING_CANCELLED = "audit-booking-cancelled-queue";
    public static final String QUEUE_BOOKING_CONFIRMED = "audit-booking-confirmed-queue";
    public static final String QUEUE_BOOKING_REJECTED = "audit-booking-rejected-queue";
    public static final String QUEUE_PAYMENT_CONFIRMED = "audit-payment-confirmed-queue";

    @Bean
    public TopicExchange bookingsExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue bookingCreatedQueue() {
        return QueueBuilder.durable(QUEUE_BOOKING_CREATED).build();
    }

    @Bean
    public Queue bookingCancelledQueue() {
        return QueueBuilder.durable(QUEUE_BOOKING_CANCELLED).build();
    }

    @Bean
    public Queue bookingConfirmedQueue() {
        return QueueBuilder.durable(QUEUE_BOOKING_CONFIRMED).build();
    }

    @Bean
    public Queue bookingRejectedQueue() {
        return QueueBuilder.durable(QUEUE_BOOKING_REJECTED).build();
    }

    @Bean
    public Queue paymentConfirmedQueue() {
        return QueueBuilder.durable(QUEUE_PAYMENT_CONFIRMED).build();
    }

    @Bean
    public Binding bindingBookingCreated(@Qualifier("bookingCreatedQueue") Queue queue,
                                         TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("booking.created");
    }

    @Bean
    public Binding bindingBookingCancelled(@Qualifier("bookingCancelledQueue") Queue queue,
                                           TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("booking.cancelled");
    }

    @Bean
    public Binding bindingBookingConfirmed(@Qualifier("bookingConfirmedQueue") Queue queue,
                                           TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("booking.confirmed");
    }

    @Bean
    public Binding bindingBookingRejected(@Qualifier("bookingRejectedQueue") Queue queue,
                                          TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("booking.rejected");
    }

    @Bean
    public Binding bindingPaymentConfirmed(@Qualifier("paymentConfirmedQueue") Queue queue,
                                           TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("payment.confirmed");
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultClassMapper classMapper = new DefaultClassMapper();
        classMapper.setTrustedPackages("com.hotel.events");
        converter.setClassMapper(classMapper);
        return converter;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jackson2JsonMessageConverter);
        return factory;
    }
}