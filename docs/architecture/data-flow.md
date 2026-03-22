# Chrono Platform — Data Flows

## 1. Authentication Flow

```
Client  →  Gateway  →  auth-service
  POST /api/v1/auth/login  { email, password }
  ← 200 { accessToken, refreshToken, expiresIn }

Gateway does NOT validate JWT on /auth/** — these routes are public.
```

**Token propagation on subsequent requests:**
```
Client (Bearer token)
  →  Gateway: validates JWT signature + Redis blacklist check
     injects headers: X-User-Id, X-Tenant-Id, X-User-Role
  →  Downstream service: reads headers, enforces tenant isolation in service layer
```

## 2. Loan Processing Workflow (Orchestrator Saga)

```
Client  POST /api/v1/workflows  { workflowType: LOAN_PROCESSING, inputPayload: {...} }
  →  Orchestrator: creates Workflow(PENDING), returns 202 immediately
  →  @Async executeWorkflowAsync():
       Step 1: Feign → Engine  POST /api/v1/engine/process
               Engine: idempotency check → Feign → Mapper (get active rule)
                       → applyMapping(rule, payload) → returns ProcessResponse
       Step 2: Feign → ETL  POST /api/v1/etl/jobs
               ETL: idempotency check → Spring Batch launch (async)
                    Reader(stub LOS) → Processor(Feign→Engine) → Writer(stub Strategy One)
  →  Orchestrator: updates Workflow(COMPLETED or FAILED)
```

**Compensation (rollback):** If any step fails after prior steps completed, the Orchestrator runs compensation in reverse step order using stored `compensationPayload`.

## 3. Mapping Rule Lifecycle

```
DRAFT  →  PUBLISHED  →  ACTIVE  (only one ACTIVE per tenant at a time)
                    ↓
                 DELETED (soft)
```

- `POST /mappings` — creates DRAFT
- `POST /mappings/{id}/publish` — moves to PUBLISHED, deactivates current ACTIVE, sets this as ACTIVE
- `GET /mappings/active` — internal endpoint consumed by Engine (cached 15 min in Redis)
- Updating an ACTIVE rule is rejected; must publish a new version

## 4. ETL Job Lifecycle

```
SUBMITTED  →  RUNNING  →  COMPLETED
                      ↓
                    FAILED   (re-submit retries via Spring Batch restart)
                      ↓
                   SKIPPED   (idempotent duplicate of COMPLETED job)
```

Idempotency key: `(tenantId, sourceRef, batchDate)` — duplicate submission of a completed job returns `SKIPPED` immediately.

## 5. Database Schema Isolation

Each service owns one PostgreSQL schema. Cross-schema queries are forbidden — services communicate only via HTTP (Feign). Schema names map 1:1 to service ownership:

| Schema                | Owner Service  | Key Tables                                          |
|-----------------------|----------------|-----------------------------------------------------|
| `chrono_auth`         | auth-service   | `user_credentials`, `refresh_tokens`               |
| `chrono_users`        | user-service   | `user_profiles`                                    |
| `chrono_tenants`      | tenant-service | `tenants`                                          |
| `chrono_mapper`       | mapper-service | `mapping_rules`                                    |
| `chrono_engine`       | engine-service | `processing_requests`                              |
| `chrono_orchestrator` | orchestrator   | `workflows`, `workflow_steps`                      |
| `chrono_etl`          | etl-service    | `etl_jobs` + Spring Batch metadata tables          |
