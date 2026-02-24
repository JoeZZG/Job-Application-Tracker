# dockerize-service

Add or fix a Dockerfile for a Spring Boot service.

## Goal
Produce a production-grade, multi-stage Dockerfile so the service can be built into a container image and deployed to AWS ECR / ECS.

## Input (provide in prompt)
- Service name (auth-service / application-service / gateway-service / notification-service)
- Server port the service listens on
- Any special JVM flags needed (e.g., memory limits)
- Whether the existing Dockerfile needs fixing or is new

## Output format
1. Dockerfile (complete, no stubs)
2. `.dockerignore` for the service (exclude `target/`, `.env`, etc.)
3. Local build + run commands to verify the image
4. Health check verification command
5. Notes on env vars that must be passed at runtime (`-e` flags or `docker-compose.full.yml` env section)

## Rules
- Multi-stage build: Maven builder → JRE Alpine runtime
- Non-root user (`appuser`) in runtime stage
- HEALTHCHECK via `wget` against `/actuator/health`
- No secrets baked into the image
- Image must start successfully with only env vars — no hardcoded `localhost` defaults
