package com.hotel.booking.config;

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
    public static final String QUEUE_BOOKING_CREATED = "orchestrator-booking-created-queue";
    public static final String ROUTING_KEY_BOOKING_CREATED = "booking.created";

    public static final String FANOUT_EXCHANGE = "booking-orchestration-fanout";

    public static final String DLQ_EXCHANGE = "orchestrator-bookings-dlx";
    public static final String DLQ_QUEUE = "dlq-orchestrator-booking-created";

    @Bean
    public TopicExchange bookingsExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue queueBookingCreated() {
        return QueueBuilder.durable(QUEUE_BOOKING_CREATED)
                .ttl(60000)
                .deadLetterExchange(DLQ_EXCHANGE)
                .deadLetterRoutingKey("dlq.booking.created")
                .build();
    }

    @Bean
    public Binding bindingBookingCreated(TopicExchange bookingsExchange) {
        return BindingBuilder.bind(queueBookingCreated())
                .to(bookingsExchange)
                .with(ROUTING_KEY_BOOKING_CREATED);
    }

    @Bean
    public DirectExchange dlqExchange() {
        return new DirectExchange(DLQ_EXCHANGE, true, false);
    }

    @Bean
    public Queue dlqQueue() {
        return new Queue(DLQ_QUEUE, true);
    }

    @Bean
    public Binding dlqBinding(DirectExchange dlqExchange) {
        return BindingBuilder.bind(dlqQueue())
                .to(dlqExchange)
                .with("dlq.booking.created");
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
