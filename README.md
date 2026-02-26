# Job Application Tracker + Resume Targeting Assistant

A full-stack microservices application for tracking job applications, managing resume-targeting notes per application, and delivering async deadline reminders. Built as a portfolio project demonstrating distributed systems design with Spring Boot, React, and event-driven messaging on AWS.

---

## Live Demo

**[https://d3tmito2x0icp5.cloudfront.net](https://d3tmito2x0icp5.cloudfront.net/login)**

Deployed on AWS: React SPA served from CloudFront + S3; four Spring Boot microservices running on ECS Fargate behind an Application Load Balancer; MySQL on RDS; Redis on ElastiCache; RabbitMQ on Amazon MQ.

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
CloudFront (CDN + SPA routing)
   |
   |-- /assets/*, / ---------> S3 bucket (React SPA static files)
   |
   |-- /auth/*               \
   |-- /applications*         >--> ALB (public subnet)
   |-- /notifications*       /          |
                                        v
                              +--------------------+
                              |  Gateway Service   |  :8080  (Spring Cloud Gateway)
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
                              (RDS MySQL)  (RDS MySQL)   (RDS MySQL)
                                            |   |
                                            v   v
                                         Redis  RabbitMQ
                                (ElastiCache) (Amazon MQ)
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
| Frontend CDN   | AWS CloudFront + S3 (Origin Access Control; SPA routing via custom error responses) |
| Compute        | AWS ECS Fargate (four services; private subnet)                             |
| Database       | AWS RDS MySQL 8.0                                                           |
| Cache (cloud)  | AWS ElastiCache Redis 7                                                     |
| Messaging (cloud) | AWS Amazon MQ for RabbitMQ                                              |
| Load Balancer  | AWS ALB (HTTP → HTTPS redirect)                                             |
| Secrets        | AWS Secrets Manager (injected into ECS tasks via `valueFrom`)               |
| IaC            | Terraform (`infra/terraform/`)                                              |
| CI/CD          | GitHub Actions — 5-job pipeline: 4 backend services + frontend              |
| Containers     | Docker (multi-stage builds), AWS ECR                                        |
| Testing        | JUnit 5, Spring Boot Test, Spring Security Test                             |

---

## Services Overview

**Gateway Service** (`:8080`) is the sole entry point for all API traffic. Built with Spring Cloud Gateway, it routes `/auth/**`, `/applications/**`, and `/notifications/**` to their respective downstream services using path-based predicates. CORS is handled at the gateway level; the allowed origin is driven by the `CORS_ALLOWED_ORIGIN` environment variable. The gateway does not perform JWT validation itself — that responsibility belongs to each downstream service.

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

### Option A — Infra in Docker, services on host (recommended for development)

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

   Wait until all three containers report healthy:

   ```bash
   docker compose ps
   ```

   Expected: `jat_mysql`, `jat_redis`, and `jat_rabbitmq` all show `healthy`.

4. **Export environment variables for the Spring Boot services.**

   ```bash
   export $(grep -v '^#' infra/.env | xargs)
   export REDIS_HOST=localhost
   export RABBITMQ_HOST=localhost
   ```

5. **Start each Spring Boot service** in separate terminal windows.

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

   Each service applies Flyway migrations on startup. Confirm startup with a log line like `Started AuthServiceApplication in X seconds`.

6. **Start the frontend.**

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   Expected output: `Local: http://localhost:5173/`

### Option B — Full stack in Docker

```bash
cp infra/.env.example infra/.env   # fill in values
cd infra
docker compose -f docker-compose.full.yml up --build
```

All services start in dependency order (infra → auth/application/notification → gateway → frontend dev server).

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

## Production Deployment

Deployment is fully automated via GitHub Actions (`.github/workflows/deploy.yml`) on every push to `main`.

**Pipeline jobs (run in parallel except gateway):**
1. `deploy-auth` — build JAR → Docker → ECR → ECS force-new-deployment
2. `deploy-application` — same
3. `deploy-notification` — same
4. `deploy-frontend` — Vite build → S3 sync → CloudFront invalidation
5. `deploy-gateway` — same as services, runs after 1–3 complete

**Required GitHub repository secrets:**

| Secret | Description |
|---|---|
| `AWS_ACCESS_KEY_ID` | IAM user with ECR push + ECS deploy + S3 sync + CloudFront permissions |
| `AWS_SECRET_ACCESS_KEY` | IAM user secret |
| `AWS_REGION` | e.g., `us-east-1` |
| `ECS_CLUSTER_NAME` | e.g., `jat-staging-cluster` |
| `FRONTEND_S3_BUCKET` | e.g., `jat-staging-frontend-217870417104` |
| `CLOUDFRONT_DISTRIBUTION_ID` | e.g., `E37UWNQZYSAOAQ` |

**Infrastructure provisioned with Terraform** (`infra/terraform/`): VPC, subnets, security groups, ALB, ECS cluster + services, RDS, ElastiCache, Amazon MQ, ECR repos, CloudFront distribution, S3 bucket, Secrets Manager secrets, IAM roles.

---

## API Reference

All requests go through the gateway. In production, CloudFront routes API paths to the ALB. Endpoints marked **Yes** under "Auth required" expect an `Authorization: Bearer <token>` header.

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

- **JWT validated at each downstream service, not only at the gateway.** The gateway forwards the `Authorization` header to downstream services, which each validate the token independently using the shared `JWT_SECRET` (injected from AWS Secrets Manager). This means a service remains protected even if it is called directly, bypassing the gateway.

- **CloudFront as the single frontend origin.** The React SPA is served from a private S3 bucket via CloudFront OAC. CloudFront path behaviors (`/auth/*`, `/applications*`, `/notifications*`) forward API requests to the ALB, so the frontend uses relative URLs in production — no CORS between the SPA and its own API. The `*` wildcard (not `/*`) is used for `/applications` and `/notifications` because those base paths are themselves valid API endpoints.

- **Flyway for all schema changes.** Every service runs Flyway on startup against its own database. Hibernate's `ddl-auto` is set to `validate` — Hibernate checks schema correctness but never modifies it. This prevents schema drift and produces a full, auditable migration history.

- **Strict service boundary isolation.** Each service owns its own MySQL schema and there are no cross-service database joins. Inter-service communication is either synchronous REST through the gateway or asynchronous events through RabbitMQ. The Notification Service has no outbound HTTP calls to any other service.

---

## Project Structure

```
Job-Application-Tracker/
├── .github/
│   └── workflows/
│       └── deploy.yml          # 5-job CI/CD pipeline (4 backend + frontend)
├── frontend/
│   ├── src/
│   │   ├── features/
│   │   │   ├── applications/   # Application list, form, detail hooks and components
│   │   │   ├── auth/           # Login, register, AuthContext, useAuth hook
│   │   │   └── notifications/  # Notification list and read-state hooks
│   │   ├── lib/
│   │   │   └── apiClient.ts    # Single Axios instance with JWT interceptor + 401 handler
│   │   ├── pages/              # Route-level page components
│   │   ├── components/         # Shared UI: AppLayout, ProtectedRoute, StatusBadge, etc.
│   │   └── main.tsx
│   ├── .env.example
│   └── package.json
├── services/
│   ├── gateway-service/        # Spring Cloud Gateway — routing and CORS
│   │   └── Dockerfile
│   ├── auth-service/           # Registration, login, JWT issuance
│   │   ├── Dockerfile
│   │   └── src/main/java/com/jobtracker/auth/
│   │       ├── controller/
│   │       ├── service/
│   │       ├── repository/
│   │       ├── dto/
│   │       ├── entity/
│   │       ├── security/       # SecurityFilterChain, JWT filter
│   │       └── exception/
│   ├── application-service/    # Job applications, targeting notes, dashboard
│   │   ├── Dockerfile
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
│       ├── Dockerfile
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
    ├── docker-compose.yml          # MySQL, Redis, RabbitMQ only (for local dev)
    ├── docker-compose.full.yml     # Full stack including all 4 Spring Boot containers
    ├── init.sql                    # Creates auth_db, application_db, notification_db
    ├── .env.example                # Environment variable template
    └── terraform/                  # AWS infrastructure as code
        ├── main.tf                 # Provider, backend, locals
        ├── vpc.tf                  # VPC, subnets, routing
        ├── security_groups.tf      # SGs per service
        ├── alb.tf                  # Application Load Balancer
        ├── cloudfront.tf           # CloudFront distribution + S3 bucket (frontend)
        ├── ecr.tf                  # ECR repositories (one per service)
        ├── ecs.tf                  # ECS cluster, task definitions, services
        ├── rds.tf                  # RDS MySQL
        ├── elasticache.tf          # ElastiCache Redis
        ├── amazonmq.tf             # Amazon MQ (RabbitMQ)
        ├── iam.tf                  # Task execution and task roles
        ├── secrets.tf              # Secrets Manager secrets
        ├── variables.tf
        └── outputs.tf
```
