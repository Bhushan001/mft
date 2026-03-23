# Task 2 — User Service Migration

> **Owner:** Sakshi
> **Sprint:** 3 — Week 3
> **Status:** Pending
> **Module:** `backend/user-service`
> **Complexity:** Medium

---

## Scope

User service handles:
- User profile CRUD
- Role assignment
- Calls auth-service via Feign (register/delete credentials)
- Redis cache for user profiles

---

## Step-by-Step Tasks

### Step 1: Replace `javax.*` → `jakarta.*`

```bash
find backend/user-service/src -name "*.java" -exec sed -i '' \
    's/import javax\.persistence\./import jakarta.persistence./g;
     s/import javax\.validation\./import jakarta.validation./g;
     s/import javax\.servlet\./import jakarta.servlet./g;
     s/import javax\.annotation\./import jakarta.annotation./g' {} +

grep -rn "import javax\." backend/user-service/src/ --include="*.java"
# Should return 0
```

---

### Step 2: Spring Security Config

User-service likely has a minimal security config (trusting gateway-injected headers). Apply the same `SecurityFilterChain` pattern:

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

### Step 3: Feign Client to Auth-Service

The Feign client calls auth-service's internal endpoints. No code change needed for the interface itself. Verify OpenFeign works with Spring Cloud 2023:

```java
// AuthServiceClient.java — no change needed
@FeignClient(name = "auth-service", path = "/api/v1/auth/internal")
public interface AuthServiceClient {
    @PostMapping("/register")
    ApiResponse<Void> registerCredential(@RequestBody RegisterCredentialRequest request);

    @DeleteMapping("/credentials/{userId}")
    ApiResponse<Void> deleteCredential(@PathVariable String userId);
}
```

**Verify Feign config:**
```yaml
# application.yml
spring:
  cloud:
    openfeign:
      circuitbreaker:
        enabled: true
```

---

### Step 4: Redis Cache Property

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

### Step 5: Spring Cache Annotations

No change needed — `@Cacheable`, `@CacheEvict`, `@CachePut` are unchanged.

```java
@Cacheable(value = "users", key = "#userId")
public UserProfileDto getUserById(String userId) { ... }

@CacheEvict(value = "users", key = "#userId")
public UserProfileDto updateUser(String userId, UpdateUserRequest request) { ... }
```

---

### Step 6: Verify `UserProfile` Entity

```java
// Ensure jakarta imports
import jakarta.persistence.*;

@Entity
@Table(name = "user_profiles", schema = "chrono_users")
public class UserProfile extends BaseEntity {
    // ...
}
```

---

### Step 7: Build and Test

```bash
mvn -pl user-service clean install

# Run tests
mvn -pl user-service test
```

---

### Step 8: Feign Integration Test

With auth-service and user-service running:

```bash
TOKEN="<jwt-from-auth-service>"

# Create user (calls auth-service internally)
curl -X POST http://localhost:8082/api/v1/users \
  -H "Content-Type: application/json" \
  -H "X-User-Id: admin" \
  -H "X-Tenant-Id: tenant-123" \
  -H "X-User-Role: SUPER_ADMIN" \
  -d '{"email":"newuser@test.com","firstName":"Test","lastName":"User","tenantId":"tenant-123","role":"TENANT_USER"}'
# Expected: 201 Created

# Get user
curl http://localhost:8082/api/v1/users/{id} \
  -H "X-Tenant-Id: tenant-123" \
  -H "X-User-Role: TENANT_ADMIN"
# Expected: 200 OK with user profile
```

---

## Completion Criteria

- [ ] Zero `javax.*` imports in user-service
- [ ] `mvn -pl user-service clean install` succeeds
- [ ] `WebSecurityConfigurerAdapter` removed
- [ ] Redis property updated
- [ ] Feign call to auth-service works (user creation registers credentials)
- [ ] All unit tests pass

---

## Notes / Observations

*(Fill in during execution)*

| Date | Observation |
|---|---|
| | |
