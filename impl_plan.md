# Implementation Plan - Chrono Application

## Project Overview
A microservices-based application with Angular 19 frontend, multiple Spring Boot backend services, and PostgreSQL database.

> **Java Strategy**: Built on **JDK 8 + Spring Boot 2.7.18** initially. A planned upgrade to **JDK 21 + Spring Boot 3.x** will be conducted as a separate activity once the application is stable in production. All code must avoid JDK 9+ constructs to ease future migration.

---

## Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Angular 19    │    │   API Gateway   │    │  Config Server  │
│   Frontend      │◄──►│  Port: 8080     │    │  Port: 8888     │
│   Port: 4200    │    └────────┬────────┘    └─────────────────┘
└─────────────────┘             │
                                ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Service        │    │   Auth Service  │    │   User Service  │
│  Registry       │    │   Port: 8081    │    │   Port: 8082    │
│  Port: 8761     │    └─────────────────┘    └─────────────────┘
└─────────────────┘
                                │
                                ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Tenant Service  │    │  Mapper Service │    │  Engine Service │
│  Port: 8083     │    │  Port: 8084     │    │  Port: 8085     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Orchestrator   │    │   ETL Service   │    │     Redis       │
│  Port: 8086     │    │  Port: 8087     │    │  Port: 6379     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │   PostgreSQL    │
                       │   Port: 5432    │
                       └─────────────────┘
```

---

## Backend Services

### 1. Commons Library (Maven Project)

> **Warning**: Keep commons lean. Every service depends on it — scope creep here forces rebuilds and redeployments across all services. Commons must contain only truly shared, stable contracts.

**Purpose**: Minimal shared library — constants, DTOs, base classes, exceptions only
- **Location**: `backend/commons/`
- **Dependencies**: Spring Boot Starter, Jackson, Validation, JPA, Lombok
- **Key Components**:
  - **Constants**: `ApiConstants.API_V1 = "/api/v1"`, error codes, error messages
  - **Enums**: User roles, processing status, error types, audit actions
  - **DTOs**: `ApiResponse<T>`, `ErrorResponse`, `PageRequest`, `PageResponse<T>`, `AuditInfo`
  - **Exceptions**: Custom exception hierarchy — `BaseException`, `ResourceNotFoundException`, `BusinessException`, `TenantViolationException`
  - **Base Classes**: `Auditable`, `BaseEntity`, `BaseController`, `BaseService`
  - **Utils**: `DateUtils`, `SecurityUtils`, `JsonUtils`

#### Commons Coding Standards (Enforced)
- **No `@Data` on JPA entities** — breaks equals/hashCode and Hibernate proxies
- **All JPA entities extend `BaseEntity`** which defines `equals`/`hashCode` based on `id` field only
- **Lombok allowed**: `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`, `@NoArgsConstructor`
- **Pagination**: All list endpoints must use `PageRequest` (page, size, sort) / `PageResponse<T>` from commons
- **Soft Delete**: All business entities must include `deletedAt` timestamp. Hard deletes are forbidden on audited entities

#### BaseEntity Pattern
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;

    private LocalDateTime deletedAt; // soft delete

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEntity)) return false;
        BaseEntity that = (BaseEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
```

---

### 2. Config Server (Spring Cloud Config)
**Purpose**: Centralized configuration management for all services
- **Location**: `backend/config-server/`
- **Port**: 8888
- **Dependencies**: Spring Cloud Config Server, Eureka Client
- **Key Features**:
  - Centralized `application.yml` per service (e.g. `auth-service-dev.yml`)
  - Environment profiles: `dev`, `test`, `prod`
  - Services bootstrap via `spring.config.import=configserver:`
  - Config stored in classpath (local dev) or Git repo (prod)
- **Startup Order**: Starts immediately after PostgreSQL and Redis; must be up before all other services

---

### 3. Service Registry (Eureka Server)
**Purpose**: Service discovery and registration
- **Location**: `backend/service-registry/`
- **Port**: 8761
- **Dependencies**: Spring Cloud Netflix Eureka Server
- **Key Features**:
  - Service registration and dynamic discovery
  - Health checks — services deregister on unhealthy status
  - Client-side load balancing via Eureka + Feign

---

### 4. API Gateway (Spring Cloud Gateway)
**Purpose**: Single entry point for all client requests
- **Location**: `backend/api-gateway/`
- **Port**: 8080
- **Dependencies**: Spring Cloud Gateway, Eureka Client, Redis (for rate limiting and token blacklist)
- **Key Features**:
  - Route configuration: `/api/v1/**` mapped to services via Eureka service names
  - **JWT Pre-filter**: Validates JWT signature/expiry, rejects blacklisted tokens (Redis check), injects `X-User-Id`, `X-User-Role`, `X-Tenant-Id` headers
  - **Rate Limiting**: `RequestRateLimiterFilter` backed by Redis — per-tenant and per-user limits configurable
  - CORS handling (configured for Angular dev origin)
  - No business logic — routing and security only

#### Rate Limiting Configuration
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/v1/auth/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 20
                redis-rate-limiter.burstCapacity: 40
                key-resolver: "#{@tenantKeyResolver}"
```

---

### 5. Auth Service
**Purpose**: Authentication and authorization
- **Location**: `backend/auth/`
- **Port**: 8081
- **Dependencies**: Spring Security, jjwt 0.11.x, Eureka Client, Commons, PostgreSQL, Redis
- **Database Schema**: `chrono_auth`
- **Key Features**:
  - Login endpoint — validates credentials, issues JWT + refresh token
  - JWT generation with claims: `userId`, `tenantId`, `role`
  - **Token Revocation**: On logout, token JTI (JWT ID) stored in Redis with TTL = remaining token lifetime; Gateway blacklist check uses this
  - Refresh token rotation — refresh token stored in DB, invalidated on use
  - BCrypt password hashing (strength 12)
  - `@PreAuthorize` for method-level RBAC
- **Bootstrap**: Liquibase changeset seeds initial `PLATFORM_ADMIN` user and default roles on first startup
- **Standard Components**:
  - `SecurityConfig extends WebSecurityConfigurerAdapter`
  - `@ControllerAdvice` GlobalExceptionHandler
  - Auditable entities via `BaseEntity`

---

### 6. User Service
**Purpose**: User management operations
- **Location**: `backend/user/`
- **Port**: 8082
- **Dependencies**: Spring Boot Web, Eureka Client, OpenFeign, Commons, PostgreSQL, Redis, Resilience4j
- **Database Schema**: `chrono_user`
- **Caching**: User profile cached in Redis (TTL: 10 min). Cache evicted on profile update.
- **Key Features**:
  - Multi-tenant user CRUD with **tenant isolation validation** (see Tenant Isolation section)
  - User profile management with DTO projections (no entity exposure)
  - Pagination on all list endpoints via `PageRequest` / `PageResponse<T>`
  - Feign client to Tenant Service for tenant existence validation
- **Standard Components**:
  - `SecurityConfig extends WebSecurityConfigurerAdapter`
  - `@ControllerAdvice` GlobalExceptionHandler
  - Auditable entities via `BaseEntity`

---

### 7. Tenant Service
**Purpose**: Tenant lifecycle and configuration management
- **Location**: `backend/tenant-service/`
- **Port**: 8083
- **Dependencies**: Spring Boot Web, Eureka Client, OpenFeign, Commons, PostgreSQL, Redis, Resilience4j
- **Database Schema**: `chrono_tenant`
- **Caching**: Tenant configuration cached in Redis (TTL: 30 min). Tenant config is read-heavy, change-rarely data — cache is critical here.
- **Key Features**:
  - Tenant onboarding and provisioning (creates initial tenant admin)
  - Tenant configuration and plan management
  - Cross-tenant admin operations (PLATFORM_ADMIN only — enforced at Gateway + `@PreAuthorize`)
  - Feign client to User Service for tenant-user association

---

### 8. Mapper Service
**Purpose**: Data mapping rule configuration
- **Location**: `backend/mapper/`
- **Port**: 8084
- **Dependencies**: Spring Boot Web, Eureka Client, OpenFeign, Commons, PostgreSQL, Redis, Resilience4j
- **Database Schema**: `chrono_mapper`
- **Caching**: Active mapping versions cached in Redis (TTL: 15 min). Engine fetches these on every loan processing request — cache mandatory.
- **Key Features**:
  - LOS-to-Strategy-One mapping rule management
  - Mapping version control — only one version active per tenant at a time
  - Schema validation on mapping configuration
  - Multi-tenant mapping isolation

---

### 9. Engine Service
**Purpose**: Core loan processing business logic
- **Location**: `backend/engine/`
- **Port**: 8085
- **Dependencies**: Spring Boot Web, Eureka Client, OpenFeign, Commons, PostgreSQL, Redis, Resilience4j
- **Database Schema**: `chrono_engine`
- **Key Features**:
  - Loan processing execution
  - Fetches mapping rules from Mapper Service (cached in Redis; circuit breaker fallback to cache)
  - **Idempotency**: All processing requests require a client-supplied `Idempotency-Key` header; duplicate requests return cached result within 24h
  - Strategy One integration (external system call — wrapped in circuit breaker)
  - Multi-tenant processing isolation — tenant validation on every request

---

### 10. Orchestrator Service
**Purpose**: Workflow orchestration across services
- **Location**: `backend/orchestrator/`
- **Port**: 8086
- **Dependencies**: Spring Boot Web, Eureka Client, OpenFeign, Commons, PostgreSQL, Resilience4j
- **Database Schema**: `chrono_orchestrator`
- **Key Features**:
  - Workflow definition and execution state management
  - **Async coordination**: Long-running workflows use `@Async` Spring executor. Orchestrator initiates step, stores state in DB, polls or receives callback.
  - Feign clients to Engine, ETL, Mapper — each with circuit breaker
  - **Distributed Transaction Strategy**: No two-phase commit. Each step is a local transaction. On failure, compensating transactions are executed (saga pattern). Compensation steps logged to DB.
  - Task scheduling via Spring `@Scheduled`

---

### 11. ETL Service
**Purpose**: Extract, Transform, Load operations
- **Location**: `backend/etl/`
- **Port**: 8087
- **Dependencies**: Spring Boot Web, Eureka Client, OpenFeign, Commons, PostgreSQL, Resilience4j, Spring Batch
- **Database Schema**: `chrono_etl` (includes Spring Batch metadata tables: `BATCH_JOB_INSTANCE`, `BATCH_JOB_EXECUTION`, `BATCH_STEP_EXECUTION`, etc.)
- **Key Features**:
  - **Idempotency**: Each ETL job keyed by `(tenantId, sourceRef, batchDate)`. Re-runs for same key skip completed jobs, resume failed ones via Spring Batch restart.
  - **Async execution**: Job submission returns immediately with `jobId`; polling endpoint `/api/v1/etl/jobs/{jobId}` for status
  - Data extraction from LOS systems
  - Data transformation via Engine Service (Feign + circuit breaker)
  - Data loading to Strategy One
  - Multi-tenant ETL isolation

---

## Inter-Service Communication

### Synchronous (OpenFeign)

| Caller | Target | Purpose |
|---|---|---|
| User Service | Tenant Service | Validate tenant exists on user create |
| Tenant Service | User Service | Create initial tenant admin on onboarding |
| Engine Service | Mapper Service | Fetch active mapping rules |
| Orchestrator | Engine, ETL, Mapper | Workflow step execution |
| ETL Service | Engine | Transformation during load phase |

**Feign Client Pattern** (all clients follow this):
```java
@FeignClient(name = "mapper-service", fallback = MapperServiceFallback.class,
             configuration = FeignConfig.class)
public interface MapperServiceClient {
    @GetMapping(ApiConstants.API_V1 + "/mappings/active")
    ApiResponse<MappingRuleDto> getActiveMappingRules(
        @RequestHeader("X-Tenant-Id") String tenantId);
}
```

**Feign Timeout Configuration** (via Config Server — applies to all services):
```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 3000   # 3s connect
        readTimeout: 10000     # 10s read
  compression:
    request.enabled: true
    response.enabled: true
```

### Circuit Breaker (Resilience4j)

Every Feign client must declare a `@CircuitBreaker` fallback. Non-negotiable.

```yaml
resilience4j:
  circuitbreaker:
    instances:
      default:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
  retry:
    instances:
      default:
        maxAttempts: 3
        waitDuration: 500ms
  timelimiter:
    instances:
      default:
        timeoutDuration: 5s
```

### Async Processing

| Service | Mechanism | Use Case |
|---|---|---|
| ETL Service | `@Async` + Spring Batch | Job submission non-blocking |
| Orchestrator | `@Async` Spring executor | Long-running workflow steps |
| Engine | Synchronous | Real-time loan processing (latency-sensitive) |

No external message broker in initial phase. Async via Spring `@Async` with a bounded `ThreadPoolTaskExecutor`. Message broker (RabbitMQ) added as a future enhancement if workflow complexity grows.

### JWT Propagation Between Services
1. Client sends `Authorization: Bearer <token>` to API Gateway
2. Gateway validates JWT signature and expiry
3. Gateway checks token JTI against Redis blacklist (logout/revocation check)
4. Gateway extracts claims, injects: `X-User-Id`, `X-User-Role`, `X-Tenant-Id`
5. Downstream services receive these headers — **no re-validation of JWT**
6. All service ports are internal only — external traffic goes through Gateway exclusively

---

## Tenant Isolation Strategy

**Threat**: A user belonging to Tenant A manipulates `X-Tenant-Id` header (if header were user-controlled) to access Tenant B data.

**Mitigation**:
- `X-Tenant-Id` is injected by the Gateway, not the client. Client cannot override Gateway-injected headers.
- Every service's base service layer cross-checks: the requesting user's `tenantId` (from `X-Tenant-Id`) must match the `tenantId` on the resource being accessed.
- `TenantViolationException` (defined in Commons) thrown and mapped to HTTP 403 when mismatch detected.
- PLATFORM_ADMIN role bypasses tenant check — enforced via `@PreAuthorize`.

```java
// In BaseService — enforced on every data access
protected void assertTenantAccess(String resourceTenantId, String requestingTenantId) {
    if (!resourceTenantId.equals(requestingTenantId)) {
        log.warn("Tenant violation attempt: requesting={}, resource={}", requestingTenantId, resourceTenantId);
        throw new TenantViolationException("Access denied: tenant mismatch");
    }
}
```

---

## Caching Strategy (Redis)

| Cache | Service | TTL | Eviction Trigger |
|---|---|---|---|
| User profile | User Service | 10 min | Profile update |
| Tenant config | Tenant Service | 30 min | Config update |
| Active mapping rules | Mapper Service | 15 min | Mapping publish |
| JWT blacklist (revoked tokens) | Gateway | Token remaining TTL | Logout / security event |
| Rate limiter counters | Gateway | Rolling window | Automatic |
| Idempotency result cache | Engine Service | 24 h | None (TTL-based) |

**Cache Key Convention**: `{service}:{entity}:{tenantId}:{id}`
Example: `user:profile:tenant-abc:42`

**Redis Configuration** (per service, via Config Server):
```yaml
spring:
  redis:
    host: localhost
    port: 6379
  cache:
    type: redis
    redis:
      time-to-live: 600000  # override per cache with @CacheConfig
```

---

## Database Strategy

### Schema Isolation (One Schema Per Service)

| Service | Schema | HikariCP Pool Size |
|---|---|---|
| Auth Service | `chrono_auth` | 5 |
| User Service | `chrono_user` | 8 |
| Tenant Service | `chrono_tenant` | 5 |
| Mapper Service | `chrono_mapper` | 5 |
| Engine Service | `chrono_engine` | 10 |
| Orchestrator | `chrono_orchestrator` | 8 |
| ETL Service | `chrono_etl` | 10 |

> **Pool Sizing Note**: Total max connections = 51. PostgreSQL `max_connections` must be set to at least 75 (add headroom for admin connections). Default is 100 — sufficient, but must be verified.

- No cross-schema JPA. Services communicate via API contracts only.
- Each service owns its Liquibase changelog under `src/main/resources/db/changelog/`.
- Spring Batch metadata tables live in `chrono_etl` schema.

### Indexing Strategy
Every service must define indexes for:
- `tenant_id` column — on every tenant-scoped table
- Foreign key columns
- `deleted_at` — partial index `WHERE deleted_at IS NULL` for soft-delete queries
- `created_at` — for time-based ordering on audit/history tables

### Soft Delete
All business entities use soft delete. Hard delete forbidden on audited data.
```sql
-- Example: get active users for tenant
SELECT * FROM chrono_user.users
WHERE tenant_id = :tenantId
AND deleted_at IS NULL;
```

### Bootstrap / Seed Data
Auth Service Liquibase initial changeset creates:
- `PLATFORM_ADMIN` role
- `TENANT_ADMIN` role
- `TENANT_USER` role
- Default system `PLATFORM_ADMIN` user (credentials from env vars — not hardcoded)

---

## API Versioning Strategy

All REST endpoints versioned under `/api/v1/`:

```
/api/v1/auth/**         → Auth Service
/api/v1/users/**        → User Service
/api/v1/tenants/**      → Tenant Service
/api/v1/mappings/**     → Mapper Service
/api/v1/engine/**       → Engine Service
/api/v1/orchestrator/** → Orchestrator Service
/api/v1/etl/**          → ETL Service
```

- `ApiConstants.API_V1 = "/api/v1"` defined in Commons — never hardcode the prefix
- Breaking changes: add `/api/v2/` routes; keep `/api/v1/` alive until clients migrate
- Non-breaking additions (new fields, new endpoints): no version bump required

### Pagination Standard
All list endpoints accept and return:
```
GET /api/v1/users?page=0&size=20&sort=createdAt,desc
```
Response:
```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8
}
```

### API Documentation
- **Library**: `springdoc-openapi-ui` 1.7.x (compatible with Spring Boot 2.7.x, JDK 8)
- Each service exposes `/swagger-ui.html` and `/v3/api-docs`
- Gateway aggregates all service docs at `/swagger-ui.html` (grouped by service)
- Added in Phase 2 alongside first service implementations

---

## Frontend

### Multi-Tenant Portal Architecture (Angular 19)
**Purpose**: Role-based user interface with separate portals for different user types
- **Location**: `frontend/web/`
- **Port**: 4200 (development)
- **Dependencies**: Angular 19, TailwindCSS 3.x, Material Icons, `@angular/cdk`, `@angular/animations`
- **Architecture**: Multi-portal with lazy loading, role-based routing, and a unified design system

#### Portal Structure
```
frontend/web/src/app/portals/
├── shared-portal/       # Landing, Login, Signup (no sidebar)
├── hub-portal/          # PLATFORM_ADMIN
├── console-portal/      # PLATFORM_TENANT_ADMIN
└── workspace-portal/    # PLATFORM_USER
```

---

## UI Design System

> The design system is the single source of truth for all visual decisions. No hardcoded colors, font sizes, or spacing values anywhere in component code — all values reference design tokens only.

### Design Token Architecture

Tokens are defined as CSS custom properties on `:root` in `styles/tokens.scss` and consumed by Tailwind config and component styles. Three layers:

```
Design Tokens (CSS vars)  →  Tailwind Config  →  Component Classes
       ↓                          ↓
  tokens.scss              tailwind.config.js
```

### File Structure — Design System
```
frontend/web/src/
├── styles/
│   ├── tokens.scss          # All CSS custom properties (single source of truth)
│   ├── typography.scss      # Font imports, base type styles
│   ├── base.scss            # Reset, root defaults
│   ├── components/
│   │   ├── _buttons.scss
│   │   ├── _badges.scss
│   │   ├── _cards.scss
│   │   ├── _tables.scss
│   │   ├── _forms.scss
│   │   ├── _sidebar.scss
│   │   ├── _navbar.scss
│   │   ├── _modals.scss
│   │   ├── _toasts.scss
│   │   └── _skeletons.scss
│   └── styles.scss          # Root import file
└── tailwind.config.js       # Extends Tailwind with design tokens
```

---

### Color System

#### Base Palette (Raw Values — never used directly in components)
```scss
// styles/tokens.scss

// Neutrals
--color-neutral-50:  #F8FAFC;
--color-neutral-100: #F1F5F9;
--color-neutral-200: #E2E8F0;
--color-neutral-300: #CBD5E1;
--color-neutral-400: #94A3B8;
--color-neutral-500: #64748B;
--color-neutral-600: #475569;
--color-neutral-700: #334155;
--color-neutral-800: #1E293B;
--color-neutral-900: #0F172A;

// Brand Blue (Primary)
--color-brand-50:  #EFF6FF;
--color-brand-100: #DBEAFE;
--color-brand-200: #BFDBFE;
--color-brand-300: #93C5FD;
--color-brand-400: #60A5FA;
--color-brand-500: #3B82F6;   // Primary action
--color-brand-600: #2563EB;   // Primary hover
--color-brand-700: #1D4ED8;
--color-brand-800: #1E3A5F;   // Deep navy (sidebar accent)
--color-brand-900: #1E3050;

// Semantic
--color-success-light: #D1FAE5;
--color-success:       #10B981;
--color-success-dark:  #065F46;

--color-warning-light: #FEF3C7;
--color-warning:       #F59E0B;
--color-warning-dark:  #92400E;

--color-danger-light:  #FEE2E2;
--color-danger:        #EF4444;
--color-danger-dark:   #991B1B;

--color-info-light:    #DBEAFE;
--color-info:          #3B82F6;
--color-info-dark:     #1E40AF;
```

#### Semantic Tokens (used in components)
```scss
// Surface
--surface-page:       var(--color-neutral-50);    // Page background
--surface-card:       #FFFFFF;                     // Card/panel background
--surface-overlay:    rgba(15, 23, 42, 0.6);       // Modal overlay
--surface-input:      #FFFFFF;
--surface-input-focus:var(--color-brand-50);

// Text
--text-primary:       var(--color-neutral-900);    // Main content
--text-secondary:     var(--color-neutral-500);    // Labels, hints
--text-muted:         var(--color-neutral-400);    // Placeholder, disabled
--text-inverse:       #FFFFFF;                     // On dark backgrounds
--text-link:          var(--color-brand-600);
--text-link-hover:    var(--color-brand-700);

// Border
--border-default:     var(--color-neutral-200);
--border-focus:       var(--color-brand-500);
--border-error:       var(--color-danger);

// Interactive
--color-primary:      var(--color-brand-500);
--color-primary-hover:var(--color-brand-600);
--color-primary-active:var(--color-brand-700);
```

---

### Portal Themes

Each portal has a distinct sidebar identity while sharing the same content area system. Theme applied via a CSS class on the root layout element.

```scss
// Hub Portal — PLATFORM_ADMIN  (Deep Navy)
.theme-hub {
  --sidebar-bg:           #0F172A;
  --sidebar-text:         #CBD5E1;
  --sidebar-text-active:  #FFFFFF;
  --sidebar-item-active:  #1E3A5F;
  --sidebar-item-hover:   rgba(255,255,255,0.06);
  --sidebar-icon-active:  var(--color-brand-400);
  --sidebar-border:       rgba(255,255,255,0.08);
  --navbar-bg:            #FFFFFF;
  --portal-accent:        var(--color-brand-500);
}

// Console Portal — PLATFORM_TENANT_ADMIN  (Deep Teal-Slate)
.theme-console {
  --sidebar-bg:           #0C1A2E;
  --sidebar-text:         #94A3B8;
  --sidebar-text-active:  #FFFFFF;
  --sidebar-item-active:  #0E3D4F;
  --sidebar-item-hover:   rgba(255,255,255,0.06);
  --sidebar-icon-active:  #2DD4BF;   // Teal accent
  --sidebar-border:       rgba(255,255,255,0.08);
  --navbar-bg:            #FFFFFF;
  --portal-accent:        #0D9488;   // Teal-600
}

// Workspace Portal — PLATFORM_USER  (Charcoal-Indigo)
.theme-workspace {
  --sidebar-bg:           #111827;
  --sidebar-text:         #9CA3AF;
  --sidebar-text-active:  #FFFFFF;
  --sidebar-item-active:  #1E1B4B;
  --sidebar-item-hover:   rgba(255,255,255,0.06);
  --sidebar-icon-active:  #818CF8;   // Indigo accent
  --sidebar-border:       rgba(255,255,255,0.08);
  --navbar-bg:            #FFFFFF;
  --portal-accent:        #6366F1;   // Indigo-500
}

// Shared / Public Portal  (Minimal light)
.theme-shared {
  --surface-page:    #F8FAFC;
  --portal-accent:   var(--color-brand-500);
}
```

---

### Typography System

**Font**: `Inter` — loaded via Google Fonts. Fallback: `-apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif`

```scss
// Typography scale
--font-family-base:  'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
--font-family-mono:  'JetBrains Mono', 'Fira Code', monospace;

// Size scale (rem based — base 16px)
--text-xs:   0.75rem;   // 12px — labels, badges
--text-sm:   0.875rem;  // 14px — table data, secondary
--text-base: 1rem;      // 16px — body
--text-lg:   1.125rem;  // 18px — card titles
--text-xl:   1.25rem;   // 20px — section headers
--text-2xl:  1.5rem;    // 24px — page titles
--text-3xl:  1.875rem;  // 30px — stat numbers
--text-4xl:  2.25rem;   // 36px — hero

// Weight
--font-regular:   400;
--font-medium:    500;
--font-semibold:  600;
--font-bold:      700;

// Line height
--leading-tight:  1.25;
--leading-normal: 1.5;
--leading-relaxed:1.625;

// Letter spacing
--tracking-tight:  -0.025em;
--tracking-normal: 0em;
--tracking-wide:   0.025em;
--tracking-widest: 0.1em;  // uppercase labels
```

---

### Spacing System

4px base unit. All spacing uses multiples of 4.

```scss
--space-1:  0.25rem;   //  4px
--space-2:  0.5rem;    //  8px
--space-3:  0.75rem;   // 12px
--space-4:  1rem;      // 16px
--space-5:  1.25rem;   // 20px
--space-6:  1.5rem;    // 24px
--space-8:  2rem;      // 32px
--space-10: 2.5rem;    // 40px
--space-12: 3rem;      // 48px
--space-16: 4rem;      // 64px
--space-20: 5rem;      // 80px
```

---

### Elevation / Shadow System

```scss
--shadow-xs:  0 1px 2px 0 rgba(0,0,0,0.05);
--shadow-sm:  0 1px 3px 0 rgba(0,0,0,0.10), 0 1px 2px -1px rgba(0,0,0,0.10);
--shadow-md:  0 4px 6px -1px rgba(0,0,0,0.10), 0 2px 4px -2px rgba(0,0,0,0.10);
--shadow-lg:  0 10px 15px -3px rgba(0,0,0,0.10), 0 4px 6px -4px rgba(0,0,0,0.10);
--shadow-xl:  0 20px 25px -5px rgba(0,0,0,0.10), 0 8px 10px -6px rgba(0,0,0,0.10);

// Contextual
--shadow-card:   var(--shadow-sm);
--shadow-navbar: 0 1px 0 0 var(--border-default);
--shadow-modal:  var(--shadow-xl);
--shadow-sidebar:4px 0 16px rgba(0,0,0,0.12);
```

---

### Border Radius System

```scss
--radius-sm:   0.25rem;   // 4px  — badges, tags
--radius-md:   0.375rem;  // 6px  — inputs, buttons
--radius-lg:   0.5rem;    // 8px  — cards
--radius-xl:   0.75rem;   // 12px — modals, larger cards
--radius-2xl:  1rem;      // 16px — feature cards
--radius-full: 9999px;    // pills
```

---

### Tailwind Config Extension

```javascript
// tailwind.config.js
const { fontFamily } = require('tailwindcss/defaultTheme');

module.exports = {
  content: ['./src/**/*.{html,ts}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', ...fontFamily.sans],
        mono: ['JetBrains Mono', ...fontFamily.mono],
      },
      colors: {
        brand: {
          50: 'var(--color-brand-50)',
          100: 'var(--color-brand-100)',
          500: 'var(--color-brand-500)',
          600: 'var(--color-brand-600)',
          700: 'var(--color-brand-700)',
          800: 'var(--color-brand-800)',
        },
        surface: {
          page:    'var(--surface-page)',
          card:    'var(--surface-card)',
          input:   'var(--surface-input)',
        },
        text: {
          primary:   'var(--text-primary)',
          secondary: 'var(--text-secondary)',
          muted:     'var(--text-muted)',
          inverse:   'var(--text-inverse)',
        },
        border: {
          default: 'var(--border-default)',
          focus:   'var(--border-focus)',
        },
        success: 'var(--color-success)',
        warning: 'var(--color-warning)',
        danger:  'var(--color-danger)',
        info:    'var(--color-info)',
      },
      boxShadow: {
        'card':   'var(--shadow-card)',
        'navbar': 'var(--shadow-navbar)',
        'modal':  'var(--shadow-modal)',
        'sidebar':'var(--shadow-sidebar)',
      },
      borderRadius: {
        DEFAULT: 'var(--radius-md)',
        sm:  'var(--radius-sm)',
        md:  'var(--radius-md)',
        lg:  'var(--radius-lg)',
        xl:  'var(--radius-xl)',
        '2xl':'var(--radius-2xl)',
      },
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
  ],
};
```

---

### Component Standards

#### Buttons
Four variants × three sizes. Always use semantic classes — never ad-hoc Tailwind in templates.

```scss
// Primary — call to action
.btn-primary {
  background: var(--color-primary);
  color: var(--text-inverse);
  border-radius: var(--radius-md);
  font-weight: var(--font-medium);
  transition: background 150ms ease, box-shadow 150ms ease;
  &:hover  { background: var(--color-primary-hover); }
  &:active { background: var(--color-primary-active); }
  &:focus-visible { outline: 2px solid var(--border-focus); outline-offset: 2px; }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
// Secondary — outlined
.btn-secondary { border: 1px solid var(--border-default); background: var(--surface-card); color: var(--text-primary); }
// Ghost — text only with hover state
.btn-ghost     { background: transparent; color: var(--text-primary); }
// Danger — destructive actions
.btn-danger    { background: var(--color-danger); color: var(--text-inverse); }

// Sizes
.btn-sm  { padding: var(--space-1) var(--space-3); font-size: var(--text-sm); }
.btn-md  { padding: var(--space-2) var(--space-4); font-size: var(--text-sm); }  // default
.btn-lg  { padding: var(--space-3) var(--space-6); font-size: var(--text-base); }
```

#### Cards
```scss
.card {
  background: var(--surface-card);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}
.card-header {
  padding: var(--space-4) var(--space-6);
  border-bottom: 1px solid var(--border-default);
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  color: var(--text-primary);
}
.card-body   { padding: var(--space-6); }
.card-footer {
  padding: var(--space-4) var(--space-6);
  border-top: 1px solid var(--border-default);
  background: var(--color-neutral-50);
  border-radius: 0 0 var(--radius-lg) var(--radius-lg);
}
```

#### Stat Cards (Dashboard)
```scss
.stat-card {
  @extend .card;
  display: flex;
  align-items: flex-start;
  gap: var(--space-4);
  padding: var(--space-5) var(--space-6);

  .stat-icon {
    width: 2.5rem; height: 2.5rem;
    border-radius: var(--radius-lg);
    display: grid; place-items: center;
    // background tinted per portal-accent (set inline via Angular binding)
  }
  .stat-value {
    font-size: var(--text-3xl);
    font-weight: var(--font-bold);
    color: var(--text-primary);
    line-height: var(--leading-tight);
  }
  .stat-label  { font-size: var(--text-sm); color: var(--text-secondary); margin-top: var(--space-1); }
  .stat-change { font-size: var(--text-xs); font-weight: var(--font-medium); }
  .stat-change.positive { color: var(--color-success); }
  .stat-change.negative { color: var(--color-danger); }
}
```

#### Data Tables
```scss
.data-table-wrapper {
  @extend .card;
  overflow: hidden;
}
.data-table {
  width: 100%;
  border-collapse: collapse;

  th {
    padding: var(--space-3) var(--space-4);
    text-align: left;
    font-size: var(--text-xs);
    font-weight: var(--font-semibold);
    color: var(--text-secondary);
    text-transform: uppercase;
    letter-spacing: var(--tracking-widest);
    background: var(--color-neutral-50);
    border-bottom: 1px solid var(--border-default);
  }
  td {
    padding: var(--space-4);
    font-size: var(--text-sm);
    color: var(--text-primary);
    border-bottom: 1px solid var(--border-default);
  }
  tbody tr:last-child td { border-bottom: none; }
  tbody tr:hover td { background: var(--color-neutral-50); }
}
```

#### Badges / Status Chips
```scss
.badge {
  display: inline-flex; align-items: center;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-full);
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  line-height: 1;
}
.badge-success { background: var(--color-success-light); color: var(--color-success-dark); }
.badge-warning { background: var(--color-warning-light); color: var(--color-warning-dark); }
.badge-danger  { background: var(--color-danger-light);  color: var(--color-danger-dark); }
.badge-info    { background: var(--color-info-light);    color: var(--color-info-dark); }
.badge-neutral { background: var(--color-neutral-100);  color: var(--color-neutral-600); }
```

#### Form Elements
```scss
.form-label {
  display: block;
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--text-primary);
  margin-bottom: var(--space-1);
}
.form-input {
  width: 100%;
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  background: var(--surface-input);
  color: var(--text-primary);
  transition: border-color 150ms ease, box-shadow 150ms ease;

  &::placeholder { color: var(--text-muted); }
  &:focus {
    outline: none;
    border-color: var(--border-focus);
    box-shadow: 0 0 0 3px rgba(59,130,246,0.15);
    background: var(--surface-input-focus);
  }
  &.error {
    border-color: var(--border-error);
    &:focus { box-shadow: 0 0 0 3px rgba(239,68,68,0.15); }
  }
}
.form-hint  { font-size: var(--text-xs); color: var(--text-muted); margin-top: var(--space-1); }
.form-error { font-size: var(--text-xs); color: var(--color-danger); margin-top: var(--space-1); }
```

#### Sidebar Component
```scss
.sidebar {
  width: 256px;
  min-height: 100vh;
  background: var(--sidebar-bg);
  border-right: 1px solid var(--sidebar-border);
  box-shadow: var(--shadow-sidebar);
  display: flex; flex-direction: column;
  transition: width 200ms ease;

  &.collapsed { width: 68px; }
}
.sidebar-logo {
  padding: var(--space-5) var(--space-4);
  border-bottom: 1px solid var(--sidebar-border);
}
.sidebar-nav { flex: 1; padding: var(--space-4) var(--space-2); overflow-y: auto; }
.sidebar-section-label {
  padding: var(--space-3) var(--space-3) var(--space-1);
  font-size: 0.65rem;
  font-weight: var(--font-semibold);
  text-transform: uppercase;
  letter-spacing: var(--tracking-widest);
  color: var(--color-neutral-500);
}
.nav-item {
  display: flex; align-items: center; gap: var(--space-3);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  color: var(--sidebar-text);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  cursor: pointer;
  transition: background 120ms ease, color 120ms ease;

  &:hover  { background: var(--sidebar-item-hover); color: var(--sidebar-text-active); }
  &.active {
    background: var(--sidebar-item-active);
    color: var(--sidebar-text-active);
    .nav-icon { color: var(--sidebar-icon-active); }
  }
  .nav-icon    { font-size: 1.25rem; flex-shrink: 0; }
  .nav-label   { flex: 1; white-space: nowrap; overflow: hidden; }
  .nav-badge   { @extend .badge; @extend .badge-danger; }
}
```

#### Navbar Component
```scss
.navbar {
  height: 60px;
  background: var(--navbar-bg);
  border-bottom: 1px solid var(--border-default);
  box-shadow: var(--shadow-navbar);
  display: flex; align-items: center;
  padding: 0 var(--space-6);
  gap: var(--space-4);
  position: sticky; top: 0; z-index: 40;
}
.navbar-breadcrumb { flex: 1; }
.navbar-actions    { display: flex; align-items: center; gap: var(--space-3); }
.user-avatar {
  width: 2rem; height: 2rem;
  border-radius: var(--radius-full);
  background: var(--portal-accent);
  color: var(--text-inverse);
  display: grid; place-items: center;
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  cursor: pointer;
}
```

#### Loading Skeletons
```scss
@keyframes shimmer {
  from { background-position: -200% 0; }
  to   { background-position:  200% 0; }
}
.skeleton {
  background: linear-gradient(
    90deg,
    var(--color-neutral-100) 25%,
    var(--color-neutral-200) 50%,
    var(--color-neutral-100) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: var(--radius-md);
}
.skeleton-text  { height: 1em;    width: 100%; }
.skeleton-title { height: 1.5em;  width: 60%;  }
.skeleton-card  { height: 120px;  width: 100%; }
```

#### Toast Notifications
```scss
.toast-container {
  position: fixed; bottom: var(--space-6); right: var(--space-6);
  z-index: 100; display: flex; flex-direction: column; gap: var(--space-2);
}
.toast {
  display: flex; align-items: flex-start; gap: var(--space-3);
  padding: var(--space-4);
  background: var(--surface-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  border-left: 4px solid;
  min-width: 320px; max-width: 420px;
  animation: slide-in 200ms ease;

  &.toast-success { border-color: var(--color-success); }
  &.toast-warning { border-color: var(--color-warning); }
  &.toast-error   { border-color: var(--color-danger); }
  &.toast-info    { border-color: var(--color-info); }
}
@keyframes slide-in {
  from { transform: translateX(100%); opacity: 0; }
  to   { transform: translateX(0);    opacity: 1; }
}
```

---

### Layout System

#### App Shell
```
┌─────────────────────────────────────────────┐
│  Navbar (sticky, 60px height)               │
├──────────────┬──────────────────────────────┤
│              │                              │
│  Sidebar     │   Main Content Area          │
│  (256px /    │   (flex: 1, scrollable)      │
│   68px col.) │                              │
│              │   .page-header               │
│              │   .page-content              │
│              │                              │
└──────────────┴──────────────────────────────┘
```

```scss
.app-shell {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--surface-page);
}
.app-body    { display: flex; flex: 1; overflow: hidden; }
.main-content {
  flex: 1; overflow-y: auto;
  padding: var(--space-6);
}
.page-header {
  margin-bottom: var(--space-6);
  .page-title    { font-size: var(--text-2xl); font-weight: var(--font-bold); color: var(--text-primary); }
  .page-subtitle { font-size: var(--text-sm); color: var(--text-secondary); margin-top: var(--space-1); }
}
.page-actions { display: flex; align-items: center; gap: var(--space-3); }
```

#### Responsive Breakpoints (Tailwind defaults, matched to TailwindCSS)
```
sm:  640px  — small tablet
md:  768px  — tablet
lg:  1024px — laptop (sidebar collapses to icon-only below this)
xl:  1280px — desktop (full sidebar)
2xl: 1536px — wide desktop
```

Sidebar behavior:
- `< lg`: Hidden by default, toggled via hamburger (overlay mode)
- `lg–xl`: Collapsed (icon-only, 68px)
- `>= xl`: Expanded (full, 256px)

---

### Angular Component Conventions

1. **No inline styles or hardcoded Tailwind color classes** in templates. Use design system classes only.
2. **Component encapsulation**: `ViewEncapsulation.None` for layout/shell components (they consume global design system classes). `Emulated` (default) for isolated UI components.
3. **ChangeDetectionStrategy.OnPush** on all components — required for enterprise performance.
4. **Loading states**: Every data-fetching component exposes `isLoading: boolean`. Show `skeleton` classes when loading, never empty white space.
5. **Empty states**: Every list/table has an empty state template with icon + message + optional CTA.
6. **Error states**: Every API-bound component handles error gracefully — show inline error with retry option, not a blank component.

---

#### Portal-Specific Architecture

**Shared Portal** — Public pages: Landing, Login, Signup. No sidebar, no guards.

**Hub Portal (PLATFORM_ADMIN)**:
- Layout: `HubLayoutComponent`, `HubSidebarComponent`, `HubNavbarComponent`
- Features: Tenant management, global user management, audit logs, system settings

**Console Portal (PLATFORM_TENANT_ADMIN)**:
- Layout: `ConsoleLayoutComponent`, `ConsoleSidebarComponent`, `ConsoleNavbarComponent`
- Features: Tenant-scoped user management, mapping configuration, workflow settings

**Workspace Portal (PLATFORM_USER)**:
- Layout: `WorkspaceLayoutComponent`, `WorkspaceSidebarComponent`, `WorkspaceNavbarComponent`
- Features: Data mapping interface, workflow execution, reports

#### Role-Based Routing
```typescript
const routes: Routes = [
  { path: '', redirectTo: '/landing', pathMatch: 'full' },
  {
    path: 'landing',
    loadChildren: () => import('./portals/shared-portal/shared-portal.module')
      .then(m => m.SharedPortalModule)
  },
  {
    path: 'hub',
    loadChildren: () => import('./portals/hub-portal/hub-portal.module')
      .then(m => m.HubPortalModule),
    canActivate: [RoleGuard],
    data: { roles: ['PLATFORM_ADMIN'] }
  },
  {
    path: 'console',
    loadChildren: () => import('./portals/console-portal/console-portal.module')
      .then(m => m.ConsolePortalModule),
    canActivate: [RoleGuard],
    data: { roles: ['PLATFORM_TENANT_ADMIN'] }
  },
  {
    path: 'workspace',
    loadChildren: () => import('./portals/workspace-portal/workspace-portal.module')
      .then(m => m.WorkspacePortalModule),
    canActivate: [RoleGuard],
    data: { roles: ['PLATFORM_USER'] }
  }
];
```

#### Shared Components
```
shared/
├── components/
│   ├── layout/          # base-layout, base-sidebar, base-navbar, base-main
│   ├── ui/              # sidebar-item, breadcrumbs, page-header, user-menu
│   └── common/          # loading-spinner, error-message, confirmation-dialog
├── services/
│   ├── auth.service.ts          # JWT storage, login/logout, role extraction
│   ├── layout.service.ts
│   ├── navigation.service.ts
│   └── permission.service.ts
├── guards/
│   ├── auth.guard.ts
│   ├── role.guard.ts
│   └── guest.guard.ts
└── interceptors/
    ├── jwt.interceptor.ts       # Attach Authorization header
    └── error.interceptor.ts     # Global HTTP error handling
```

---

## Technology Stack

### Backend

| Concern | Technology | Version |
|---|---|---|
| Language | Java | **8** (OpenJDK 8) |
| Framework | Spring Boot | **2.7.18** |
| Cloud | Spring Cloud | **2021.0.x** (Jubilee) |
| Build | Maven | 3.8+ |
| Service Discovery | Netflix Eureka | Spring Cloud Netflix |
| API Gateway | Spring Cloud Gateway | (reactive, JDK 8 compatible) |
| Config | Spring Cloud Config | — |
| Security | Spring Security + jjwt | jjwt 0.11.x |
| Inter-Service | Spring Cloud OpenFeign | — |
| Fault Tolerance | Resilience4j | 1.7.x |
| Cache | Redis + Spring Cache | Spring Data Redis |
| ORM | Hibernate / Spring Data JPA | `javax.persistence` namespace |
| DB Migration | Liquibase | — |
| Batch | Spring Batch | 4.3.x (JDK 8 compatible) |
| API Docs | springdoc-openapi-ui | 1.7.x |
| Testing | JUnit 5, Mockito, WireMock | spring-boot-starter-test |
| Boilerplate | Lombok | — |

> **JDK 21 Upgrade Path** (future — separate activity):
> - `java.source` / `java.target`: 8 → 21
> - Spring Boot: 2.7.18 → 3.x | Spring Cloud: 2021.0.x → 2023.x
> - `javax.*` → `jakarta.*` across all services
> - `WebSecurityConfigurerAdapter` → `SecurityFilterChain` (lambda-style)
> - Spring Batch 4.x → 5.x
> - Review Lombok + JDK 21 compatibility
> - Remove `spring-boot-properties-migrator` after upgrade

### Frontend

| Concern | Technology | Notes |
|---|---|---|
| Framework | Angular 19 | Standalone components |
| Styling | TailwindCSS 3.x + `@tailwindcss/forms` | Extended with design tokens |
| Design Tokens | CSS custom properties (`tokens.scss`) | Single source of truth |
| Icons | Material Icons (ligature font) | Via `material-symbols` |
| Accessibility | `@angular/cdk/a11y` | Focus traps, live regions |
| Animations | `@angular/animations` | Sidebar collapse, toasts |
| Font | Inter (Google Fonts) | 400, 500, 600, 700 weights |
| Build | Angular CLI | Production build with budget limits |
| HTTP | Angular HttpClient + interceptors | JWT attach, error handling |
| Testing | Jasmine + Karma + Cypress (E2E) | OnPush change detection aware |

### Infrastructure

| Concern | Technology | Port |
|---|---|---|
| Database | PostgreSQL 14+ | 5432 |
| Cache / Blacklist / Rate Limit | Redis | 6379 |
| Connection Pool | HikariCP | — |
| Tracing | Spring Cloud Sleuth + Zipkin | 9411 |
| Metrics | Micrometer + Actuator | — |

---

## Development Environment Setup

### Prerequisites
- Java 8 (OpenJDK 8)
- Maven 3.8+
- Node.js 18+ and npm
- PostgreSQL 14+
- Redis 7+ (local install or Docker)
- IntelliJ IDEA
- VS Code or IntelliJ IDEA

### Service Startup Order

Services **must** start in this order:

```
1.  PostgreSQL            — port 5432  (external)
2.  Redis                 — port 6379  (external)
3.  Config Server         — port 8888  (no Eureka dependency)
4.  Service Registry      — port 8761
5.  API Gateway           — port 8080
6.  Auth Service          — port 8081
7.  User Service          — port 8082
8.  Tenant Service        — port 8083
9.  Mapper Service        — port 8084
10. Engine Service        — port 8085
11. Orchestrator Service  — port 8086
12. ETL Service           — port 8087
13. Angular Frontend      — port 4200
```

> Config Server starts before Eureka because it does not register itself; other services fetch config before registering with Eureka.

### Local Development
1. **Database**: Install PostgreSQL, create `chrono_db`. Schemas auto-created by Liquibase.
2. **Redis**: `redis-server` locally or `docker run -p 6379:6379 redis:7-alpine`.
3. **Backend**: Import Maven projects in IntelliJ. Run services in startup order above.
4. **Frontend**: `cd frontend/web && npm install && ng serve`.

---

## Project Structure

```
chrono/
├── impl_plan.md
├── CLAUDE.md
├── backend/
│   ├── commons/
│   ├── config-server/
│   ├── service-registry/
│   ├── api-gateway/
│   ├── auth/
│   ├── user/
│   ├── tenant-service/
│   ├── mapper/
│   ├── engine/
│   ├── orchestrator/
│   └── etl/
├── frontend/
│   └── web/
│       └── src/app/
│           ├── portals/
│           │   ├── shared-portal/
│           │   ├── hub-portal/
│           │   ├── console-portal/
│           │   └── workspace-portal/
│           ├── shared/
│           ├── guards/
│           └── core/
├── database/
│   ├── scripts/
│   └── migrations/
└── docs/
    ├── api/
    └── architecture/
```

---

## Implementation Phases

### Phase 1: Foundation (Week 1-2)
**Goal**: Infrastructure skeleton — nothing useful ships yet, but everything else depends on this

- [ ] Set up Maven multi-module structure
- [ ] Build `commons` library: `BaseEntity`, `ApiResponse<T>`, `PageRequest`/`PageResponse<T>`, exceptions, `ApiConstants`
- [ ] Set up Config Server with classpath-backed configs for all services (dev profile)
- [ ] Set up Service Registry (Eureka)
- [ ] Create API Gateway: routes, JWT pre-filter, Redis-backed rate limiter, token blacklist check
- [ ] Create `chrono_db` PostgreSQL database; set `max_connections = 100` (verify)
- [ ] Redis local setup and smoke test
- [ ] Define HikariCP pool sizes per service in Config Server configs

### Phase 2: Core Services (Week 3-4)
**Goal**: Authentication, user and tenant management operational

- [ ] Auth Service: login, JWT issuance, refresh token, logout (Redis blacklist), BCrypt
- [ ] Auth Service: Liquibase seed changeset (roles, default PLATFORM_ADMIN user from env vars)
- [ ] Auth Service: OpenAPI/Swagger setup (`springdoc-openapi-ui`)
- [ ] User Service: CRUD, tenant-scoped, `assertTenantAccess` enforcement, Redis cache, pagination
- [ ] Tenant Service: onboarding, config management, Redis cache
- [ ] Feign clients: User ↔ Tenant (with circuit breakers and fallbacks)
- [ ] Integration tests with WireMock for Feign clients
- [ ] OpenAPI docs on all services

### Phase 3: Business Services (Week 5-6)
**Goal**: Core business processing pipeline operational

- [ ] Mapper Service: mapping rules CRUD, version control, Redis cache
- [ ] Engine Service: loan processing, idempotency key enforcement, Mapper Feign client with cache fallback
- [ ] Orchestrator Service: workflow state machine, `@Async` executor, saga compensation logging
- [ ] ETL Service: Spring Batch job setup, async job submission endpoint, idempotency by `(tenantId, sourceRef, batchDate)`
- [ ] All Feign clients configured with timeouts (3s connect / 10s read)
- [ ] Resilience4j config applied across all services
- [ ] Liquibase migrations for all business schemas
- [ ] Unit + integration tests for all services

### Phase 4: Frontend Implementation (Weeks 7-11)

#### Phase 4A: Foundation & Design System (Week 7)
- [ ] Install and configure TailwindCSS 3.x with `@tailwindcss/forms` plugin
- [ ] Create `styles/tokens.scss` — full design token definitions (colors, typography, spacing, shadows, radii)
- [ ] Extend `tailwind.config.js` to consume all design tokens via CSS vars
- [ ] Create `styles/typography.scss` — Inter font import and base type rules
- [ ] Create `styles/base.scss` — CSS reset and `:root` defaults
- [ ] Build all component SCSS partials: `_buttons`, `_badges`, `_cards`, `_tables`, `_forms`, `_sidebar`, `_navbar`, `_modals`, `_toasts`, `_skeletons`
- [ ] Define all three portal themes (`.theme-hub`, `.theme-console`, `.theme-workspace`)
- [ ] Create shared folder structure
- [ ] `AuthService`: JWT storage (localStorage), login/logout, role extraction
- [ ] `JwtInterceptor`: attach `Authorization` header on all API calls
- [ ] `ErrorInterceptor`: global HTTP error handling (401 → redirect to login, 403 → error page)
- [ ] Base layout components (sidebar, navbar, main) — wired to design system classes
- [ ] Sidebar responsive behavior (overlay / icon-only / expanded by breakpoint)
- [ ] `ToastService`: global notification service using `.toast` component
- [ ] TypeScript interfaces mirroring backend DTOs (including `PageResponse<T>`)
- [ ] Verify `ChangeDetectionStrategy.OnPush` applied on all generated components

#### Phase 4B: Portal Structure & Routing (Week 8)
- [ ] Portal module files (hub, console, workspace)
- [ ] Lazy-loaded routing with `RoleGuard`, `AuthGuard`, `GuestGuard`
- [ ] Route guards with role enforcement matching backend RBAC

#### Phase 4C: Layout Components (Week 9)
- [ ] Hub, Console, Workspace layout + sidebar + navbar components
- [ ] Navigation config per portal (role-appropriate menu items)
- [ ] Responsive/collapsible sidebar
- [ ] Breadcrumb navigation

#### Phase 4D: Portal Features (Week 10)
- [ ] Hub portal: dashboard, tenant management, global user management, audit log viewer
- [ ] Console portal: dashboard, tenant-scoped user management, mapping config
- [ ] Workspace portal: dashboard, data mapping interface, workflow execution
- [ ] Paginated data tables using `PageResponse<T>` from backend
- [ ] Forms with client-side validation mirroring backend Bean Validation constraints

#### Phase 4E: Integration & Testing (Week 11)
- [ ] Wire all Angular services to live backend endpoints
- [ ] E2E tests (Cypress) for: login flow, role-based redirect, CRUD operations per portal
- [ ] Unit tests for all components and services
- [ ] Bundle size analysis and lazy loading verification
- [ ] Accessibility compliance (WCAG 2.1 AA)

### Phase 5: Final Integration & Production Readiness (Week 12)
**Goal**: Production-grade hardening

- [ ] End-to-end integration testing across all services
- [ ] Security audit: JWT signing key strength, CORS headers, SQL injection (verify no native queries with user input), input validation coverage
- [ ] Load test API Gateway rate limiter behavior
- [ ] Set up Spring Cloud Sleuth + Zipkin distributed tracing
- [ ] Actuator health endpoints verified on all services (`/actuator/health`, `/actuator/info`)
- [ ] Documentation: API docs aggregation at Gateway, deployment guide
- [ ] Prod `application-prod.yml` configs: connection pools, Redis, JWT secret from env
- [ ] User acceptance testing

---

## Configuration Management

### Config Server Layout
```
config-server/src/main/resources/config/
├── application.yml                  # shared defaults
├── auth-service-dev.yml
├── auth-service-prod.yml
├── user-service-dev.yml
├── user-service-prod.yml
├── tenant-service-dev.yml
├── mapper-service-dev.yml
├── engine-service-dev.yml
├── orchestrator-service-dev.yml
└── etl-service-dev.yml
```

### Per-Service Config Includes
Each service config file contains:
- `spring.datasource` — schema-specific JDBC URL, HikariCP pool size
- `spring.redis` — Redis connection (shared instance, different key namespaces)
- `eureka.client` — Eureka registration
- `resilience4j` — circuit breaker, retry, time limiter thresholds
- `feign.client.config` — connect and read timeouts
- `logging.level` — per-package log levels
- `jwt.secret` — **value from environment variable, never hardcoded**

---

## Security Considerations

### Authentication
- jjwt 0.11.x; signing algorithm: HS512 (or RS256 for prod with key pair)
- JWT validated at Gateway only
- Token revocation via Redis blacklist (JTI → expiry TTL)
- Refresh token rotation: stored in DB, single-use, invalidated on use or logout
- BCrypt strength 12

### Authorization
- RBAC: `PLATFORM_ADMIN`, `TENANT_ADMIN`, `TENANT_USER`
- Gateway: coarse-grained route-level security
- Services: fine-grained `@PreAuthorize("hasRole('...')")` on methods
- Tenant isolation: `assertTenantAccess` in `BaseService` — throws 403 on mismatch
- PLATFORM_ADMIN bypasses tenant check via role check in `assertTenantAccess`

### Data Security
- No entity exposure — DTOs only in all API responses
- Bean Validation on all request DTOs (`@Valid`, `@NotNull`, `@Size`, etc.)
- No native queries with user input — all queries via JPA / JPQL with parameters
- HTTPS enforced in prod (TLS termination at load balancer or Gateway)
- JWT secret in environment variable; never in version control

---

## Engineering Standards Checklist

These are enforced per CLAUDE.md and must be verified in every code review:

- [ ] No `@Data` on any JPA entity — use explicit `@Getter`/`@Setter`/`@Builder`
- [ ] All entities extend `BaseEntity` (id-based `equals`/`hashCode`)
- [ ] No entity returned from any controller — DTO only
- [ ] All list endpoints paginated — `PageRequest` / `PageResponse<T>`
- [ ] All business entities have `deletedAt` (soft delete); hard delete forbidden
- [ ] SLF4J structured logging — `log.info("msg field={}", value)` — no string concat
- [ ] No sensitive data in logs (passwords, tokens, PII)
- [ ] MDC populated with `tenantId`, `userId`, `traceId` at service entry points
- [ ] Every Feign client has a `fallback` class with circuit breaker
- [ ] Feign timeouts configured (3s connect / 10s read) via Config Server
- [ ] `@ControllerAdvice` GlobalExceptionHandler present in every service
- [ ] `assertTenantAccess` called before any data read/write in tenant-scoped services
- [ ] Idempotency key enforced on Engine and ETL write endpoints
- [ ] `ApiConstants.API_V1` used as prefix — never hardcoded `/api/v1`
- [ ] Liquibase changelog per service — no manual DDL changes

---

## Testing Strategy

### Backend
- **Unit**: JUnit 5 + Mockito — service layer, every happy path + failure scenario + edge case
- **Integration**: `@SpringBootTest` + Testcontainers (PostgreSQL + Redis) — controller through DB
- **Feign Clients**: WireMock stubs for downstream services
- **API**: MockMvc for controller-layer tests

### Frontend
- **Unit**: Jasmine + Karma — components and services
- **E2E**: Cypress — login flow, role redirect, CRUD per portal, pagination

---

## Monitoring and Logging

### Logging
- SLF4J + Logback; MDC: `tenantId`, `userId`, `traceId`
- Gateway logs all inbound requests (method, path, tenantId, responseTime)
- Never log: tokens, passwords, PII
- Log levels: DEBUG (dev), INFO (prod), WARN/ERROR always

### Distributed Tracing
- Spring Cloud Sleuth — auto-injects `traceId`/`spanId` into all log lines and Feign calls
- Zipkin — trace visualization. Optional locally, required in prod.

### Monitoring
- `/actuator/health`, `/actuator/info`, `/actuator/metrics` on all services
- Micrometer metrics (future: Prometheus scrape + Grafana dashboards)

---

## Deployment Considerations

### Local Development
- Services run individually in IntelliJ IDEA
- PostgreSQL and Redis run locally (or via minimal Docker)
- Frontend on localhost:4200

### Future Production
- Docker: Dockerfile per service
- Docker Compose for full local stack
- Kubernetes for production
- CI/CD: GitHub Actions or Jenkins
- PostgreSQL read replicas for reporting queries

---

## Current Implementation Status

### Pending (Not Started)
- All backend services
- Frontend portals
- Database schemas and Liquibase migrations
- Infrastructure (Redis, Config Server, Eureka)

---

> **Development Note**: Run services directly in IntelliJ IDEA for local development. No Docker required during development phase.
> **Upgrade Note**: JDK 21 + Spring Boot 3.x migration is a separate planned activity post-delivery.
