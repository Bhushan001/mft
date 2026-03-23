# 04 - Spring Boot 3.x Migration

> **Confluence Page:** Generic Guidelines / Spring Boot 3.x Migration
> **Owner:** All team members

---

## The Biggest Change: `javax.*` → `jakarta.*`

Spring Boot 3.x moved from Java EE (`javax.*`) to Jakarta EE 10 (`jakarta.*`). This is a **rename-only** change — the APIs are identical, only the package prefix changed.

### Packages Affected in This Project

| Old Package | New Package | Used In |
|---|---|---|
| `javax.persistence.*` | `jakarta.persistence.*` | All entities, repositories |
| `javax.validation.*` | `jakarta.validation.*` | All DTOs with `@Valid`, `@NotNull` |
| `javax.servlet.*` | `jakarta.servlet.*` | Filters, `HttpServletRequest/Response` |
| `javax.annotation.*` | `jakarta.annotation.*` | `@PostConstruct`, `@PreDestroy` |
| `javax.transaction.*` | `jakarta.transaction.*` | `@Transactional` (use Spring's instead) |

### How to Migrate (IntelliJ)

1. **Find & Replace** in entire project:
   - `import javax.persistence.` → `import jakarta.persistence.`
   - `import javax.validation.` → `import jakarta.validation.`
   - `import javax.servlet.` → `import jakarta.servlet.`
   - `import javax.annotation.` → `import jakarta.annotation.`

2. **Via CLI (sed):**
```bash
# Run from backend/ directory
find . -name "*.java" -exec sed -i '' \
    's/import javax\.persistence\./import jakarta.persistence./g;
     s/import javax\.validation\./import jakarta.validation./g;
     s/import javax\.servlet\./import jakarta.servlet./g;
     s/import javax\.annotation\./import jakarta.annotation./g' {} +
```

3. **Verify no remaining javax imports:**
```bash
grep -rn "import javax\." backend/ --include="*.java"
# Should return 0 results (except javax.sql.* which stays — it's JDK, not Jakarta EE)
```

> **Note:** `javax.sql.*` (e.g., `DataSource`) stays as `javax.sql` — it is part of the JDK, not Jakarta EE.

---

## Auto-Configuration Changes

Several Spring Boot auto-configuration classes were renamed or removed.

### Removed / Renamed Classes

| Old Class | New Class / Action |
|---|---|
| `spring.factories` (META-INF) | `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` |
| `SpringBootServletInitializer` | Stays, but import changes to `jakarta.servlet` |
| `WebMvcConfigurationSupport` | No change, but ensure `jakarta.servlet` imports |

### Custom Auto-Config

If any service has `META-INF/spring.factories`:

```properties
# BEFORE (META-INF/spring.factories)
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  com.chrono.commons.config.GlobalConfig

# AFTER (META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)
com.chrono.commons.config.GlobalConfig
```

---

## Application Properties Migration

Spring Boot 3 renamed many properties. Use `spring-boot-properties-migrator` to detect them at startup.

### Common Renames

| Old Property | New Property |
|---|---|
| `spring.redis.*` | `spring.data.redis.*` |
| `spring.jpa.properties.hibernate.dialect` | Auto-detected (remove if generic) |
| `management.metrics.export.prometheus.*` | `management.prometheus.metrics.export.*` |
| `spring.zipkin.base-url` | `management.zipkin.tracing.endpoint` |
| `spring.sleuth.*` | Replaced by **Micrometer Tracing** |

### Zipkin / Distributed Tracing

Spring Cloud Sleuth is **removed** in Spring Cloud 2022+. Replaced by Micrometer Tracing:

```xml
<!-- REMOVE -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-sleuth-zipkin</artifactId>
</dependency>

<!-- ADD -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

```yaml
# BEFORE
spring:
  zipkin:
    base-url: http://zipkin:9411
  sleuth:
    sampler:
      probability: 1.0

# AFTER
management:
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
  tracing:
    sampling:
      probability: 1.0
```

---

## Actuator Changes

Spring Boot 3 Actuator health format changed slightly:

```yaml
# BEFORE
management:
  endpoint:
    health:
      show-details: always
  endpoints:
    web:
      exposure:
        include: health,info,metrics

# AFTER — same, but some endpoint paths changed
management:
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true   # NEW: enables /health/liveness and /health/readiness
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

---

## OpenFeign Changes

Spring Cloud OpenFeign in 2023.0.x requires a different import:

```xml
<!-- BEFORE -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>

<!-- AFTER — same GAV, but now uses Jakarta HTTP client -->
<!-- No dependency change needed; just ensure javax→jakarta in client classes -->
```

Feign client interface changes:

```java
// BEFORE
import feign.RequestInterceptor;
import feign.RequestTemplate;

// AFTER — no package change for Feign itself, but any servlet imports change
// Feign error decoders using javax.servlet must update to jakarta.servlet
```

---

## Spring Data JPA / Hibernate 6

### Named Queries

Hibernate 6 is stricter about HQL syntax:

```java
// BEFORE — positional parameters
@Query("SELECT u FROM UserProfile u WHERE u.tenantId = ?1 AND u.deletedAt IS NULL")

// AFTER — named parameters preferred (positional still works but stricter)
@Query("SELECT u FROM UserProfile u WHERE u.tenantId = :tenantId AND u.deletedAt IS NULL")
```

### `@Type` Annotation

`@Type` from Hibernate 5 changed in Hibernate 6:

```java
// BEFORE (Hibernate 5)
@Type(type = "json")
private Map<String, Object> metadata;

// AFTER (Hibernate 6) — use JPA standard or new @JdbcTypeCode
@JdbcTypeCode(SqlTypes.JSON)
private Map<String, Object> metadata;
```

---

## Eureka / Config Server

Spring Cloud 2023.x with Spring Boot 3.3.x:

```yaml
# No structural change needed for eureka client
eureka:
  client:
    serviceUrl:
      defaultZone: http://service-registry:8761/eureka/
  instance:
    preferIpAddress: true

# Config import (replaces bootstrap.yml)
spring:
  config:
    import: "optional:configserver:http://config-server:8888"
  cloud:
    config:
      fail-fast: false
```

---

## Checklist: Spring Boot 3.x per Service

- [ ] All `javax.*` imports replaced with `jakarta.*`
- [ ] `spring.redis.*` → `spring.data.redis.*` in application.yml
- [ ] Sleuth dependencies replaced with Micrometer Tracing
- [ ] Zipkin URL property updated
- [ ] `spring-boot-properties-migrator` added, app starts without deprecation warnings, then removed
- [ ] Actuator health endpoint tested
- [ ] Spring Batch job builders updated (etl-service only)
