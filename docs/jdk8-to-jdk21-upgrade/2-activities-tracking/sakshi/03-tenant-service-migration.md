# Task 3 — Tenant Service Migration

> **Owner:** Sakshi
> **Sprint:** 3 — Week 3
> **Status:** Pending
> **Module:** `backend/tenant-service`
> **Complexity:** Low

---

## Scope

Tenant service handles:
- Tenant provisioning (CRUD)
- Plan and settings management
- Minimal security (gateway-trusted headers)

---

## Step-by-Step Tasks

### Step 1: Replace `javax.*` → `jakarta.*`

```bash
find backend/tenant-service/src -name "*.java" -exec sed -i '' \
    's/import javax\.persistence\./import jakarta.persistence./g;
     s/import javax\.validation\./import jakarta.validation./g;
     s/import javax\.servlet\./import jakarta.servlet./g;
     s/import javax\.annotation\./import jakarta.annotation./g' {} +

grep -rn "import javax\." backend/tenant-service/src/ --include="*.java"
```

---

### Step 2: Spring Security Config

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

### Step 3: Update application.yml

```yaml
# Check for any deprecated Spring Boot 2.x properties
# Add spring-boot-properties-migrator temporarily and check startup logs

# Key check: spring.redis.* → spring.data.redis.* (if Redis is used in tenant-service)
```

---

### Step 4: Verify Tenant Entity

```java
import jakarta.persistence.*;

@Entity
@Table(name = "tenants", schema = "chrono_tenants")
public class Tenant extends BaseEntity {
    // verify all annotations use jakarta.*
}
```

---

### Step 5: Build and Test

```bash
mvn -pl tenant-service clean install

# Run tests
mvn -pl tenant-service test

# Smoke test
curl -X POST http://localhost:8083/api/v1/tenants \
  -H "Content-Type: application/json" \
  -H "X-User-Id: admin" \
  -H "X-User-Role: SUPER_ADMIN" \
  -d '{"name":"Acme Corp","plan":"ENTERPRISE"}'
# Expected: 201 Created
```

---

## Completion Criteria

- [ ] Zero `javax.*` imports in tenant-service
- [ ] `mvn -pl tenant-service clean install` succeeds
- [ ] Security config updated to `SecurityFilterChain`
- [ ] Tenant CRUD API works
- [ ] All unit tests pass

---

## Notes / Observations

*(Fill in during execution)*

| Date | Observation |
|---|---|
| | |
