package com.hotel.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String FANOUT_EXCHANGE = "booking-orchestration-fanout";
    public static final String QUEUE_NOTIFICATION = "q.notification.orchestration";

    /**
     * Fanout Exchange для уведомлений от оркестратора
     */
    @Bean
    public FanoutExchange orchestrationExchange() {
        return new FanoutExchange(FANOUT_EXCHANGE, true, false);
    }

    /**
     * Queue для получения событий BookingProcessedEvent
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICATION)
                .ttl(60000)  // 60 секунд TTL
                .build();
    }

    /**
     * Binding между Queue и Fanout Exchange
     */
    @Bean
    public Binding notificationBinding(Queue notificationQueue, FanoutExchange orchestrationExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(orchestrationExchange);
    }

    /**
     * Jackson2 Message Converter для десериализации JSON в Java objects
     * КРИТИЧНО для правильной конвертации BookingProcessedEvent!
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();  // Для работы с Java 8+ types (Optional, etc.)
        return new Jackson2JsonMessageConverter(mapper);
    }

    /**
     * RabbitTemplate с Jackson converter для отправки сообщений
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}