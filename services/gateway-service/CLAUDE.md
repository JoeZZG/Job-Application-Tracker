# services/gateway-service/

## Purpose

Spring Cloud Gateway service that acts as the **sole API entry point** for all frontend traffic. Routes incoming requests to downstream services by path prefix, and handles CORS globally. Does **not** perform JWT validation — that is delegated to each downstream service.

Port: **8080**
No database, no cache, no messaging.

## Key Files

| File | Role |
|---|---|
| `src/main/java/com/jobtracker/gateway/GatewayServiceApplication.java` | Spring Boot main class (minimal — all config is YAML-driven) |
| `src/main/resources/application.yml` | All routing rules, CORS config, and downstream URIs |

## Structure

```
gateway-service/
├── src/main/java/com/jobtracker/gateway/
│   └── GatewayServiceApplication.java   # @SpringBootApplication only
├── src/main/resources/
│   └── application.yml                   # Routes + CORS
├── Dockerfile
├── .dockerignore
└── pom.xml
```

## Implementation Overview

All gateway behavior is configured in `application.yml`. There is no custom Java filter code.

### Route Table

| Route ID | Path Predicate | Downstream URI |
|---|---|---|
| `auth-service` | `/auth/**` | `${AUTH_SERVICE_URI}` |
| `application-service` | `/applications/**` | `${APP_SERVICE_URI}` |
| `notification-service` | `/notifications/**` | `${NOTIF_SERVICE_URI}` |

`StripPrefix=0` on all routes — the full path (including `/auth/`, `/applications/`, etc.) is forwarded as-is to the downstream service.

### CORS

Configured globally via `spring.cloud.gateway.globalcors`. The allowed origin is driven by `CORS_ALLOWED_ORIGIN` (set to the CloudFront distribution URL in production, `http://localhost:5173` for local dev). Allowed methods: `GET, POST, PUT, PATCH, DELETE, OPTIONS`. Credentials allowed.

### Service Discovery

In production (ECS), downstream URIs use **ECS Service Connect** internal DNS:
- `AUTH_SERVICE_URI=http://auth-service:8081`
- `APP_SERVICE_URI=http://application-service:8082`
- `NOTIF_SERVICE_URI=http://notification-service:8083`

In local dev, they default to `http://localhost:{port}`.

## Implementation Details & Gotchas

- **No JWT validation at gateway**: tokens are forwarded in the `Authorization` header as-is. Each downstream service validates independently. This means a service is still protected even if called directly (bypassing the gateway).
- **All downstream URIs are env-var-driven**: never hardcode `localhost` in `application.yml` for production profiles. The `${VAR:default}` pattern provides local-dev defaults.
- **CORS origin must match exactly**: CloudFront serves the SPA at `https://d3tmito2x0icp5.cloudfront.net`. If `CORS_ALLOWED_ORIGIN` is set to the wrong URL (e.g., the ALB URL), browsers will reject API responses due to CORS policy.
- **Spring Cloud Gateway is reactive (WebFlux)**: uses Netty, not a servlet container. Do not add servlet-based libraries (e.g., `spring-boot-starter-web`). If you need a custom filter, implement `GatewayFilter` or `GlobalFilter`, not a servlet `Filter`.
- **Actuator health check**: `GET /actuator/health` → 200. Used by ECS ALB health checks. No additional config needed.
- **Path predicates are prefix-based**: `/auth/**` matches `/auth/login`, `/auth/register`, `/auth/me`, etc. Adding a new service requires a new `routes` entry in `application.yml` and a corresponding CloudFront behavior.

## Dependencies

```xml
<parent>spring-boot-starter-parent 3.2.1</parent>
spring-cloud-starter-gateway
spring-boot-starter-actuator
<!-- Spring Cloud BOM: 2023.0.0 -->
```

No web, JPA, security, or data dependencies — purely reactive routing.

## Usage

```bash
# Requires downstream services to be running (or accessible)
export AUTH_SERVICE_URI=http://localhost:8081
export APP_SERVICE_URI=http://localhost:8082
export NOTIF_SERVICE_URI=http://localhost:8083
export CORS_ALLOWED_ORIGIN=http://localhost:5173

mvn spring-boot:run
# → http://localhost:8080
# All API traffic proxied through here
```

## Related

- `services/auth-service/` — receives `/auth/**`
- `services/application-service/` — receives `/applications/**`
- `services/notification-service/` — receives `/notifications/**`
- `infra/terraform/ecs.tf` — ECS task definition (CPU: 256, memory: 512 MB)
- `infra/terraform/alb.tf` — ALB forwards all HTTPS traffic to this service's target group
- `infra/terraform/cloudfront.tf` — CloudFront forwards `/auth/*`, `/applications*`, `/notifications*` to the ALB
