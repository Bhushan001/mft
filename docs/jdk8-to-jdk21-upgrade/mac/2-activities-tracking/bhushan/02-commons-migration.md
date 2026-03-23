# Task 2 — Commons & Shared Library Migration

> **Owner:** Bhushan Gadekar
> **Sprint:** 1 — Week 1
> **Status:** Pending
> **Module:** `backend/commons`

---

## Objective

Migrate the `commons` shared library first. All other services depend on it — fixing `commons` eliminates the bulk of `javax` compilation errors across the entire project.

---

## Scope of Changes

The `commons` module contains:
- `BaseEntity` — JPA entity base class
- `ApiResponse<T>` — standard response wrapper
- `ErrorResponse` + `FieldError` — error DTOs
- `GlobalExceptionHandler` — `@ControllerAdvice`
- Exception hierarchy (`BaseException`, `BusinessException`, etc.)
- `AuditConfig` — JPA auditing configuration
- Common validation annotations and constants

---

## Step-by-Step Tasks

### Step 1: Replace `javax.*` → `jakarta.*`

Run the following in the `backend/commons/src` directory:

```bash
find . -name "*.java" -exec sed -i '' \
    's/import javax\.persistence\./import jakarta.persistence./g;
     s/import javax\.validation\./import jakarta.validation./g;
     s/import javax\.servlet\./import jakarta.servlet./g;
     s/import javax\.annotation\./import jakarta.annotation./g' {} +
```

**Files most likely affected:**
- `BaseEntity.java` — `javax.persistence.*`
- `GlobalExceptionHandler.java` — `javax.servlet.http.HttpServletRequest`
- All DTO classes with `@NotNull`, `@Size`, `@Email` — `javax.validation.constraints.*`
- `AuditConfig.java` / `AuditorAwareImpl.java` — `javax.servlet` for request extraction

---

### Step 2: Verify `BaseEntity` Auditing

`BaseEntity` uses `@EntityListeners(AuditingEntityListener.class)`. In Hibernate 6, ensure this still functions:

```java
// Verify these annotations still resolve correctly after jakarta migration
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ID-based equals/hashCode — no change needed
    @Override
    public boolean equals(Object o) { ... }

    @Override
    public int hashCode() { ... }
}
```

---

### Step 3: Verify `GlobalExceptionHandler`

The handler extends `ResponseEntityExceptionHandler`. In Spring 6, the base class method signatures changed:

```java
// BEFORE — Spring 5
@Override
protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpHeaders headers, HttpStatus status, WebRequest request) {
    // ...
}

// AFTER — Spring 6 (HttpStatusCode replaces HttpStatus)
@Override
protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    // ...
}
```

> Import: `org.springframework.http.HttpStatusCode` (new in Spring 6)

---

### Step 4: Fix `AuditorAwareImpl`

```java
// Verify jakarta.servlet import is updated
import jakarta.servlet.http.HttpServletRequest;  // was javax.servlet

@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    private final HttpServletRequest request;

    @Override
    public Optional<String> getCurrentAuditor() {
        String userId = request.getHeader("X-User-Id");
        return Optional.ofNullable(userId != null ? userId : "system");
    }
}
```

---

### Step 5: Compile and Verify

```bash
# Compile commons
mvn -pl commons clean install -DskipTests

# Expected: BUILD SUCCESS
# If Hibernate 6 issues: check @Type annotations

# Run commons tests (if any)
mvn -pl commons test
```

---

### Step 6: Verify Downstream Compilation

After commons builds successfully, compile all services to see remaining errors:

```bash
mvn clean compile -DskipTests 2>&1 | grep "ERROR" | head -50
```

This will show the remaining `javax.*` issues per service — share the output with Sakshi and Pankaj so they know what to fix in their services.

---

## Completion Criteria

- [ ] Zero `javax.*` imports in `commons/src`
- [ ] `mvn -pl commons clean install` succeeds
- [ ] `BaseEntity` compiles with `jakarta.persistence.*`
- [ ] `GlobalExceptionHandler` handles `HttpStatusCode` (Spring 6)
- [ ] `AuditorAwareImpl` uses `jakarta.servlet`
- [ ] Downstream `mvn clean compile` output shared with team

---

## Notes / Observations

*(Fill in during execution)*

| Date | Observation |
|---|---|
| | |
