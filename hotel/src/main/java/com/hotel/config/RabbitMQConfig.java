package com.hotel.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE_NAME = "hotel-bookings-exchange";
    public static final String ROUTING_KEY_BOOKING_CREATED = "booking.created";
    public static final String ROUTING_KEY_BOOKING_CANCELLED = "booking.cancelled";

    @Bean
    public TopicExchange bookingsExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}