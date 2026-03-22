# Chrono Platform — API Reference

All APIs are versioned at `/api/v1`. The API Gateway (`localhost:8080`) is the single entry point.

Authenticated endpoints require `Authorization: Bearer <accessToken>`. The gateway injects:
- `X-User-Id` — caller's UUID
- `X-Tenant-Id` — caller's tenant
- `X-User-Role` — `PLATFORM_ADMIN | TENANT_ADMIN | TENANT_USER`

---

## Auth Service — `POST /api/v1/auth/*`

| Method | Path                       | Auth | Description                          |
|--------|----------------------------|------|--------------------------------------|
| POST   | `/auth/login`              | No   | Issue access + refresh tokens        |
| POST   | `/auth/refresh`            | No   | Exchange refresh token for new pair  |
| POST   | `/auth/logout`             | Yes  | Blacklist token, revoke refresh      |
| POST   | `/auth/forgot-password`    | No   | Initiate password reset              |
| POST   | `/auth/reset-password`     | No   | Complete password reset with token   |
| POST   | `/auth/internal/register`  | No*  | Register credentials (user-service)  |
| DELETE | `/auth/internal/credentials/{userId}` | No* | Delete credentials        |

`*` Internal — called only by user-service via Feign, not exposed externally.

**Login request/response:**
```json
// POST /api/v1/auth/login
{ "email": "admin@chrono.dev", "password": "password" }

// 200 OK
{
  "success": true,
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "...",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
}
```

---

## User Service — `/api/v1/users`

| Method | Path              | Role Required      | Description         |
|--------|-------------------|--------------------|---------------------|
| POST   | `/users`          | TENANT_ADMIN+      | Create user profile |
| GET    | `/users/{userId}` | Any                | Get profile by ID   |
| GET    | `/users`          | TENANT_ADMIN+      | List users (paged)  |
| PATCH  | `/users/{userId}` | TENANT_ADMIN+      | Update profile      |
| DELETE | `/users/{userId}` | TENANT_ADMIN+      | Soft-delete user    |

Query params for `GET /users`: `tenantId`, `search`, `status`, `page`, `size`

---

## Tenant Service — `/api/v1/tenants`

| Method | Path                 | Role Required   | Description          |
|--------|----------------------|-----------------|----------------------|
| POST   | `/tenants`           | PLATFORM_ADMIN  | Create tenant        |
| GET    | `/tenants/{tenantId}`| PLATFORM_ADMIN  | Get tenant           |
| GET    | `/tenants`           | PLATFORM_ADMIN  | List tenants (paged) |
| PUT    | `/tenants/{tenantId}`| PLATFORM_ADMIN  | Update tenant        |
| DELETE | `/tenants/{tenantId}`| PLATFORM_ADMIN  | Soft-delete tenant   |

---

## Mapper Service — `/api/v1/mappings`

| Method | Path                       | Description                            |
|--------|----------------------------|----------------------------------------|
| POST   | `/mappings`                | Create mapping rule (DRAFT)            |
| GET    | `/mappings`                | List rules for tenant (paged)          |
| GET    | `/mappings/{ruleId}`       | Get rule by ID                         |
| GET    | `/mappings/active`         | Get active rule (cached 15 min)        |
| PUT    | `/mappings/{ruleId}`       | Update rule (DRAFT only)               |
| POST   | `/mappings/{ruleId}/publish` | Publish rule → sets as ACTIVE        |
| DELETE | `/mappings/{ruleId}`       | Soft-delete rule                       |

---

## Engine Service — `/api/v1/engine`

| Method | Path                          | Description                               |
|--------|-------------------------------|-------------------------------------------|
| POST   | `/engine/process`             | Process a record (idempotent)             |
| GET    | `/engine/requests/{requestId}`| Get processing request by ID             |
| GET    | `/engine/requests`            | List requests for tenant (paged)          |

**Process request:**
```json
// POST /api/v1/engine/process
{
  "idempotencyKey": "job-abc-record-001",
  "inputPayload": "{\"loanId\": \"L001\", ...}"
}

// 200 OK
{
  "success": true,
  "data": {
    "requestId": "...",
    "status": "COMPLETED",
    "outputPayload": "{...}"
  }
}
```

---

## Orchestrator Service — `/api/v1/workflows`

| Method | Path                        | Description                        |
|--------|-----------------------------|------------------------------------|
| POST   | `/workflows`                | Start workflow → `202 ACCEPTED`    |
| GET    | `/workflows/{workflowId}`   | Get workflow + steps               |
| GET    | `/workflows`                | List workflows for tenant (paged)  |

**Workflow types:** `LOAN_PROCESSING`, `ETL_PIPELINE`

```json
// POST /api/v1/workflows
{
  "workflowType": "LOAN_PROCESSING",
  "correlationId": "loan-batch-2024-01",
  "inputPayload": "{...}"
}
// 202 Accepted — poll GET /workflows/{id} for status
```

**Workflow statuses:** `PENDING → RUNNING → COMPLETED | FAILED | COMPENSATING → COMPENSATED`

---

## ETL Service — `/api/v1/etl/jobs`

| Method | Path                    | Description                           |
|--------|-------------------------|---------------------------------------|
| POST   | `/etl/jobs`             | Submit ETL job → `202 ACCEPTED`       |
| GET    | `/etl/jobs/{jobId}`     | Get job status                        |
| GET    | `/etl/jobs`             | List jobs for tenant (paged)          |

**Idempotency:** Re-submitting `(sourceRef, batchDate)` for a COMPLETED job returns `SKIPPED`.

```json
// POST /api/v1/etl/jobs
{
  "sourceRef": "LOS-BATCH-001",
  "batchDate": "2024-01-15",
  "inputPayload": "{...}"
}
// 202 Accepted
```

**ETL job statuses:** `SUBMITTED → RUNNING → COMPLETED | FAILED | SKIPPED`

---

## Common Response Envelope

All endpoints return `ApiResponse<T>`:

```json
{
  "success": true,
  "data": { ... },
  "message": null,
  "status": 200
}
```

Error response:
```json
{
  "success": false,
  "data": null,
  "message": "Tenant not found: abc",
  "status": 404
}
```

Paginated response (`data` field):
```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3,
  "last": false
}
```
