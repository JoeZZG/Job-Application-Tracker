# services/application-service/

## Purpose

Spring Boot 3.2.1 service that owns the core domain: `JobApplication` and `ResumeTargetingNote` entities. Provides full CRUD, dashboard summary (Redis-cached), and publishes deadline events to RabbitMQ for async notification delivery.

Port: **8082**
Database: **application_db** (RDS MySQL)
Cache: **Redis** (ElastiCache — dashboard only)
Messaging: **RabbitMQ** publisher (Amazon MQ)

## Key Files

| File | Role |
|---|---|
| `controller/ApplicationController.java` | All REST endpoints: CRUD, dashboard, targeting notes |
| `service/ApplicationService.java` | CRUD logic; publishes deadline event on create/update |
| `service/DashboardService.java` | `@Cacheable` dashboard summary — queries counts by status + upcoming deadlines |
| `service/TargetingNoteService.java` | Get/upsert resume targeting note with ownership check |
| `config/RedisConfig.java` | `RedisCacheManager` with TTL from `REDIS_DASHBOARD_TTL_MINUTES` |
| `config/RabbitMQConfig.java` | `TopicExchange` bean, `RabbitTemplate` bean, `Jackson2JsonMessageConverter` |
| `config/CacheNames.java` | Cache name constant: `DASHBOARD_SUMMARY = "app:dashboard"` |
| `messaging/DeadlineEventPublisher.java` | Publishes `DeadlineEventPayload` to `applications.topic` exchange |
| `messaging/DeadlineEventPayload.java` | Record: `id, userId, company, jobTitle, deadline, type, timestamp` |
| `entity/JobApplication.java` | JPA entity: `job_applications` table |
| `entity/ResumeTargetingNote.java` | JPA entity: `resume_targeting_notes` table (FK → job_applications) |
| `entity/ApplicationStatus.java` | Enum: `SAVED, APPLIED, OA, INTERVIEW, REJECTED, OFFER` |
| `src/main/resources/application.yml` | MySQL, Redis, RabbitMQ connection config |
| `src/main/resources/db/migration/V1__init.sql` | Flyway: creates both tables with indexes |

## Structure

```
application-service/
├── src/main/java/com/jobtracker/application/
│   ├── ApplicationServiceApplication.java
│   ├── controller/
│   │   └── ApplicationController.java
│   ├── service/
│   │   ├── ApplicationService.java
│   │   ├── DashboardService.java
│   │   └── TargetingNoteService.java
│   ├── repository/
│   │   ├── JobApplicationRepository.java
│   │   └── ResumeTargetingNoteRepository.java
│   ├── entity/
│   │   ├── JobApplication.java
│   │   ├── ResumeTargetingNote.java
│   │   └── ApplicationStatus.java     # ENUM
│   ├── dto/
│   │   ├── CreateApplicationRequest.java
│   │   ├── UpdateApplicationRequest.java
│   │   ├── ApplicationResponse.java
│   │   ├── DashboardSummaryResponse.java
│   │   ├── TargetingNoteRequest.java
│   │   └── TargetingNoteResponse.java
│   ├── config/
│   │   ├── CacheNames.java            # Redis cache name constants
│   │   ├── RedisConfig.java           # RedisCacheManager + TTL
│   │   └── RabbitMQConfig.java        # Exchange, RabbitTemplate, converter
│   ├── messaging/
│   │   ├── DeadlineEventPayload.java  # JSON payload record
│   │   └── DeadlineEventPublisher.java
│   ├── security/
│   │   ├── SecurityConfig.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── JwtUtil.java
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       ├── ErrorResponse.java
│       ├── ForbiddenException.java    # 403 not owner
│       └── ResourceNotFoundException.java
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/V1__init.sql
├── src/test/java/com/jobtracker/application/
│   ├── controller/ApplicationControllerTest.java
│   └── service/
│       ├── ApplicationServiceTest.java
│       └── TargetingNoteServiceTest.java
├── Dockerfile
├── .dockerignore
└── pom.xml
```

## Implementation Overview

### CRUD (`ApplicationService`)
- All operations scope to `userId` extracted from JWT — a user can only see/modify their own applications.
- `create()` and `update()` trigger `DeadlineEventPublisher.publish()` when a deadline is set.
- `delete()` cascades to `ResumeTargetingNote` (DB-level `ON DELETE CASCADE`).
- All write methods are `@Transactional`.

### Dashboard (`DashboardService`)
- `getSummary(userId)` is annotated `@Cacheable(CacheNames.DASHBOARD_SUMMARY)`.
- Cache key: `app:dashboard:{userId}` (Spring auto-appends the method argument).
- Cache is **evicted** when any application is created, updated, or deleted (use `@CacheEvict` or `cacheManager.getCache(...).evict(userId)` in `ApplicationService`).
- TTL set from `REDIS_DASHBOARD_TTL_MINUTES` env var (default 5 min).

### Targeting Notes (`TargetingNoteService`)
- One note per application (1:1 relationship, `application_id UNIQUE`).
- `upsert()` is idempotent: creates if absent, updates if present.
- Ownership check: verifies the application's `userId` matches the requesting user (403 if not).

### RabbitMQ Publishing
- Exchange: `applications.topic` (durable, topic type).
- Routing key: `applications.application.deadline`.
- Payload: `DeadlineEventPayload` serialized to JSON via `Jackson2JsonMessageConverter`.
- Fire-and-forget — no ack/confirm mode; publishing failures are logged but do not fail the HTTP request.

## Implementation Details & Gotchas

- **Redis cache eviction**: Dashboard cache must be evicted on every application mutation. If you add a new write operation (e.g., bulk import), remember to call `@CacheEvict` or you'll serve stale counts.
- **`ResumeTargetingNote` FK cascade**: deleting a `JobApplication` automatically deletes its note via DB cascade. No service-layer code needed for note cleanup.
- **`@Cacheable` requires a proxy call**: calling a `@Cacheable` method from within the same class bypasses the cache proxy. Always call `DashboardService.getSummary()` from `ApplicationController`, not from within `ApplicationService`.
- **`ddl-auto: validate`**: Hibernate never modifies schema. New columns require a new Flyway migration.
- **JWT_SECRET**: uses the same shared secret as auth-service. Injected from AWS Secrets Manager in production.
- **`DashboardSummaryResponse` must be serializable**: stored in Redis as JSON. Ensure all fields have no-arg constructors or use Java Records.
- **`ApplicationStatus` as MySQL ENUM**: the Flyway migration defines the enum at DB level. Adding a new status requires both a Flyway migration and a Java enum update.

## Dependencies

Key `pom.xml` dependencies (Spring Boot 3.2.1):
```xml
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-validation
spring-boot-starter-data-redis
spring-boot-starter-amqp
spring-boot-starter-cache
spring-boot-starter-actuator
mysql-connector-j (runtime)
flyway-core + flyway-mysql
jjwt-api 0.12.3 + jjwt-impl + jjwt-jackson
springdoc-openapi-starter-webmvc-ui 2.3.0
```

## Usage

```bash
# Requires MySQL (application_db), Redis, and RabbitMQ
export MYSQL_HOST=localhost MYSQL_PORT=3306
export MYSQL_DATABASE=application_db
export MYSQL_USERNAME=root MYSQL_PASSWORD=<pass>
export JWT_SECRET=<hex>
export REDIS_HOST=localhost REDIS_PORT=6379
export REDIS_DASHBOARD_TTL_MINUTES=5
export RABBITMQ_HOST=localhost RABBITMQ_PORT=5672
export RABBITMQ_USERNAME=guest RABBITMQ_PASSWORD=guest

mvn spring-boot:run
# → http://localhost:8082
# → http://localhost:8082/swagger-ui.html
```

## Testing

```bash
mvn test
# ApplicationControllerTest — HTTP layer tests
# ApplicationServiceTest — unit tests with mocked repository
# TargetingNoteServiceTest — ownership checks, upsert logic
```

## Related

- `services/notification-service/` — consumes events published by this service
- `services/auth-service/` — issues JWTs that this service validates
- `infra/terraform/elasticache.tf` — Redis cluster for dashboard cache
- `infra/terraform/amazonmq.tf` — RabbitMQ broker
- `infra/terraform/ecs.tf` — ECS task definition (CPU: 512, memory: 1024 MB)
