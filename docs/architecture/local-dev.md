# Local Development Guide

## Prerequisites

- Docker Desktop
- JDK 8 + Maven 3.8+
- Node 18+ + npm

## Option A — Full Docker Stack

```bash
# From repo root
docker compose up -d

# Frontend (separate terminal)
cd frontend && npm start
```

Services come up in dependency order. Total cold-start: ~3–4 min (image builds first time).

Check health:
```bash
docker compose ps
curl http://localhost:8761   # Eureka dashboard
curl http://localhost:8080/actuator/health  # Gateway
```

## Option B — Infrastructure Only (Recommended for Development)

Run just postgres + redis in Docker; start Spring Boot services natively for faster iteration.

```bash
# Start infrastructure
docker compose up -d postgres redis

# Start platform services (order matters)
cd backend
mvn -pl service-registry spring-boot:run &
mvn -pl config-server spring-boot:run &

# Wait ~15s for Eureka + Config to be ready, then start app services
mvn -pl auth-service spring-boot:run &
mvn -pl user-service spring-boot:run &
mvn -pl tenant-service spring-boot:run &
mvn -pl mapper spring-boot:run &
mvn -pl engine spring-boot:run &
mvn -pl orchestrator spring-boot:run &
mvn -pl etl spring-boot:run &

# Start gateway last
mvn -pl api-gateway spring-boot:run &

# Frontend
cd ../frontend && npm start
```

## Build

```bash
# Full backend build (skip tests)
cd backend && mvn clean package -DskipTests

# Single service
cd backend && mvn -pl engine -am package -DskipTests

# Frontend
cd frontend && npm run build
```

## Seed Credentials

| Email               | Password   | Role            |
|---------------------|------------|-----------------|
| admin@chrono.dev    | password   | PLATFORM_ADMIN  |

Seeded via Liquibase changeset `004-seed-platform-admin` — runs automatically on `auth-service` startup in `dev` and `docker` contexts.

## Environment Variables

All services accept these overrides (used by docker-compose for container networking):

| Variable                                      | Default (local)              |
|-----------------------------------------------|------------------------------|
| `SPRING_DATASOURCE_URL`                        | `jdbc:postgresql://localhost:5432/chrono_db?currentSchema=<schema>` |
| `SPRING_REDIS_HOST`                            | `localhost`                  |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`        | `http://localhost:8761/eureka/` |
| `SPRING_CONFIG_IMPORT`                        | `optional:configserver:http://localhost:8888` |
| `DB_USERNAME` / `DB_PASSWORD`                 | `postgres` / `postgres`      |
| `JWT_SECRET`                                  | dev secret (see config)      |

## Ports Quick Reference

| Service            | Port |
|--------------------|------|
| Angular frontend   | 4200 |
| API Gateway        | 8080 |
| Auth Service       | 8081 |
| User Service       | 8082 |
| Tenant Service     | 8083 |
| Mapper Service     | 8084 |
| Engine Service     | 8085 |
| Orchestrator       | 8086 |
| ETL Service        | 8087 |
| Service Registry   | 8761 |
| Config Server      | 8888 |
| PostgreSQL         | 5432 |
| Redis              | 6379 |
| Zipkin (tracing)   | 9411 |
