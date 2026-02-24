# Job Application Tracker + Resume Targeting Assistant

A hybrid-cloud full-stack microservices platform for tracking job/internship applications, managing resume-targeting notes, and generating deadline reminders.

## Live Demo

**https://d3tmito2x0icp5.cloudfront.net**

## What It Does
- Track applications: company, role, link, deadline, status, notes
- Application statuses: `SAVED → APPLIED → OA → INTERVIEW → REJECTED | OFFER`
- Resume-targeting notes per application: keywords, bullet ideas, job description excerpt, match notes
- Dashboard: total count, counts by status, upcoming deadlines, recent updates — Redis-cached
- Async deadline reminders: Application Service publishes event → Notification Service creates notification

## Domain Model

**JobApplication**: company, job title, location, job post URL, deadline, applied date, status, notes

**ResumeTargetingNote**: must-have keywords, nice-to-have keywords, custom bullet ideas, job description excerpt, match notes

**Notification**: type, title, message, read/unread status

## Architecture

Four Spring Boot services behind a Spring Cloud Gateway, fronted by CloudFront:

```
Browser → CloudFront → S3 (static SPA)
                    → ALB → gateway-service (Spring Cloud Gateway)
                                ├── /auth/**         → auth-service
                                ├── /applications/** → application-service
                                └── /notifications/**→ notification-service
```

- **Auth Service** — registration, login, JWT issuance/validation
- **Application Service** — job apps, targeting notes, dashboard; publishes reminder events; Redis-caches dashboard
- **Notification Service** — consumes RabbitMQ events, stores and surfaces notifications
- **Gateway Service** — sole API entry point; routes all frontend traffic; handles CORS

## Cloud Infrastructure

| Resource | Service |
|---|---|
| Frontend CDN | CloudFront + S3 (private bucket, OAC) |
| Compute | ECS Fargate (4 services, private subnet) |
| Database | RDS MySQL 8.0 (auth_db, application_db, notification_db) |
| Cache | ElastiCache Redis 7 (application-service only) |
| Messaging | Amazon MQ for RabbitMQ 3.x |
| Load Balancer | ALB (public subnet) |
| Secrets | AWS Secrets Manager → ECS task env injection |
| Images | ECR (one repo per service, mutable tags) |
| IaC | Terraform (`infra/terraform/`) |
| CI/CD | GitHub Actions (`.github/workflows/deploy.yml`) |

## API

**Auth**: `POST /auth/register` · `POST /auth/login` · `GET /auth/me`

**Applications**: `POST /applications` · `GET /applications` · `GET /applications/{id}` · `PUT /applications/{id}` · `DELETE /applications/{id}`

**Targeting Notes**: `GET /applications/{id}/targeting-note` · `PUT /applications/{id}/targeting-note`

**Dashboard**: `GET /applications/dashboard/summary`

**Notifications**: `GET /notifications` · `PATCH /notifications/{id}/read`

## CloudFront Routing Convention

CloudFront behaviors route API paths to the ALB. Use `/path*` (wildcard without leading slash) for any prefix where the base path is itself a valid endpoint:

| Pattern | Matches |
|---|---|
| `/auth/*` | `/auth/login`, `/auth/register` (always has sub-path) |
| `/applications*` | `/applications`, `/applications/dashboard/summary`, `/applications/{id}` |
| `/notifications*` | `/notifications`, `/notifications/{id}/read` |

**Important:** `/path/*` does NOT match `/path` (exact base path). Always use `/path*` unless all endpoints in that prefix have a mandatory sub-path.

## Project Structure

```
Job-Application-Tracker/
├── .github/workflows/deploy.yml    # CI/CD: 5 jobs (4 backend + frontend)
├── frontend/                       # React 18 + TypeScript + Vite + Tailwind
│   ├── src/
│   │   ├── features/               # Feature co-located hooks and components
│   │   │   ├── applications/
│   │   │   ├── auth/
│   │   │   └── notifications/
│   │   ├── lib/apiClient.ts        # Axios singleton, JWT interceptor, 401 handler
│   │   ├── pages/                  # Route-level components
│   │   └── components/             # Shared UI
│   └── .env.example
├── services/
│   ├── gateway-service/            # Spring Cloud Gateway
│   ├── auth-service/               # JWT auth
│   ├── application-service/        # Core domain + Redis + RabbitMQ publisher
│   └── notification-service/       # RabbitMQ consumer
│       (each has a Dockerfile)
└── infra/
    ├── docker-compose.yml          # Infra only (MySQL, Redis, RabbitMQ)
    ├── docker-compose.full.yml     # Full stack (infra + all 4 services)
    ├── .env.example
    └── terraform/                  # Complete AWS IaC
```
