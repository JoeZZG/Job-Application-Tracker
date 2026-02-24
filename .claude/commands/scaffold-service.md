# scaffold-service

Scaffold a new backend service with minimal production-style structure.

## Goal
Create a clean starting point for a Spring Boot service that fits the project conventions.

## Input (provide in prompt)
- Service name (gateway-service / auth-service / application-service / notification-service)
- Service responsibility
- Needed dependencies (JPA, Security, RabbitMQ, Redis, etc.)
- Whether it needs DB access

## Output format
1. Suggested folder/package structure
2. Required dependencies
3. `application.yml` skeleton (env-based config)
4. Main starter files/classes
5. Health endpoint / actuator setup
6. Basic error handling setup
7. Dockerfile (if applicable)
8. Environment variables needed
9. Startup checklist
10. What is intentionally left out

## Rules
- Prefer simple defaults
- Use Java 17 + Spring Boot 3 conventions
- Do not overbuild abstractions at scaffold stage