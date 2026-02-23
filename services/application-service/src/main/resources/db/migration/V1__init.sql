-- V1__init.sql — application-service schema initialisation
-- Flyway migration: applied once to application_db on service startup.

-- ---------------------------------------------------------------------------
-- job_applications
-- Core entity. One row per job a user is tracking.
-- status ENUM drives UI grouping and deadline notification triggers.
-- ---------------------------------------------------------------------------
CREATE TABLE job_applications (
    id           BIGINT UNSIGNED   AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT UNSIGNED   NOT NULL,
    company      VARCHAR(255)      NOT NULL,
    job_title    VARCHAR(255)      NOT NULL,
    location     VARCHAR(255)      NULL,
    job_post_url VARCHAR(2048)     NULL,
    deadline     DATE              NULL,
    applied_date DATE              NULL,
    status       ENUM(
                     'SAVED',
                     'APPLIED',
                     'OA',
                     'INTERVIEW',
                     'REJECTED',
                     'OFFER'
                 )                 NOT NULL DEFAULT 'SAVED',
    notes        TEXT              NULL,
    created_at   TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Lookup by owner (primary access pattern)
    INDEX idx_applications_user_id (user_id),
    -- Dashboard grouping by status within a user's applications
    INDEX idx_applications_user_status (user_id, status),
    -- Deadline reminder queries: find upcoming deadlines per user
    INDEX idx_applications_user_deadline (user_id, deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- resume_targeting_notes
-- One-to-one extension of job_applications.
-- Stores AI-assist / manual keyword targeting notes per application.
-- Cascades delete so orphaned notes are never left behind.
-- ---------------------------------------------------------------------------
CREATE TABLE resume_targeting_notes (
    id                    BIGINT UNSIGNED   AUTO_INCREMENT PRIMARY KEY,
    application_id        BIGINT UNSIGNED   NOT NULL,
    must_have_keywords    TEXT              NULL,
    nice_to_have_keywords TEXT              NULL,
    custom_bullet_ideas   TEXT              NULL,
    job_description_excerpt TEXT            NULL,
    match_notes           TEXT              NULL,
    created_at            TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Enforce one targeting note per application
    CONSTRAINT uq_targeting_note_application
        UNIQUE (application_id),
    -- FK with cascade delete: removing an application removes its targeting note
    CONSTRAINT fk_targeting_note_application
        FOREIGN KEY (application_id)
        REFERENCES job_applications (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
