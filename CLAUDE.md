# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Documentation Index

Per-directory CLAUDE.md files with implementation details, gotchas, and usage instructions:

| Directory | CLAUDE.md | Description |
|---|---|---|
| `frontend/` | [frontend/CLAUDE.md](frontend/CLAUDE.md) | React 18 + TypeScript SPA: auth flow, React Query hooks, form validation, CloudFront path patterns |
| `services/auth-service/` | [services/auth-service/CLAUDE.md](services/auth-service/CLAUDE.md) | JWT issuance, BCrypt, Spring Security FilterChain, Flyway schema |
| `services/application-service/` | [services/application-service/CLAUDE.md](services/application-service/CLAUDE.md) | Core CRUD, Redis dashboard cache, RabbitMQ publisher, targeting notes |
| `services/notification-service/` | [services/notification-service/CLAUDE.md](services/notification-service/CLAUDE.md) | RabbitMQ consumer, DLQ + retry policy, notification REST API |
| `services/gateway-service/` | [services/gateway-service/CLAUDE.md](services/gateway-service/CLAUDE.md) | Spring Cloud Gateway routing, CORS, ECS Service Connect |
| `infra/` | [infra/CLAUDE.md](infra/CLAUDE.md) | Docker Compose (local dev), environment variables, volume management |
| `infra/terraform/` | [infra/terraform/CLAUDE.md](infra/terraform/CLAUDE.md) | AWS Terraform IaC: VPC, ECS, RDS, Redis, MQ, ALB, CloudFront, ECR, Secrets Manager |

## Project Overview

Job Application Tracker + Resume Targeting Assistant — a hybrid-cloud full-stack microservices portfolio project. See `.claude/README.md` for the full feature spec, domain model, and API surface.

## Fixed Technology Stack

Do not introduce technologies outside this list without explicit justification:

| Layer | Technology |
|---|---|
| Frontend | React 18 + TypeScript, Vite, React Router v6, Axios, TanStack Query, React Hook Form + Zod, Tailwind CSS |
| Backend | Java 17, Spring Boot 3.x (`jakarta.*`), Spring Cloud Gateway |
| Auth | Spring Security (`SecurityFilterChain` bean), JWT |
| Persistence | MySQL on AWS RDS, Spring Data JPA |
| Cache | Redis on AWS ElastiCache — Application Service only |
| Messaging | RabbitMQ via Amazon MQ with `Jackson2JsonMessageConverter`; DLQ required on every queue |
| Containerisation | Docker (multi-stage builds); images stored in AWS ECR |
| Infrastructure (local) | Docker Compose — `infra/docker-compose.yml` (infra only) and `infra/docker-compose.full.yml` (full stack) |
| Infrastructure (cloud) | AWS ECS Fargate (services), RDS MySQL, ElastiCache Redis, Amazon MQ, ALB, CloudFront + S3 (frontend), Secrets Manager |
| IaC | Terraform (`infra/terraform/`) |
| CI/CD | GitHub Actions (`.github/workflows/deploy.yml`) — build → ECR push → ECS deploy + S3 sync → CloudFront invalidation |
| Testing | JUnit, Spring Boot Test |

## Architecture Rules

1. **Auth Service** is the sole owner of credentials and JWT issuance/validation.
2. **Application Service** owns job applications and resume-targeting notes.
3. **Notification Service** consumes RabbitMQ events and never calls other services directly.
4. No cross-service DB joins — services never share a schema.
5. Sync = REST via API Gateway. Async = RabbitMQ.
6. Redis caching: Application Service only.
7. Frontend calls API Gateway only — never individual services directly.
8. All downstream service URIs in the gateway are env-var-driven — never hardcoded.
9. Secrets (DB passwords, JWT secret, MQ password) live in AWS Secrets Manager in production; injected as ECS task env vars. Never in committed files.

## AWS Deployment Architecture

```
Browser
  │
  ▼
CloudFront (d3tmito2x0icp5.cloudfront.net)
  │
  ├── /assets/*,  /  (default)  →  S3 bucket (static React SPA)
  │
  ├── /auth/*          ─┐
  ├── /applications*   ─┤→  ALB (public subnet)
  └── /notifications*  ─┘       │
                                 ▼
                         gateway-service  (ECS Fargate, private subnet)
                           ├── /auth/**         → auth-service        (ECS Fargate, private subnet)
                           ├── /applications/** → application-service (ECS Fargate, private subnet)
                           └── /notifications/**→ notification-service (ECS Fargate, private subnet)

Managed services (all in private subnet):
  • RDS MySQL 8.0      — auth_db, application_db, notification_db
  • ElastiCache Redis  — application-service dashboard cache
  • Amazon MQ (RabbitMQ 3.x) — async deadline events

Service discovery: ECS Service Connect (internal DNS)
Secrets: AWS Secrets Manager → ECS task env injection
Images: AWS ECR (one repo per service, mutable tags)
Frontend: S3 (private) → CloudFront OAC
```

**CloudFront path pattern convention:** Use `/path*` (no slash before wildcard) for any API prefix where the base path itself is a valid endpoint (e.g., `GET /notifications`, `GET /applications`). Use `/path/*` only when all endpoints always have a sub-path. Current behaviors: `/auth/*`, `/applications*`, `/notifications*`.

## Backend Conventions (Spring Boot)

- Package-by-feature: `com.app.{feature}`
- Never expose JPA entities in responses — use DTOs (Java records preferred)
- Constructor injection; `@Transactional` on all write methods; `@Valid` on all request DTOs
- Global `@RestControllerAdvice`; domain-specific exceptions; consistent error body: `{ status, error, message, timestamp, path }`
- OpenAPI annotations on all endpoints
- RabbitMQ: exchanges/queues/bindings as `@Bean`; `@RabbitListener` with DLQ; JSON serialization
- Redis: explicit `RedisCacheManager`; cache names as constants; document TTL and invalidation
- Schema changes: always include a Flyway/Liquibase migration
- All external host/port/credential config must be env-var-driven (no defaults that point to localhost in production profiles)

## Frontend Conventions (React + TypeScript)

- Single Axios instance at `src/lib/apiClient.ts` with JWT interceptor; no raw `fetch`
- `AuthContext` + `useAuth` hook; JWT in `localStorage`; `<ProtectedRoute>` for gating
- React Query for server state; hooks co-located in `src/features/{feature}/`; invalidate keys on mutations
- React Hook Form + Zod; field names must match backend DTO exactly
- No `any`; explicit prop interfaces; every async UI needs loading, error, and empty states
- `VITE_API_BASE_URL` env var drives the gateway URL. In production it is empty — CloudFront routes API paths to the ALB so relative URLs work. In local dev it is also empty and Vite proxy handles routing.
- **Always use `Array.isArray()` before calling array methods on data from `useQuery`** — unexpected non-array responses (e.g., from misconfigured routing) must not crash the app.

## Data / Infrastructure Conventions

- MySQL: `snake_case` plural tables; `id BIGINT UNSIGNED AUTO_INCREMENT`; always `created_at`/`updated_at`
- Redis key pattern: `{service}:{entity}:{identifier}`; every key needs an explicit TTL via env var
- RabbitMQ: exchanges `{domain}.{type}`; queues `{service}.{domain}.{event}`; DLQs suffixed `.dlq`
- Env vars: `UPPER_SNAKE_CASE`, prefixed by concern (`MYSQL_*`, `REDIS_*`, `RABBITMQ_*`, `AWS_*`); never hardcoded; only `.env.example` committed
- Dockerfiles: multi-stage (Maven build → JRE runtime); use `eclipse-temurin:17-jre-alpine` as runtime base
- ECS task definitions: `awslogs` log driver → CloudWatch; explicit CPU/memory per service
- ECR repositories: **MUTABLE** image tags (`:latest` is re-pushed on every deploy)

## Dockerfile Conventions

Every Spring Boot service has a `Dockerfile` at its root:
```
Stage 1 (builder): maven:3.9-eclipse-temurin-17-alpine — compile + package
Stage 2 (runtime): eclipse-temurin:17-jre-alpine — copy jar, EXPOSE port, ENTRYPOINT
```
- Non-root user (`appuser`) created in the runtime stage
- No secrets in Dockerfile or build args
- Health check: `HEALTHCHECK CMD wget -qO- http://localhost:{PORT}/actuator/health || exit 1`

## CI/CD Conventions (GitHub Actions)

- Trigger: push to `main` or `workflow_dispatch`
- Five jobs run in parallel: `deploy-auth`, `deploy-application`, `deploy-notification`, `deploy-frontend`, and `deploy-gateway` (depends on all three service jobs)
- Steps per backend service: checkout → set up JDK → Maven build → ECR login → Docker build+push → ECS update-service → wait stable
- Frontend job: checkout → Node 20 → npm ci → Vite build (VITE_API_BASE_URL="") → S3 sync → CloudFront invalidation
- ECS service update uses `--force-new-deployment` to trigger rolling replace
- Required GitHub Secrets:
  - `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`
  - `ECS_CLUSTER_NAME` (e.g., `jat-staging-cluster`)
  - `FRONTEND_S3_BUCKET` (e.g., `jat-staging-frontend-217870417104`)
  - `CLOUDFRONT_DISTRIBUTION_ID` (e.g., `E37UWNQZYSAOAQ`)

## Custom Agents

| Agent | Invoke when |
|---|---|
| `system-architect` | Before any new feature, service boundary decision, or API/event contract |
| `backend-spring` | Spring Boot controllers, services, repos, DTOs, JWT, RabbitMQ, Redis |
| `frontend-react` | React pages, forms, routing, API integration, auth flows |
| `data-infra-engineer` | Docker Compose, MySQL schema, Redis strategy, RabbitMQ topology, env vars, Dockerfiles |
| `aws-deploy` | AWS ECS, ECR, RDS, ElastiCache, Amazon MQ, ALB, CloudFront, Secrets Manager, Terraform, GitHub Actions CI/CD |
| `code-reviewer` | Before commits/PRs and after major feature integration |
| `docs-writer` | README updates, API docs, demo scripts, recruiter summaries |

## Custom Commands

| Command | Purpose |
|---|---|
| `/plan-feature` | Plan a feature across all layers before implementation |
| `/scaffold-service` | Bootstrap a new Spring Boot service |
| `/implement-endpoint` | Implement one endpoint end-to-end |
| `/implement-frontend-page` | Build a React page wired to a backend endpoint |
| `/add-cache` | Add Redis caching to an existing service method |
| `/add-rabbitmq-event` | Add a new RabbitMQ producer/consumer pair |
| `/write-tests` | Write tests for an existing implementation |
| `/review-pr` | Code review before opening a PR |
| `/update-readme` | Sync README with current implementation |
| `/dockerize-service` | Add or fix a Dockerfile for a Spring Boot service |
| `/provision-aws` | Plan and apply Terraform for AWS infrastructure |
| `/setup-cicd` | Add or update the GitHub Actions deployment pipeline |
