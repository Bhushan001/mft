# Task 1 — Mapper Service Migration

> **Owner:** Pankaj
> **Sprint:** 3 — Week 3
> **Status:** Pending
> **Module:** `backend/mapper-service`
> **Complexity:** Medium

---

## Scope

Mapper service handles:
- Mapping rule CRUD
- Rule lifecycle: draft → publish → activate → rollback
- Redis cache for active mapping rules
- Resilience4j circuit breaker on Feign clients

---

## Step-by-Step Tasks

### Step 1: Replace `javax.*` → `jakarta.*`

```bash
find backend/mapper-service/src -name "*.java" -exec sed -i '' \
    's/import javax\.persistence\./import jakarta.persistence./g;
     s/import javax\.validation\./import jakarta.validation./g;
     s/import javax\.servlet\./import jakarta.servlet./g;
     s/import javax\.annotation\./import jakarta.annotation./g' {} +

grep -rn "import javax\." backend/mapper-service/src/ --include="*.java"
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
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated());
        return http.build();
    }
}
```

---

### Step 3: Redis Property Update

```yaml
# BEFORE
spring:
  redis:
    host: ${REDIS_HOST:redis}
    port: 6379

# AFTER
spring:
  data:
    redis:
      host: ${REDIS_HOST:redis}
      port: 6379
```

---

### Step 4: Resilience4j 2.x

Spring Cloud 2023 brings Resilience4j 2.x. Update Feign client circuit breaker config:

```yaml
# application.yml — circuit breaker config (Resilience4j 2.x property keys unchanged for basic config)
resilience4j:
  circuitbreaker:
    instances:
      engine-service:
        registerHealthIndicator: true
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
```

Replace manual Resilience4j dependency with Spring Cloud Circuit Breaker:

```xml
<!-- In pom.xml if using manual resilience4j-feign -->
<!-- REMOVE -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-feign</artifactId>
</dependency>

<!-- ADD (managed by Spring Cloud BOM) -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

---

### Step 5: Verify MappingRule Entity

```java
import jakarta.persistence.*;

@Entity
@Table(name = "mapping_rules", schema = "chrono_mapper")
public class MappingRule extends BaseEntity {
    // verify jakarta imports
}
```

---

### Step 6: Build and Test

```bash
mvn -pl mapper-service clean install
mvn -pl mapper-service test

# Smoke test
curl -X POST http://localhost:8084/api/v1/mapping-rules \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-123" \
  -H "X-User-Id: user-001" \
  -H "X-User-Role: TENANT_ADMIN" \
  -d '{"name":"Field Mapper","sourceField":"input.name","targetField":"output.fullName"}'
# Expected: 201 Created
```

---

## Completion Criteria

- [ ] Zero `javax.*` imports
- [ ] `mvn -pl mapper-service clean install` succeeds
- [ ] Redis property updated
- [ ] Security config updated
- [ ] Resilience4j 2.x working with Feign fallback
- [ ] All unit tests pass

---

## Notes / Observations

*(Fill in during execution)*

| Date | Observation |
|---|---|
| | |
