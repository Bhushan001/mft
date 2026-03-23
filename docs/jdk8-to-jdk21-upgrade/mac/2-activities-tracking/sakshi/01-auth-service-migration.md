# Task 1 — Auth Service Migration

> **Owner:** Sakshi
> **Sprint:** 2 — Week 2
> **Status:** Pending
> **Module:** `backend/auth-service`
> **Complexity:** High — Spring Security 6 + JJWT 0.12.x + Redis

---

## Scope

The auth-service handles:
- Login (JWT + refresh token issuance)
- Token refresh and rotation
- Logout (token blacklisting in Redis)
- Password reset
- Internal credential registration (called by user-service via Feign)

**Breaking changes:**
1. `javax.*` → `jakarta.*`
2. Spring Security 6 — `WebSecurityConfigurerAdapter` removed
3. JJWT 0.11.x → 0.12.x — builder API changed
4. Redis property prefix changed

---

## Step-by-Step Tasks

### Step 1: Replace `javax.*` → `jakarta.*`

```bash
find backend/auth-service/src -name "*.java" -exec sed -i '' \
    's/import javax\.persistence\./import jakarta.persistence./g;
     s/import javax\.validation\./import jakarta.validation./g;
     s/import javax\.servlet\./import jakarta.servlet./g;
     s/import javax\.annotation\./import jakarta.annotation./g' {} +
```

Verify zero remaining:
```bash
grep -rn "import javax\." backend/auth-service/src/ --include="*.java"
```

---

### Step 2: Migrate Spring Security Config

See [Generic Guidelines — Security Migration](../../1-generic-guidelines/06-security-migration.md) for the full pattern.

```java
// BEFORE
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception { ... }
}

// AFTER
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/login",
                                 "/api/v1/auth/refresh",
                                 "/api/v1/auth/forgot-password",
                                 "/api/v1/auth/reset-password",
                                 "/api/v1/auth/internal/**",
                                 "/actuator/**").permitAll()
                .anyRequest().authenticated());

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

### Step 3: Migrate JJWT 0.11.x → 0.12.x

Find the `JwtUtil` or `JwtService` class in auth-service. Update:

```java
// BEFORE (JJWT 0.11.x)

// Key creation
Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));

// Token generation
String token = Jwts.builder()
    .setSubject(userId)
    .claim("tenantId", tenantId)
    .claim("role", role)
    .setIssuedAt(new Date())
    .setExpiration(new Date(System.currentTimeMillis() + expiration))
    .setId(jti)
    .signWith(key, SignatureAlgorithm.HS256)
    .compact();

// Token parsing
Claims claims = Jwts.parserBuilder()
    .setSigningKey(key)
    .build()
    .parseClaimsJws(token)
    .getBody();

// AFTER (JJWT 0.12.x)

// Key creation — same
SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));

// Token generation
String token = Jwts.builder()
    .subject(userId)                    // setSubject → subject
    .claim("tenantId", tenantId)        // same
    .claim("role", role)                // same
    .issuedAt(new Date())               // setIssuedAt → issuedAt
    .expiration(new Date(...))          // setExpiration → expiration
    .id(jti)                            // setId → id
    .signWith(key)                      // algorithm inferred from SecretKey type
    .compact();

// Token parsing
Claims claims = Jwts.parser()          // parserBuilder() → parser()
    .verifyWith(key)                   // setSigningKey() → verifyWith()
    .build()
    .parseSignedClaims(token)          // parseClaimsJws() → parseSignedClaims()
    .getPayload();                     // getBody() → getPayload()
```

**JTI (JWT ID) for blacklisting — same logic, new API:**
```java
String jti = claims.getId();           // getId() still works in 0.12.x
```

---

### Step 4: Redis Property Update

```yaml
# BEFORE — application.yml
spring:
  redis:
    host: ${REDIS_HOST:redis}
    port: ${REDIS_PORT:6379}

# AFTER
spring:
  data:
    redis:
      host: ${REDIS_HOST:redis}
      port: ${REDIS_PORT:6379}
```

---

### Step 5: Fix JWT Filter for `jakarta.servlet`

If auth-service has its own `OncePerRequestFilter` for internal endpoint protection:

```java
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
// was javax.servlet.*
```

---

### Step 6: Verify Entity and Repository

```java
// UserCredential entity — verify jakarta imports
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
// etc.

// Verify Liquibase migrations still run
mvn -pl auth-service liquibase:update -Dliquibase.url=jdbc:postgresql://localhost:5432/chrono_db \
    -Dliquibase.username=postgres -Dliquibase.password=postgres
```

---

### Step 7: Build and Test

```bash
# Build
mvn -pl auth-service clean install

# Run unit tests
mvn -pl auth-service test

# Check test output — all existing test scenarios must pass:
# - loginShouldSucceed_whenCredentialsAreValid
# - loginShouldThrowBusinessException_whenAccountIsLocked
# - loginShouldThrowBusinessException_whenAccountIsDisabled
# etc.
```

---

### Step 8: Manual Smoke Test

```bash
# Start: redis, postgres, service-registry, config-server, auth-service
docker compose up -d postgres redis
mvn -pl auth-service spring-boot:run

# Test login
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@chrono.com","password":"Admin@123"}'
# Expected: 200 OK with accessToken and refreshToken

# Test with wrong password
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@chrono.com","password":"wrong"}'
# Expected: 401 with error message
```

---

## Completion Criteria

- [ ] Zero `javax.*` imports in auth-service
- [ ] `mvn -pl auth-service clean install` succeeds
- [ ] `WebSecurityConfigurerAdapter` removed; `SecurityFilterChain` bean in place
- [ ] JJWT 0.12.x API used (no `parserBuilder`, `parseClaimsJws`, `getBody`)
- [ ] Redis property updated to `spring.data.redis.*`
- [ ] All unit tests pass
- [ ] Login smoke test returns JWT
- [ ] Logout blacklists token in Redis

---

## Notes / Observations

*(Fill in during execution)*

| Date | Observation |
|---|---|
| | |
