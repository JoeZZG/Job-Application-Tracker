# services/auth-service/

## Purpose

Spring Boot 3.2.1 service that owns all user credentials and is the **sole issuer of JWTs** in the system. Other services validate tokens but never issue them. No Redis or RabbitMQ dependency — auth is purely synchronous.

Port: **8081**
Database: **auth_db** (RDS MySQL)

## Key Files

| File | Role |
|---|---|
| `src/main/java/com/jobtracker/auth/controller/AuthController.java` | REST endpoints: `POST /auth/register`, `POST /auth/login`, `GET /auth/me` |
| `src/main/java/com/jobtracker/auth/service/AuthService.java` | Business logic: BCrypt hashing, JWT issuance, user lookup |
| `src/main/java/com/jobtracker/auth/security/JwtUtil.java` | JJWT 0.12.3 wrapper: `generateToken()`, `extractUserId()`, `isTokenValid()` |
| `src/main/java/com/jobtracker/auth/security/JwtAuthenticationFilter.java` | `OncePerRequestFilter`: extracts Bearer token, validates, sets `userId` as Spring Security principal |
| `src/main/java/com/jobtracker/auth/security/SecurityConfig.java` | `SecurityFilterChain` bean: permits `/auth/register` + `/auth/login`, requires auth on all others |
| `src/main/java/com/jobtracker/auth/entity/User.java` | JPA entity mapped to `users` table |
| `src/main/java/com/jobtracker/auth/exception/GlobalExceptionHandler.java` | `@RestControllerAdvice`: maps exceptions to consistent error body |
| `src/main/resources/application.yml` | Server port, JDBC URL, Flyway config, JWT secret + expiry |
| `src/main/resources/db/migration/V1__init.sql` | Flyway migration: creates `users` table |

## Structure

```
auth-service/
├── src/main/java/com/jobtracker/auth/
│   ├── AuthServiceApplication.java
│   ├── controller/
│   │   └── AuthController.java
│   ├── service/
│   │   └── AuthService.java
│   ├── repository/
│   │   └── UserRepository.java         # findByEmail(String)
│   ├── entity/
│   │   └── User.java                   # id, email, password_hash, created_at
│   ├── dto/
│   │   ├── RegisterRequest.java        # email, password
│   │   ├── LoginRequest.java           # email, password
│   │   ├── AuthResponse.java           # token, user (UserResponse)
│   │   └── UserResponse.java           # id, email
│   ├── security/
│   │   ├── SecurityConfig.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── JwtUtil.java
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       ├── ErrorResponse.java          # status, error, message, timestamp, path
│       ├── ConflictException.java      # 409 duplicate email
│       └── UnauthorizedException.java  # 401 auth failure
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/V1__init.sql
├── src/test/java/com/jobtracker/auth/
│   ├── controller/AuthControllerTest.java
│   ├── controller/TestSecurityConfig.java
│   └── service/AuthServiceTest.java
├── Dockerfile
├── .dockerignore
└── pom.xml
```

## Implementation Overview

1. **Register** (`POST /auth/register`): validates `@Valid RegisterRequest`, checks for duplicate email (409), hashes password with BCrypt, saves `User`, generates JWT, returns `AuthResponse`.
2. **Login** (`POST /auth/login`): loads user by email, verifies BCrypt password match (401 if fail), generates JWT, returns `AuthResponse`.
3. **Me** (`GET /auth/me`): `@PreAuthorize` — Spring Security principal contains `userId` set by `JwtAuthenticationFilter`. Loads `User` by ID, returns `UserResponse`.
4. **JWT structure**: HS256, subject = `userId.toString()`, custom claim `email`, signed with `JWT_SECRET` from env var.

## Implementation Details & Gotchas

- **JWT_SECRET must match all services**: `JwtUtil.java` in auth-service, application-service, and notification-service all use the same `JWT_SECRET` env var (injected from AWS Secrets Manager in production). If they diverge, downstream services will reject all tokens with 401.
- **BCrypt**: password hashing uses `BCryptPasswordEncoder` with default strength (10 rounds). Never store plaintext passwords.
- **Token expiry**: controlled by `jwt.expiration-ms` (default 24 hours = 86400000 ms). Configurable via env var `JWT_EXPIRATION_MS`.
- **`ddl-auto: validate`**: Hibernate validates schema against running DB but never modifies it. Flyway owns all DDL. Adding a column requires a new `V{n}__*.sql` migration file.
- **Spring Security 6 (jakarta.* namespace)**: uses `SecurityFilterChain` bean pattern — not the deprecated `WebSecurityConfigurerAdapter`.
- **No shared user lookup between services**: downstream services extract `userId` from the JWT claim — they never call auth-service to validate a user. The JWT is the sole source of truth for the user's identity.

## Dependencies

Key `pom.xml` dependencies:
```xml
<parent>spring-boot-starter-parent 3.2.1</parent>
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-validation
spring-boot-starter-actuator
mysql-connector-j (runtime)
flyway-core + flyway-mysql
jjwt-api 0.12.3 + jjwt-impl + jjwt-jackson
spring-boot-starter-test + spring-security-test (test scope)
```

## Usage

```bash
# Requires MySQL with auth_db database accessible
# Environment variables (see infra/.env.example):
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_DATABASE=auth_db
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=<password>
export JWT_SECRET=<hex-secret>
export JWT_EXPIRATION_MS=86400000

mvn spring-boot:run
# → http://localhost:8081
# → http://localhost:8081/actuator/health
```

Flyway runs automatically on startup and applies any pending migrations.

## Testing

```bash
mvn test
# Runs AuthControllerTest + AuthServiceTest
# Uses @SpringBootTest with TestSecurityConfig to bypass JWT filter in tests
```

Key test scenarios:
- Duplicate email registration returns 409
- Invalid password login returns 401
- `GET /auth/me` without token returns 401
- Valid login returns token + user info

## Related

- `services/application-service/security/JwtUtil.java` — copy of JWT validation logic (same `JWT_SECRET`)
- `services/notification-service/security/JwtUtil.java` — same
- `infra/terraform/secrets.tf` — `jat-staging/jwt/secret` in Secrets Manager
- `infra/terraform/ecs.tf` — ECS task definition for auth-service (env var injection)
