# services/notification-service/

## Purpose

Spring Boot 3.2.1 service that **consumes** deadline events from RabbitMQ and persists them as `Notification` records. Exposes a REST API for listing notifications and marking them as read. Makes **no outbound HTTP calls** to any other service.

Port: **8083**
Database: **notification_db** (RDS MySQL)
Messaging: **RabbitMQ** consumer (Amazon MQ)

## Key Files

| File | Role |
|---|---|
| `messaging/DeadlineEventConsumer.java` | `@RabbitListener` — converts `DeadlineEventPayload` to `Notification` entity and persists it |
| `config/RabbitMQTopologyConfig.java` | Declares queue, DLQ, exchange binding, retry policy (3 attempts, exponential backoff) |
| `messaging/DeadlineEventPayload.java` | Record matching the payload published by application-service |
| `controller/NotificationController.java` | `GET /notifications`, `PATCH /notifications/{id}/read` |
| `service/NotificationService.java` | `listForUser()`, `markAsRead()` with ownership check |
| `src/main/resources/application.yml` | RabbitMQ consumer config, retry settings, MySQL connection |
| `src/main/resources/db/migration/V1__init.sql` | Flyway: creates `notifications` table |

## Structure

```
notification-service/
├── src/main/java/com/jobtracker/notification/
│   ├── NotificationServiceApplication.java
│   ├── controller/
│   │   └── NotificationController.java
│   ├── service/
│   │   └── NotificationService.java
│   ├── repository/
│   │   └── NotificationRepository.java
│   ├── entity/
│   │   └── Notification.java           # id, userId, type, title, message, is_read, created_at
│   ├── dto/
│   │   └── NotificationResponse.java
│   ├── config/
│   │   └── RabbitMQTopologyConfig.java # Queue, DLQ, exchange binding, retry
│   ├── messaging/
│   │   ├── DeadlineEventConsumer.java  # @RabbitListener
│   │   └── DeadlineEventPayload.java   # Must match application-service exactly
│   ├── security/
│   │   ├── SecurityConfig.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── JwtUtil.java
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       ├── ErrorResponse.java
│       ├── ForbiddenException.java
│       └── ResourceNotFoundException.java
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/V1__init.sql
├── src/test/java/com/jobtracker/notification/
│   ├── NotificationServiceApplicationTests.java
│   ├── controller/NotificationControllerTest.java
│   └── messaging/
│       ├── DeadlineEventConsumerTest.java
│       └── service/NotificationServiceTest.java
├── Dockerfile
├── .dockerignore
└── pom.xml
```

## Implementation Overview

### RabbitMQ Consumer
- `@RabbitListener(queues = "${app.rabbitmq.queue.deadline}")` on `DeadlineEventConsumer.consume()`.
- Queue name resolved from env var (default `notification.applications.deadline`).
- Converts JSON message to `DeadlineEventPayload` via `Jackson2JsonMessageConverter` (configured in `RabbitMQTopologyConfig`).
- Creates a `Notification` entity from the payload and calls `notificationRepository.save()`.
- `@Transactional` — DB save is atomic; message is only acked after successful commit.

### Dead Letter Queue
- Queue: `notification.applications.deadline` (main)
- DLQ: `notification.applications.deadline.dlq`
- On consume failure: retried 3 times with exponential backoff (1s, 2s, 4s).
- After exhausted retries: message routed to DLQ for manual inspection.
- **Never silently discard messages** — DLQ preserves them.

### REST API
- `GET /notifications` — returns all notifications for the authenticated user (ordered by `created_at DESC`).
- `PATCH /notifications/{id}/read` — marks a specific notification as read; verifies ownership (403 if not owner).
- Both endpoints require valid JWT (validated locally via `JwtAuthenticationFilter`).

## Implementation Details & Gotchas

- **`DeadlineEventPayload` must match application-service**: both services define the same `DeadlineEventPayload` record independently. If application-service changes the payload shape (field name, type), this service's deserialization will silently null out mismatched fields, or throw and route to DLQ. Keep both in sync.
- **No outbound HTTP**: notification-service is intentionally isolated — it never calls auth-service, application-service, or any external HTTP endpoint. The `userId` comes from the RabbitMQ message payload, not from a JWT lookup.
- **REST endpoints do use JWT**: when users query `GET /notifications` via the browser, the request includes a JWT (forwarded by the gateway). The service validates the JWT locally to extract `userId` for scoping the query.
- **Consumer idempotency**: if a message is delivered twice (e.g., after a broker failover), two identical `Notification` rows will be inserted. There is no deduplication key. Acceptable for this use case, but note for production review.
- **`ddl-auto: validate`**: Flyway owns the schema. New notification types require updating the `type` field handling in the consumer.
- **JWT_SECRET**: same secret as auth-service and application-service, injected from AWS Secrets Manager.

## Dependencies

Key `pom.xml` dependencies (Spring Boot 3.2.1):
```xml
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-validation
spring-boot-starter-amqp
spring-boot-starter-actuator
mysql-connector-j (runtime)
flyway-core + flyway-mysql
jjwt-api 0.12.3 + jjwt-impl + jjwt-jackson
springdoc-openapi-starter-webmvc-ui 2.3.0
```

## Usage

```bash
# Requires MySQL (notification_db) and RabbitMQ
export MYSQL_HOST=localhost MYSQL_PORT=3306
export MYSQL_DATABASE=notification_db
export MYSQL_USERNAME=root MYSQL_PASSWORD=<pass>
export JWT_SECRET=<hex>
export RABBITMQ_HOST=localhost RABBITMQ_PORT=5672
export RABBITMQ_USERNAME=guest RABBITMQ_PASSWORD=guest

mvn spring-boot:run
# → http://localhost:8083
# → http://localhost:8083/swagger-ui.html
```

RabbitMQ queue and DLQ are auto-declared on startup via `RabbitMQTopologyConfig` beans. The exchange (`applications.topic`) must already exist (declared by application-service on its startup).

## Testing

```bash
mvn test
# NotificationControllerTest — HTTP endpoints with MockMvc
# DeadlineEventConsumerTest — mock RabbitMQ message consumption
# NotificationServiceTest — unit tests for list/markAsRead
```

## Related

- `services/application-service/messaging/DeadlineEventPublisher.java` — publishes events consumed here
- `services/application-service/messaging/DeadlineEventPayload.java` — **must stay in sync** with this service's copy
- `infra/terraform/amazonmq.tf` — Amazon MQ broker configuration
- `infra/terraform/ecs.tf` — ECS task definition for notification-service
