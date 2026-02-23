package com.jobtracker.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQTopologyConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.queue.deadline}")
    private String deadlineQueue;

    @Value("${app.rabbitmq.queue.deadline-dlq}")
    private String deadlineDlq;

    @Value("${app.rabbitmq.routing-key.deadline}")
    private String deadlineRoutingKey;

    @Bean
    public TopicExchange applicationExchange() {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue deadlineQueue() {
        return QueueBuilder.durable(deadlineQueue)
                .deadLetterExchange("")
                .deadLetterRoutingKey(deadlineDlq)
                .build();
    }

    @Bean
    public Queue deadlineDlqQueue() {
        return QueueBuilder.durable(deadlineDlq).build();
    }

    @Bean
    public Binding deadlineBinding(Queue deadlineQueue, TopicExchange applicationExchange) {
        return BindingBuilder.bind(deadlineQueue).to(applicationExchange).with(deadlineRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }
}
