-- V1__init.sql — notification-service schema initialisation
-- Flyway migration: applied once to notification_db on service startup.

-- ---------------------------------------------------------------------------
-- notifications
-- Persisted notification records consumed by notification-service from
-- the notification.applications.deadline RabbitMQ queue.
-- is_read flag drives the unread badge count in the frontend.
-- ---------------------------------------------------------------------------
CREATE TABLE notifications (
    id         BIGINT UNSIGNED   AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT UNSIGNED   NOT NULL,
    type       VARCHAR(50)       NOT NULL,
    title      VARCHAR(255)      NOT NULL,
    message    TEXT              NOT NULL,
    is_read    BOOLEAN           NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Fetch all notifications for a user (newest first via ORDER BY created_at DESC)
    INDEX idx_notifications_user_id (user_id),
    -- Efficient unread count query: WHERE user_id = ? AND is_read = FALSE
    INDEX idx_notifications_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
