# infra/

## Purpose

Local development infrastructure and environment configuration. Contains Docker Compose files for running MySQL, Redis, and RabbitMQ locally, plus the full-stack compose for running all services together. The `terraform/` subdirectory contains the AWS infrastructure-as-code.

## Key Files

| File | Role |
|---|---|
| `docker-compose.yml` | Infra-only: MySQL 8.0, Redis 7, RabbitMQ 3 with management UI. Use for local development when running Spring Boot services on the host. |
| `docker-compose.full.yml` | Full stack: infra + all 4 Spring Boot services + Vite frontend. One-command local env. |
| `init.sql` | MySQL initialization: creates `auth_db`, `application_db`, `notification_db` databases. Runs once on first container start. |
| `.env.example` | Template for all required environment variables. Copy to `.env` and fill in values before running compose. |
| `.env` | Local secrets (gitignored — never commit). Generated from `.env.example`. |

## Structure

```
infra/
├── docker-compose.yml          # MySQL + Redis + RabbitMQ
├── docker-compose.full.yml     # Above + all 4 services + frontend
├── init.sql                    # DB initialization script
├── .env.example                # Variable template (committed)
├── .env                        # Local secrets (gitignored)
└── terraform/                  # AWS IaC — see terraform/CLAUDE.md
```

## Environment Variables

All variables in `.env` / `.env.example`:

| Variable | Used by | Description |
|---|---|---|
| `MYSQL_ROOT_PASSWORD` | MySQL container | Root password |
| `MYSQL_USERNAME` | Spring Boot services | App DB user |
| `MYSQL_PASSWORD` | Spring Boot services | App DB password |
| `REDIS_PASSWORD` | Redis container + services | Redis auth (can be empty for local) |
| `RABBITMQ_USERNAME` | RabbitMQ + services | MQ user |
| `RABBITMQ_PASSWORD` | RabbitMQ + services | MQ password |
| `JWT_SECRET` | All 3 downstream services | HS256 signing key — **must be identical across all services** |
| `JWT_EXPIRATION_MS` | auth-service | Token lifetime in ms (default 86400000 = 24h) |
| `REDIS_DASHBOARD_TTL_MINUTES` | application-service | Dashboard cache TTL |

Generate a secure `JWT_SECRET` with:
```bash
openssl rand -hex 32
```

## Docker Compose Topology

### `docker-compose.yml` (infra only)

| Container | Image | Port | Health check |
|---|---|---|---|
| `jat_mysql` | mysql:8.0 | 3306 | `mysqladmin ping` |
| `jat_redis` | redis:7-alpine | 6379 | `redis-cli ping` |
| `jat_rabbitmq` | rabbitmq:3-management | 5672, 15672 | `rabbitmq-diagnostics -q ping` |

All containers use named volumes for data persistence. The `init.sql` is bind-mounted into the MySQL init directory.

### `docker-compose.full.yml` (full stack)

Extends the infra compose and adds:
- `auth-service` (builds from `../services/auth-service/Dockerfile`) → :8081
- `application-service` → :8082
- `notification-service` → :8083
- `gateway-service` → :8080
- `frontend` (Vite dev server) → :5173

All Spring Boot services `depends_on` the infra containers with `condition: service_healthy`.

## Implementation Details & Gotchas

- **`init.sql` runs only once**: MySQL `docker-entrypoint-initdb.d/` scripts run only when the volume is empty. To re-run `init.sql`, remove the volume: `docker compose down -v`.
- **JWT_SECRET consistency**: The same `JWT_SECRET` value must be present in all three downstream services (auth, application, notification). If they diverge, auth-service issues tokens that other services reject with 401.
- **Redis auth in local dev**: `REDIS_PASSWORD` can be empty for local development. In production, Redis is in a private subnet and accessed without a password (security through network isolation).
- **RabbitMQ management UI**: accessible at `http://localhost:15672` with `RABBITMQ_USERNAME`/`RABBITMQ_PASSWORD` from `.env`.
- **Named volumes vs bind mounts**: DB data is stored in named Docker volumes (survives `docker compose stop`). Use `docker compose down -v` to wipe all data and start fresh.
- **`docker-compose.full.yml` rebuild**: when service code changes, use `docker compose -f docker-compose.full.yml up --build` to rebuild images. Otherwise stale images are reused.

## Usage

### Option A — Infra only (recommended for development)

```bash
cd infra
cp .env.example .env   # fill in values
docker compose up -d
docker compose ps      # wait for all: healthy
```

Then export env vars and start Spring Boot services separately:
```bash
export $(grep -v '^#' .env | xargs)
export REDIS_HOST=localhost RABBITMQ_HOST=localhost
# start each service: cd services/auth-service && mvn spring-boot:run
```

### Option B — Full stack in Docker

```bash
cd infra
cp .env.example .env   # fill in values
docker compose -f docker-compose.full.yml up --build
# Wait for all services to start
# → http://localhost:5173 (frontend)
# → http://localhost:8080 (gateway)
```

## Related

- `infra/terraform/` — AWS infrastructure provisioning
- `services/*/Dockerfile` — multi-stage builds used by `docker-compose.full.yml`
- Root `CLAUDE.md` → Docker Compose and Dockerfile conventions
