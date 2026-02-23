package com.jobtracker.notification.messaging;

import com.jobtracker.notification.entity.Notification;
import com.jobtracker.notification.repository.NotificationRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeadlineEventConsumer {

    private final NotificationRepository notificationRepository;

    public DeadlineEventConsumer(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @RabbitListener(queues = "${app.rabbitmq.queue.deadline}")
    @Transactional
    public void handleDeadlineEvent(DeadlineEventPayload payload) {
        Notification notification = new Notification();
        notification.setUserId(payload.userId());
        notification.setType("DEADLINE_REMINDER");
        notification.setTitle("Deadline approaching: " + payload.company());
        notification.setMessage("Your application for " + payload.jobTitle()
                + " at " + payload.company()
                + " has a deadline on " + payload.deadline() + ".");
        notification.setRead(false);
        notificationRepository.save(notification);
    }
}
