package com.hotel.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE_NAME = "hotel-bookings-exchange";
    public static final String ROUTING_KEY_BOOKING_CREATED = "booking.created";
    public static final String ROUTING_KEY_BOOKING_CANCELLED = "booking.cancelled";

    public static final String DLQ_EXCHANGE = "hotel-bookings-dlx";
    public static final String DLQ_BOOKING_CREATED = "dlq-booking-created";
    public static final String DLQ_BOOKING_CANCELLED = "dlq-booking-cancelled";

    public static final String FANOUT_EXCHANGE = "booking-orchestration-fanout";

    @Bean
    public TopicExchange bookingsExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public DirectExchange dlqExchange() {
        return new DirectExchange(DLQ_EXCHANGE, true, false);
    }

    @Bean
    public Queue dlqQueueBookingCreated() {
        return new Queue(DLQ_BOOKING_CREATED, true);
    }

    @Bean
    public Queue dlqQueueBookingCancelled() {
        return new Queue(DLQ_BOOKING_CANCELLED, true);
    }

    @Bean
    public Binding dlqBindingBookingCreated(DirectExchange dlqExchange) {
        return BindingBuilder.bind(dlqQueueBookingCreated())
                .to(dlqExchange)
                .with("dlq.booking.created");
    }

    @Bean
    public Binding dlqBindingBookingCancelled(DirectExchange dlqExchange) {
        return BindingBuilder.bind(dlqQueueBookingCancelled())
                .to(dlqExchange)
                .with("dlq.booking.cancelled");
    }

    @Bean
    public FanoutExchange orchestrationExchange() {
        return new FanoutExchange(FANOUT_EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
