-- V1__init.sql — auth-service schema initialisation
-- Flyway migration: applied once to auth_db on service startup.
-- Re-applying is safe; Flyway tracks applied versions in flyway_schema_history.

CREATE TABLE users (
    id            BIGINT UNSIGNED    AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(255)       NOT NULL,
    password_hash VARCHAR(255)       NOT NULL,
    created_at    TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
