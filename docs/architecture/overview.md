# Chrono Platform — Architecture Overview

## System Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                        Client Layer                              │
│                  Angular 19  (Port 4200)                         │
└─────────────────────────────┬────────────────────────────────────┘
                              │ HTTP
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│                       API Gateway  :8080                         │
│   JWT validation · header injection · rate limiting (Redis)      │
└──────┬──────────┬──────────┬──────────┬──────────┬──────────────┘
       │          │          │          │          │
       ▼          ▼          ▼          ▼          ▼
  Auth :8081  User :8082  Tenant   Mapper    Engine :8085
               Service    :8083    :8084
                                              │ Feign
                                              ▼
                                         Mapper :8084 (active rule)

                    Orchestrator :8086
                     │ Feign         │ Feign
                     ▼               ▼
                 Engine :8085    ETL :8087
                                     │ Feign
                                     ▼
                                 Engine :8085

┌──────────────────────────────────────────────────────────────────┐
│                     Platform Services                            │
│   Service Registry :8761 (Eureka)   Config Server :8888          │
├──────────────────────────────────────────────────────────────────┤
│                     Infrastructure                               │
│   PostgreSQL :5432 (single DB, 7 schemas)   Redis :6379          │
└──────────────────────────────────────────────────────────────────┘
```

## Service Inventory

| Service            | Port | Schema              | Redis | Purpose                                   |
|--------------------|------|---------------------|-------|-------------------------------------------|
| api-gateway        | 8080 | —                   | ✓     | JWT filter, routing, rate limiting        |
| auth-service       | 8081 | `chrono_auth`       | ✓     | Login, JWT issue/refresh, token blacklist |
| user-service       | 8082 | `chrono_users`      | ✓     | User profiles, role assignment            |
| tenant-service     | 8083 | `chrono_tenants`    | —     | Tenant CRUD, plan management              |
| mapper-service     | 8084 | `chrono_mapper`     | ✓     | Mapping rules, publish/activate lifecycle |
| engine-service     | 8085 | `chrono_engine`     | ✓     | Record transformation, idempotency cache  |
| orchestrator       | 8086 | `chrono_orchestrator`| —    | Saga workflows (loan processing, ETL)     |
| etl-service        | 8087 | `chrono_etl`        | —     | Spring Batch ETL jobs                     |
| service-registry   | 8761 | —                   | —     | Eureka service discovery                  |
| config-server      | 8888 | —                   | —     | Centralised Spring Cloud Config           |

## Key Design Decisions

### Multi-Schema Single Database
All services share `chrono_db` on PostgreSQL but own isolated schemas. Pros: simpler ops for dev/staging; schema-level isolation prevents cross-service query leakage. Migration path to separate DB instances per service is straightforward (change JDBC URL only).

### JWT at the Gateway
The API Gateway validates JWT signature, expiry, and Redis blacklist on every request. Downstream services are fully trusted — they read `X-User-Id`, `X-Tenant-Id`, `X-User-Role` headers injected by the gateway and never re-validate tokens. This centralises security logic and keeps services stateless.

### Idempotency Strategy
- **Engine**: `(tenantId, idempotencyKey)` unique constraint + 24h Redis cache
- **ETL**: `(tenantId, sourceRef, batchDate)` unique constraint
- **Orchestrator**: correlation-scoped workflows prevent duplicate saga starts

### Async Processing
- Orchestrator: `@Async("orchestratorExecutor")` — returns `202 ACCEPTED` immediately
- ETL: `@Async("etlExecutor")` — Spring Batch jobs run off the request thread
- Thread pools are configurable per service via `application.yml`

### Circuit Breakers
All inter-service Feign clients have Resilience4j circuit breakers with fallbacks returning `503` so a downstream outage degrades gracefully rather than cascading.

## Startup Order

```
postgres + redis  →  service-registry  →  config-server
     →  auth-service, user-service, tenant-service, mapper-service
     →  engine-service
     →  orchestrator-service, etl-service
     →  api-gateway
```

Docker Compose encodes this with `depends_on: condition: service_healthy`.

## Technology Stack

| Layer       | Technology                                      |
|-------------|-------------------------------------------------|
| Language    | Java 8 (upgrade path to Java 21 planned)        |
| Framework   | Spring Boot 2.7.18, Spring Cloud 2021.0.9       |
| Persistence | PostgreSQL 15, Spring Data JPA, Liquibase       |
| Cache       | Redis 7, Spring Cache                           |
| Batch       | Spring Batch (ETL service)                      |
| Discovery   | Netflix Eureka                                  |
| Config      | Spring Cloud Config (classpath / Git)           |
| Gateway     | Spring Cloud Gateway (reactive)                 |
| HTTP Client | OpenFeign + Resilience4j                        |
| Auth        | JJWT 0.11.5, BCrypt                             |
| Frontend    | Angular 19, standalone components, OnPush       |
| Build       | Maven multi-module, Node/npm                    |
| Container   | Docker Compose (local dev)                      |
