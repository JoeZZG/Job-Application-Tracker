# Job Application Tracker + Resume Targeting Assistant

A full-stack microservices application for tracking job applications, managing resume-targeting notes per application, and delivering async deadline reminders. Built as a portfolio project demonstrating distributed systems design with Spring Boot, React, and event-driven messaging.

---

## Features

- Track applications through a defined status lifecycle: `SAVED → APPLIED → OA → INTERVIEW → REJECTED | OFFER`
- Attach resume-targeting notes to any application: must-have keywords, nice-to-have keywords, custom bullet ideas, job description excerpt, and match notes
- Dashboard summary: total applications, counts by status, and upcoming deadlines — cached in Redis
- Async deadline reminders: Application Service publishes an event to RabbitMQ; Notification Service consumes it and persists a notification
- JWT-based authentication: tokens issued by Auth Service, validated independently by each downstream service using a shared secret

---

## Architecture

```
Browser
   |
   v
+--------------------+
|  Gateway Service   |  :8080  (Spring Cloud Gateway — sole entry point)
+--------------------+
   |          |          |
   v          v          v
+--------+ +-------------+ +-------------------+
|  Auth  | | Application | |   Notification    |
|Service | |   Service   | |     Service       |
| :8081  | |   :8082     | |     :8083         |
+--------+ +-------------+ +-------------------+
   |              |   |              |
   v              v   |              v
auth_db    application_db  notification_db
(MySQL)       (MySQL)          (MySQL)
               |   |
               v   v
            Redis  RabbitMQ
          (cache) (async events)
                      |
                      +-----> Notification Service (consumer)
```

All synchronous traffic flows through the gateway via REST. Async communication between Application Service and Notification Service uses RabbitMQ exclusively. Services do not share databases.

---

## Tech Stack

| Layer          | Technology                                                                   |
|----------------|------------------------------------------------------------------------------|
| Frontend       | React 18, TypeScript, Vite 5, React Router v6, Axios, TanStack Query v5, React Hook Form, Zod, Tailwind CSS |
| Backend        | Java 17, Spring Boot 3.2.1, Spring Cloud Gateway                            |
| Auth           | Spring Security (`SecurityFilterChain`), JWT (jjwt 0.12.3)                 |
| Persistence    | MySQL 8.0, Spring Data JPA, Flyway (schema migrations)                      |
| Cache          | Redis 7 (Application Service only — dashboard summary)                      |
| Messaging      | RabbitMQ 3 with `Jackson2JsonMessageConverter`; dead-letter queue on every consumer |
| Infrastructure | Docker Compose (local), AWS RDS MySQL (cloud database)                      |
| Testing        | JUnit 5, Spring Boot Test, Spring Security Test                             |

---

## Services Overview

**Gateway Service** (`:8080`) is the sole entry point for all frontend traffic. Built with Spring Cloud Gateway, it routes `/auth/**`, `/applications/**`, and `/notifications/**` to their respective downstream services using path-based predicates. CORS is handled at the gateway level so downstream services require no individual CORS configuration. The gateway does not perform JWT validation itself — that responsibility belongs to each downstream service.

**Auth Service** (`:8081`) owns all user credentials and is the only service that issues JWTs. It exposes endpoints for registration, login, and fetching the authenticated user's profile. Spring Security is configured via a `SecurityFilterChain` bean. The JWT expiration is set to 24 hours (configurable). Schema is managed by Flyway against `auth_db`. There is no Redis or RabbitMQ dependency — auth is synchronous only.

**Application Service** (`:8082`) owns the `JobApplication` and `ResumeTargetingNote` domain objects. It provides full CRUD for applications, a upsert endpoint for targeting notes, and a dashboard summary endpoint. The dashboard summary is cached in Redis under the key pattern `app:dashboard:{userId}` with a configurable TTL (default 5 minutes). When a deadline event is triggered, the service publishes a message to the `applications.topic` exchange with routing key `applications.application.deadline`. JWT tokens forwarded from the gateway are validated locally within this service.

**Notification Service** (`:8083`) has no outbound HTTP calls to other services. It consumes messages from the `notification.applications.deadline` RabbitMQ queue, persists a `Notification` record to `notification_db`, and exposes REST endpoints for listing notifications and marking them read. Consumer retry is configured with exponential backoff (up to 3 attempts: 1 s, 2 s, 4 s). Messages that exhaust retries are routed to the dead-letter queue `notification.applications.deadline.dlq`. JWT validation is performed locally on REST endpoints.

---

## Getting Started — Local Setup

### Prerequisites

- Java 17 (verify with `java -version`)
- Maven 3.8+ (verify with `mvn -version`)
- Node.js 18+ and npm (verify with `node -version`)
- Docker and Docker Compose (verify with `docker compose version`)

### Steps

1. **Clone the repository.**

   ```bash
   git clone <repository-url>
   cd Job-Application-Tracker
   ```

2. **Set up environment variables.**

   ```bash
   cp infra/.env.example infra/.env
   ```

   Open `infra/.env` and replace every `change_me_*` placeholder with real values. For `JWT_SECRET`, generate a value with:

   ```bash
   openssl rand -hex 32
   ```

   The same `JWT_SECRET` value must be present in the environment of every Spring Boot service at startup.

3. **Start infrastructure services (MySQL, Redis, RabbitMQ).**

   ```bash
   cd infra
   docker compose up -d
   ```

   Wait until all three containers report healthy. You can check with:

   ```bash
   docker compose ps
   ```

   Expected: `jat_mysql`, `jat_redis`, and `jat_rabbitmq` all show `healthy`.

4. **Export environment variables for the Spring Boot services.**

   The Spring Boot processes run on the host, not inside Docker. They connect to the containers via the mapped host ports. Export the variables from your `.env` file, or set them directly in your shell. A convenient way on macOS/Linux:

   ```bash
   export $(grep -v '^#' infra/.env | xargs)
   ```

   When running on the host (not inside the Docker network), set:

   ```bash
   export REDIS_HOST=localhost
   export RABBITMQ_HOST=localhost
   ```

5. **Start each Spring Boot service** in separate terminal windows, from the repository root.

   ```bash
   # Terminal 1 — Auth Service
   cd services/auth-service && mvn spring-boot:run

   # Terminal 2 — Application Service
   cd services/application-service && mvn spring-boot:run

   # Terminal 3 — Notification Service
   cd services/notification-service && mvn spring-boot:run

   # Terminal 4 — Gateway Service
   cd services/gateway-service && mvn spring-boot:run
   ```

   Each service applies Flyway migrations on startup. Confirm startup by checking for a log line like `Started AuthServiceApplication in X seconds`.

6. **Start the frontend.**

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   Expected output: `Local: http://localhost:5173/`

### Access Points

| Resource              | URL                                      |
|-----------------------|------------------------------------------|
| Frontend              | http://localhost:5173                    |
| API Gateway           | http://localhost:8080                    |
| Auth Service Swagger  | http://localhost:8081/swagger-ui.html    |
| App Service Swagger   | http://localhost:8082/swagger-ui.html    |
| Notif Service Swagger | http://localhost:8083/swagger-ui.html    |
| RabbitMQ Management   | http://localhost:15672 (credentials from `.env`) |

---

## API Reference

All requests to the API go through the gateway at `http://localhost:8080`. Endpoints marked **Yes** under "Auth required" expect a `Authorization: Bearer <token>` header.

| Method | Path                                      | Service      | Auth required | Description                                              |
|--------|-------------------------------------------|--------------|---------------|----------------------------------------------------------|
| POST   | `/auth/register`                          | Auth         | No            | Register a new user account                             |
| POST   | `/auth/login`                             | Auth         | No            | Authenticate and receive a JWT                          |
| GET    | `/auth/me`                                | Auth         | Yes           | Return the authenticated user's profile                 |
| POST   | `/applications`                           | Application  | Yes           | Create a new job application                            |
| GET    | `/applications`                           | Application  | Yes           | List all applications for the authenticated user        |
| GET    | `/applications/{id}`                      | Application  | Yes           | Fetch a single application by ID                        |
| PUT    | `/applications/{id}`                      | Application  | Yes           | Update an existing application                          |
| DELETE | `/applications/{id}`                      | Application  | Yes           | Delete an application                                   |
| GET    | `/applications/{id}/targeting-note`       | Application  | Yes           | Fetch the resume-targeting note for an application      |
| PUT    | `/applications/{id}/targeting-note`       | Application  | Yes           | Create or update the targeting note for an application  |
| GET    | `/applications/dashboard/summary`         | Application  | Yes           | Return aggregate counts and upcoming deadlines (cached) |
| GET    | `/notifications`                          | Notification | Yes           | List all notifications for the authenticated user       |
| PATCH  | `/notifications/{id}/read`               | Notification | Yes           | Mark a notification as read                             |

---

## Key Engineering Decisions

- **Dead-letter queue on every consumer.** The `notification.applications.deadline` consumer is configured with exponential-backoff retry (3 attempts) and a dedicated DLQ (`notification.applications.deadline.dlq`). This ensures that transient failures (e.g., a brief DB outage) are retried, and messages that cannot be processed are preserved for inspection rather than silently dropped.

- **Redis scoped to Application Service only.** The dashboard summary aggregation queries multiple tables and is called frequently. Caching it at the service layer with a 5-minute TTL (configurable via `REDIS_DASHBOARD_TTL_MINUTES`) avoids repeated aggregation queries under normal load. No other service has a cache dependency, keeping operational complexity proportional to actual need.

- **JWT validated at each downstream service, not only at the gateway.** The gateway forwards the `Authorization` header to downstream services, which each validate the token independently using the shared `JWT_SECRET`. This means a service remains protected even if it is called directly, bypassing the gateway. It also avoids introducing a synchronous auth call on every request.

- **Flyway for all schema changes.** Every service runs Flyway on startup against its own database (`auth_db`, `application_db`, `notification_db`). Hibernate's `ddl-auto` is set to `validate`, not `update`, so Hibernate checks schema correctness but never modifies it. This prevents schema drift in shared environments and produces a full, auditable migration history.

- **Strict service boundary isolation.** Each service owns its own MySQL schema and there are no cross-service database joins. Inter-service communication is either synchronous REST through the gateway or asynchronous events through RabbitMQ. The Notification Service has no outbound HTTP calls to any other service.

---

## Project Structure

```
Job-Application-Tracker/
├── frontend/
│   ├── src/
│   │   ├── features/
│   │   │   ├── applications/   # Application list, form, detail hooks and components
│   │   │   ├── auth/           # Login, register, AuthContext, useAuth hook
│   │   │   └── notifications/  # Notification list and read-state hooks
│   │   ├── lib/
│   │   │   └── apiClient.ts    # Single Axios instance with JWT interceptor
│   │   ├── pages/              # Route-level page components
│   │   ├── components/         # Shared UI components
│   │   └── main.tsx
│   └── package.json
├── services/
│   ├── gateway-service/        # Spring Cloud Gateway — routing and CORS
│   ├── auth-service/           # Registration, login, JWT issuance
│   │   └── src/main/java/com/jobtracker/auth/
│   │       ├── controller/
│   │       ├── service/
│   │       ├── repository/
│   │       ├── dto/
│   │       ├── entity/
│   │       ├── security/       # SecurityFilterChain, JWT filter
│   │       └── exception/
│   ├── application-service/    # Job applications, targeting notes, dashboard
│   │   └── src/main/java/com/jobtracker/application/
│   │       ├── controller/
│   │       ├── service/
│   │       ├── repository/
│   │       ├── dto/
│   │       ├── entity/
│   │       ├── config/         # RedisCacheManager, RabbitMQ topology beans
│   │       ├── messaging/      # RabbitMQ event publisher
│   │       ├── security/
│   │       └── exception/
│   └── notification-service/   # RabbitMQ consumer, notification persistence
│       └── src/main/java/com/jobtracker/notification/
│           ├── controller/
│           ├── service/
│           ├── repository/
│           ├── dto/
│           ├── entity/
│           ├── config/         # RabbitMQ topology beans (queue, DLQ, exchange)
│           ├── messaging/      # @RabbitListener consumer
│           ├── security/
│           └── exception/
└── infra/
    ├── docker-compose.yml      # MySQL, Redis, RabbitMQ with healthchecks
    ├── init.sql                # Creates auth_db, application_db, notification_db
    └── .env.example            # Environment variable template
```

---

## Known Limitations

- The gateway routes to `localhost` addresses for downstream services. This is a local-development configuration. A production deployment would replace these with internal DNS names or a service registry.
- No Docker Compose definition exists yet for the Spring Boot services themselves. They are started manually via `mvn spring-boot:run`.
- There is no production deployment pipeline in this repository. AWS RDS is used for MySQL; Redis and RabbitMQ run locally via Docker Compose and are not provisioned on AWS.
