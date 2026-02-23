package com.jobtracker.application.messaging;

import com.jobtracker.application.entity.JobApplication;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DeadlineEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public DeadlineEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.exchange}") String exchange,
            @Value("${app.rabbitmq.routing-key.deadline}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void publishDeadlineEvent(JobApplication app) {
        DeadlineEventPayload payload = new DeadlineEventPayload(
                app.getId(),
                app.getUserId(),
                app.getCompany(),
                app.getJobTitle(),
                app.getDeadline(),
                "DEADLINE_SET",
                Instant.now().toString()
        );

        rabbitTemplate.convertAndSend(exchange, routingKey, payload);
    }
}
