# Task 5 — Audit Service Migration

> **Owner:** Pankaj
> **Sprint:** 4 — Week 4
> **Status:** Pending
> **Module:** `backend/audit-service`
> **Complexity:** Low — simplest service, no Redis, no Feign, no Batch

---

## Scope

Audit service handles:
- Recording immutable audit events (called internally by other services)
- Querying audit trail (paginated, filtered by tenant/action/resource)
- Append-only AuditEvent entity (no updates, no deletes)

---

## Step-by-Step Tasks

### Step 1: Replace `javax.*` → `jakarta.*`

```bash
find backend/audit-service/src -name "*.java" -exec sed -i '' \
    's/import javax\.persistence\./import jakarta.persistence./g;
     s/import javax\.validation\./import jakarta.validation./g;
     s/import javax\.servlet\./import jakarta.servlet./g;
     s/import javax\.annotation\./import jakarta.annotation./g' {} +

grep -rn "import javax\." backend/audit-service/src/ --include="*.java"
```

---

### Step 2: Security Config Update

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/audit/internal/**", "/actuator/**").permitAll()
                .anyRequest().authenticated());
        return http.build();
    }
}
```

---

### Step 3: Verify AuditEvent Entity

AuditEvent is append-only — no `@LastModifiedDate` or `@LastModifiedBy`. Ensure the entity is correct:

```java
import jakarta.persistence.*;

@Entity
@Table(
    name = "audit_events",
    schema = "chrono_audit",
    indexes = {
        @Index(name = "idx_audit_tenant", columnList = "tenant_id"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_created", columnList = "created_at")
    }
)
public class AuditEvent {  // Note: does NOT extend BaseEntity (immutable)

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", unique = true, nullable = false)
    private UUID eventId;

    // ... other fields

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    // No setter for createdAt — immutable after creation
}
```

---

### Step 4: Pagination — Spring Data 3.x

Spring Data 3.x (part of Spring Boot 3) changes `Page` and `Pageable` behavior slightly:

```java
// BEFORE — Spring Data 2.x
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// AFTER — same imports, no change needed
// But note: Pageable parameter in controller now uses spring.data.web.PageableHandlerMethodArgumentResolver
// This is auto-configured in Spring Boot 3 — no manual config needed
```

---

### Step 5: Query Method for Audit Trail

Verify the repository query still works with Hibernate 6:

```java
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    // Named parameters work — positional parameters are stricter in Hibernate 6
    @Query("SELECT a FROM AuditEvent a WHERE a.tenantId = :tenantId " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND (:resourceType IS NULL OR a.resourceType = :resourceType) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditEvent> findByTenantIdWithFilters(
        @Param("tenantId") String tenantId,
        @Param("action") String action,
        @Param("resourceType") String resourceType,
        Pageable pageable);
}
```

---

### Step 6: Build and Test

```bash
mvn -pl audit-service clean install
mvn -pl audit-service test

# Smoke tests

# Record an audit event (internal endpoint)
curl -X POST http://localhost:8088/api/v1/audit/internal/record \
  -H "Content-Type: application/json" \
  -d '{
    "action": "LOGIN",
    "resourceType": "USER",
    "resourceId": "user-001",
    "performedBy": "user-001",
    "tenantId": "tenant-123",
    "ipAddress": "127.0.0.1"
  }'
# Expected: 204 No Content

# Query audit events
curl "http://localhost:8088/api/v1/audit/events?action=LOGIN&page=0&size=10" \
  -H "X-Tenant-Id: tenant-123" \
  -H "X-User-Role: TENANT_ADMIN"
# Expected: 200 OK with paginated events
```

---

## Completion Criteria

- [ ] Zero `javax.*` imports
- [ ] `mvn -pl audit-service clean install` succeeds
- [ ] Security config updated
- [ ] Audit event recording works (204 response)
- [ ] Audit event querying works with pagination
- [ ] No updates/deletes possible on audit records
- [ ] All unit tests pass

---

## Notes / Observations

*(Fill in during execution)*

| Date | Observation |
|---|---|
| | |
