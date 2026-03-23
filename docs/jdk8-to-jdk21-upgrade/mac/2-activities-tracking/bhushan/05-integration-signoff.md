# Task 5 — Integration Sign-off

> **Owner:** Bhushan Gadekar
> **Sprint:** 5 — Week 5
> **Status:** Pending
> **Prerequisite:** All 13 services migrated by respective owners

---

## Objective

Validate the fully migrated platform end-to-end via Docker Compose. Confirm no regressions, measure performance, and produce the final go-live checklist.

---

## Step 1: Full Docker Compose Startup

```bash
# Build all images
docker compose build --no-cache

# Start infrastructure
docker compose up -d postgres redis zipkin

# Wait for health checks
docker compose ps  # All infra should be "healthy"

# Start platform
docker compose up -d service-registry config-server

# Start all application services
docker compose up -d

# Check all services healthy
docker compose ps
```

Expected: All 13 services show `Up (healthy)`.

---

## Step 2: Smoke Tests

Run these API tests sequentially to verify the critical path:

```bash
BASE="http://localhost:8080/api/v1"

# 1. Register a tenant
curl -X POST $BASE/tenants \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Tenant","plan":"BASIC"}'

# 2. Register a user
curl -X POST $BASE/users \
  -H "Content-Type: application/json" \
  -d '{"email":"test@tenant.com","password":"Test@123","tenantId":"<tenant-id>","role":"TENANT_ADMIN"}'

# 3. Login
TOKEN=$(curl -s -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@tenant.com","password":"Test@123"}' | jq -r '.data.accessToken')

# 4. Get user profile
curl $BASE/users/me \
  -H "Authorization: Bearer $TOKEN"

# 5. Create mapping rule
curl -X POST $BASE/mapping-rules \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Rule","sourceField":"input","targetField":"output"}'

# 6. Submit ETL job
curl -X POST $BASE/etl/jobs/submit \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"sourceRef":"batch-001","batchDate":"2024-01-01"}'

# 7. Check audit trail
curl "$BASE/audit/events?action=LOGIN" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Step 3: Performance Baseline Comparison

Measure key endpoints before and after migration:

```bash
# Install wrk or use Apache Bench
brew install wrk

# Measure login endpoint (10s, 10 concurrent)
wrk -t10 -c10 -d10s -s post.lua http://localhost:8080/api/v1/auth/login

# post.lua:
# wrk.method = "POST"
# wrk.body = '{"email":"test@tenant.com","password":"Test@123"}'
# wrk.headers["Content-Type"] = "application/json"
```

| Endpoint | JDK 8 (req/s) | JDK 21 (req/s) | Improvement |
|---|---|---|---|
| POST /auth/login | *(fill in)* | *(fill in)* | |
| GET /users/me | *(fill in)* | *(fill in)* | |
| POST /engine/process | *(fill in)* | *(fill in)* | |

Expected: 15–40% improvement on JDK 21 with virtual threads enabled.

---

## Step 4: Final Checklist

### Security
- [ ] Login with valid credentials returns JWT
- [ ] Expired JWT returns 401
- [ ] Blacklisted JWT (post-logout) returns 401
- [ ] Role-based endpoints return 403 for wrong role
- [ ] `X-Tenant-Id` header is injected by gateway

### Data Integrity
- [ ] Liquibase migrations run cleanly on fresh DB
- [ ] `created_at`, `updated_at`, `created_by`, `updated_by` populated on all entities
- [ ] Soft-delete sets `deleted_at`; soft-deleted records not returned in GET calls
- [ ] Audit events recorded for CREATE, UPDATE, DELETE, LOGIN operations

### Resilience
- [ ] Circuit breaker trips after engine-service is stopped (ETL returns 503 gracefully)
- [ ] Services restart cleanly after Redis restart
- [ ] All services re-register with Eureka after service-registry restart

### Observability
- [ ] Zipkin traces visible for multi-service requests: http://localhost:9411
- [ ] All services appear in Eureka dashboard: http://localhost:8761
- [ ] Actuator health endpoints return `UP` for all services

---

## Step 5: Update Documentation

- [ ] Update `docs/architecture/overview.md` — reflect JDK 21 + Spring Boot 3.3 versions
- [ ] Update `docs/architecture/local-dev.md` — update local setup instructions
- [ ] Update `docker-compose.yml` comments
- [ ] Tag the migrated commit: `git tag jdk21-migration-complete`

---

## Go-Live Decision

| Criteria | Result | Pass/Fail |
|---|---|---|
| All unit tests passing | | |
| All smoke tests passing | | |
| No performance regression | | |
| No security regressions | | |
| Liquibase migrations clean | | |
| All docker-compose services healthy | | |

**Decision:** _______________
**Signed off by:** Bhushan Gadekar
**Date:** _______________
