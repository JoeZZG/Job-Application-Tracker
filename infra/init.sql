-- init.sql
-- Executed once by the MySQL Docker entrypoint on first container startup.
-- Creates all three application databases if they do not already exist.
-- The MYSQL_USER defined in docker-compose is granted full privileges on each.
--
-- Re-running is safe: all statements are idempotent via IF NOT EXISTS.

CREATE DATABASE IF NOT EXISTS auth_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS application_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS notification_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Grant the application user full access to all three schemas.
-- ROOT credentials are used only by Docker health checks and DBA operations.
GRANT ALL PRIVILEGES ON auth_db.*        TO '${MYSQL_USER}'@'%';
GRANT ALL PRIVILEGES ON application_db.* TO '${MYSQL_USER}'@'%';
GRANT ALL PRIVILEGES ON notification_db.* TO '${MYSQL_USER}'@'%';

FLUSH PRIVILEGES;
