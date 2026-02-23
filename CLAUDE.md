# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Job Application Tracker + Resume Targeting Assistant — a hybrid-cloud full-stack microservices portfolio project. See `.claude/README.md` for the full feature spec, domain model, and API surface.

## Fixed Technology Stack

Do not introduce technologies outside this list without explicit justification:

| Layer | Technology |
|---|---|
| Frontend | React 18 + TypeScript, Vite, React Router v6, Axios, TanStack Query, React Hook Form + Zod |
| Backend | Java 17, Spring Boot 3.x (`jakarta.*`), Spring Cloud Gateway |
| Auth | Spring Security (`SecurityFilterChain` bean), JWT |
| Persistence | MySQL on AWS RDS, Spring Data JPA |
| Cache | Redis — Application Service only |
| Messaging | RabbitMQ with `Jackson2JsonMessageConverter`; DLQ required on every queue |
| Infrastructure | Docker Compose (local), AWS (cloud) |
| Testing | JUnit, Spring Boot Test |

## Architecture Rules

1. **Auth Service** is the sole owner of credentials and JWT issuance/validation.
2. **Application Service** owns job applications and resume-targeting notes.
3. **Notification Service** consumes RabbitMQ events and never calls other services directly.
4. No cross-service DB joins — services never share a schema.
5. Sync = REST via API Gateway. Async = RabbitMQ.
6. Redis caching: Application Service only.
7. Frontend calls API Gateway only — never individual services directly.

## Backend Conventions (Spring Boot)

- Package-by-feature: `com.app.{feature}`
- Never expose JPA entities in responses — use DTOs (Java records preferred)
- Constructor injection; `@Transactional` on all write methods; `@Valid` on all request DTOs
- Global `@RestControllerAdvice`; domain-specific exceptions; consistent error body: `{ status, error, message, timestamp, path }`
- OpenAPI annotations on all endpoints
- RabbitMQ: exchanges/queues/bindings as `@Bean`; `@RabbitListener` with DLQ; JSON serialization
- Redis: explicit `RedisCacheManager`; cache names as constants; document TTL and invalidation
- Schema changes: always include a Flyway/Liquibase migration

## Frontend Conventions (React + TypeScript)

- Single Axios instance at `src/lib/apiClient.ts` with JWT interceptor; no raw `fetch`
- `AuthContext` + `useAuth` hook; JWT in `localStorage`; `<ProtectedRoute>` for gating
- React Query for server state; hooks co-located in `src/features/{feature}/`; invalidate keys on mutations
- React Hook Form + Zod; field names must match backend DTO exactly
- No `any`; explicit prop interfaces; every async UI needs loading, error, and empty states

## Data / Infrastructure Conventions

- MySQL: `snake_case` plural tables; `id BIGINT UNSIGNED AUTO_INCREMENT`; always `created_at`/`updated_at`
- Redis key pattern: `{service}:{entity}:{identifier}`; every key needs an explicit TTL via env var
- RabbitMQ: exchanges `{domain}.{type}`; queues `{service}.{domain}.{event}`; DLQs suffixed `.dlq`
- Env vars: `UPPER_SNAKE_CASE`, prefixed by concern (`MYSQL_*`, `REDIS_*`, `RABBITMQ_*`); never hardcoded; only `.env.example` committed

## Custom Agents

| Agent | Invoke when |
|---|---|
| `system-architect` | Before any new feature, service boundary decision, or API/event contract |
| `backend-spring` | Spring Boot controllers, services, repos, DTOs, JWT, RabbitMQ, Redis |
| `frontend-react` | React pages, forms, routing, API integration, auth flows |
| `data-infra-engineer` | Docker Compose, MySQL schema, Redis strategy, RabbitMQ topology, env vars |
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
