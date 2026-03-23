# Task 2 — Engine Service Migration

> **Owner:** Pankaj
> **Sprint:** 3 — Week 3
> **Status:** Pending
> **Module:** `backend/engine-service`
> **Complexity:** Medium

---

## Scope

Engine service handles:
- Record transformation execution
- Idempotency via `(tenantId, idempotencyKey)` unique constraint + Redis 24h cache
- Calls mapper-service via Feign to retrieve active mapping rules

---

## Step-by-Step Tasks

### Step 1: Replace `javax.*` → `jakarta.*`

```bash
find backend/engine-service/src -name "*.java" -exec sed -i '' \
    's/import javax\.persistence\./import jakarta.persistence./g;
     s/import javax\.validation\./import jakarta.validation./g;
     s/import javax\.servlet\./import jakarta.servlet./g;
     s/import javax\.annotation\./import jakarta.annotation./g' {} +

grep -rn "import javax\." backend/engine-service/src/ --include="*.java"
```

---

### Step 2: Redis Property Update

```yaml
# BEFORE
spring:
  redis:
    host: ${REDIS_HOST:redis}

# AFTER
spring:
  data:
    redis:
      host: ${REDIS_HOST:redis}
      port: 6379
```

---

### Step 3: Verify Idempotency Logic

The idempotency cache key pattern uses `StringRedisTemplate` or `RedisTemplate`. No API changes in Spring Data Redis 3.x — only the property prefix changed.

```java
// Idempotency check — no code change needed
String cacheKey = "engine:idempotency:" + tenantId + ":" + idempotencyKey;
String cached = redisTemplate.opsForValue().get(cacheKey);
if (cached != null) {
    return objectMapper.readValue(cached, ProcessResponseDto.class);
}
```

---

### Step 4: Security Config Update

Same `SecurityFilterChain` pattern as other services.

---

### Step 5: `ProcessingRequest` Entity — Check Unique Constraint

```java
@Entity
@Table(
    name = "processing_requests",
    schema = "chrono_engine",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "idempotency_key"})
    }
)
public class ProcessingRequest extends BaseEntity {
    // ensure jakarta.persistence.*
}
```

---

### Step 6: Build and Test

```bash
mvn -pl engine-service clean install
mvn -pl engine-service test

# Idempotency test
curl -X POST http://localhost:8085/api/v1/engine/process \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-123" \
  -H "X-User-Role: TENANT_USER" \
  -d '{"idempotencyKey":"key-001","data":{"input":"test"}}'
# First call: 200 OK (processed)
# Second call with same key: 200 OK (cached response)
```

---

## Completion Criteria

- [ ] Zero `javax.*` imports
- [ ] `mvn -pl engine-service clean install` succeeds
- [ ] Redis property updated
- [ ] Idempotency cache works (duplicate request returns same response)
- [ ] All unit tests pass

---

## Notes / Observations

*(Fill in during execution)*

| Date | Observation |
|---|---|
| | |
