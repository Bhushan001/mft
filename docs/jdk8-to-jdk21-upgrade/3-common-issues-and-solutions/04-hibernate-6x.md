# 04 - Hibernate 6.x Issues

---

## Issue 1: `@Type` annotation `type` attribute not found

**Error:**
```
[ERROR] attribute type is undefined for the annotation type @Type
```

**Root Cause:** Hibernate 6 removed the `type` attribute from `@Type`. The old Hibernate 5 type system was replaced with the new `@JdbcTypeCode` / `@JavaType` system.

**Fix:**
```java
// BEFORE (Hibernate 5)
@Type(type = "json")
private Map<String, Object> metadata;

@Type(type = "uuid-char")
private UUID externalId;

// AFTER (Hibernate 6)
@JdbcTypeCode(SqlTypes.JSON)
private Map<String, Object> metadata;

// UUID is handled natively — no @Type needed
private UUID externalId;
```

---

## Issue 2: `org.hibernate.annotations.Type` class not found

**Error:**
```
[ERROR] cannot find symbol: class Type
  import org.hibernate.annotations.Type;
```

**Root Cause:** `org.hibernate.annotations.Type` still exists in Hibernate 6, but its usage changed (see Issue 1).

**Fix:** Replace with appropriate Hibernate 6 annotations:
- JSON columns → `@JdbcTypeCode(SqlTypes.JSON)`
- Enum as string → `@Enumerated(EnumType.STRING)` (JPA standard — preferred)
- Custom type → implement `UserType` interface (changed in Hibernate 6)

---

## Issue 3: Named Query `?1` positional parameters breaking

**Error:**
```
org.hibernate.query.sqm.ParsingException: ... positional parameter near '?'
```

**Root Cause:** Hibernate 6 is stricter about positional parameter syntax. `?1` style may not work in all contexts.

**Fix:** Use named parameters:
```java
// BEFORE
@Query("SELECT u FROM UserProfile u WHERE u.tenantId = ?1 AND u.deletedAt IS NULL")
List<UserProfile> findByTenantId(String tenantId);

// AFTER — named parameters (preferred)
@Query("SELECT u FROM UserProfile u WHERE u.tenantId = :tenantId AND u.deletedAt IS NULL")
List<UserProfile> findByTenantId(@Param("tenantId") String tenantId);
```

---

## Issue 4: `HibernateJpaVendorAdapter` dialect auto-detection

**Symptom:** Startup warning: `HHH000511: The ...Dialect class has been deprecated`.

**Fix:** Remove explicit dialect configuration — Hibernate 6 auto-detects:
```yaml
# REMOVE this if using standard PostgreSQL
spring:
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect  # REMOVE

# Let Hibernate auto-detect (default behavior)
spring:
  jpa:
    database-platform: # leave empty or remove
```

---

## Issue 5: `FetchType.EAGER` causing `MultipleBagFetchException`

**Error:**
```
org.hibernate.loader.MultipleBagFetchException: cannot simultaneously fetch multiple bags
```

**Root Cause:** Hibernate 6 is stricter about multiple eager `@OneToMany` / `@ManyToMany` collections.

**Fix:**
```java
// Option 1: Change to SET (avoid duplicate-sensitive bag issue)
@OneToMany(mappedBy = "workflow", fetch = FetchType.LAZY)
private Set<WorkflowStep> steps = new HashSet<>();  // Set, not List

// Option 2: Use @EntityGraph for selective eager loading
@EntityGraph(attributePaths = {"steps"})
Optional<Workflow> findWithStepsById(Long id);

// Option 3: Use @Fetch(FetchMode.SUBSELECT)
@Fetch(FetchMode.SUBSELECT)
@OneToMany(mappedBy = "workflow")
private List<WorkflowStep> steps;
```

---

## Issue 6: Hibernate 6 `@Column(columnDefinition = "...")` changes

**Symptom:** DDL generated differently — column types may change subtly.

**Fix:** Be explicit with `columnDefinition` for non-standard types:
```java
// For JSON columns in PostgreSQL
@Column(columnDefinition = "jsonb")
@JdbcTypeCode(SqlTypes.JSON)
private Map<String, Object> metadata;
```

---

## Issue 7: `CriteriaBuilder` query type changes

**Symptom:** Criteria API queries that worked in Hibernate 5 fail at runtime.

**Root Cause:** Hibernate 6 tightened type checking in the Criteria API.

**Fix:** Use JPQL / Spring Data derived queries instead of Criteria API where possible. If Criteria API is required, update type tokens:
```java
// BEFORE
CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);

// AFTER — use typed results
CriteriaQuery<UserProfile> query = cb.createQuery(UserProfile.class);
```

---

## Issue 8: `@CreationTimestamp` / `@UpdateTimestamp` behavior change

**Symptom:** `createdAt` field getting updated on entity updates (should be immutable).

**Fix:** Ensure `updatable = false` on `@Column`:
```java
@CreatedDate
@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;
```

---

## Issue 9: Schema validation fails after Hibernate 6 upgrade

**Symptom:**
```
org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column
```

**Root Cause:** Hibernate 6 may map some types to different column types (e.g., `Boolean` → `boolean` vs `smallint`).

**Fix:**
```yaml
# Temporarily disable schema validation to identify differences
spring:
  jpa:
    hibernate:
      ddl-auto: none  # Let Liquibase manage schema
    properties:
      hibernate:
        globally_quoted_identifiers: false
```

Compare Liquibase-created schema with what Hibernate 6 expects, then align.
