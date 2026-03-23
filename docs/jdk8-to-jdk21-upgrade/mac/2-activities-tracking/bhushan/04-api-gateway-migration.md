# Task 4 — API Gateway Migration

> **Owner:** Bhushan Gadekar
> **Sprint:** 2 — Week 2
> **Status:** Pending
> **Module:** `backend/api-gateway`
> **Complexity:** High — WebFlux + Spring Security 6 + Redis Reactive

---

## Objective

Migrate the api-gateway. This is the most complex service because it uses:
- **Spring WebFlux** (reactive) — not Spring MVC
- **Spring Security 6 WebFlux** — different API from servlet-based security
- **Custom JWT validation** (`JwtAuthenticationFilter` as `GlobalFilter`)
- **Redis Reactive** (Lettuce) for token blacklist and rate limiting

---

## Step-by-Step Tasks

### Step 1: Replace `javax.*` → `jakarta.*`

```bash
find backend/api-gateway/src -name "*.java" -exec sed -i '' \
    's/import javax\.servlet\./import jakarta.servlet./g;
     s/import javax\.annotation\./import jakarta.annotation./g' {} +
```

> Note: WebFlux uses `ServerWebExchange`, `ServerHttpRequest`, not `HttpServletRequest`. Most gateway code may not have `javax.servlet` imports at all — verify:

```bash
grep -rn "import javax\." backend/api-gateway/src/ --include="*.java"
```

---

### Step 2: Spring Security 6 WebFlux Config

```java
// BEFORE — Spring Security 5 WebFlux
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf().disable()
            .authorizeExchange()
                .pathMatchers("/api/v1/auth/**", "/actuator/**").permitAll()
                .anyExchange().authenticated()
            .and()
            .build();
    }
}

// AFTER — Spring Security 6 WebFlux
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/api/v1/auth/**", "/actuator/**", "/eureka/**").permitAll()
                .anyExchange().permitAll())  // JWT validation is in GlobalFilter, not Security
            .build();
    }
}
```

> The gateway delegates JWT validation to `JwtAuthenticationFilter` (a `GlobalFilter`), not Spring Security. Therefore security config is mostly permissive — just disabling CSRF.

---

### Step 3: JWT Global Filter

`JwtAuthenticationFilter` implements `GlobalFilter` and `Ordered`. The reactive filter API uses `ServerWebExchange`:

```java
// This code uses WebFlux types — no javax.servlet involved
// Key changes needed:
// 1. JJWT 0.12.x API change (parserBuilder → parser, parseClaimsJws → parseSignedClaims)
// 2. Redis reactive template API — verify it works with Spring Data Redis 3.x

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // JWT parsing: update for JJWT 0.12.x
        // BEFORE:
        // Claims claims = Jwts.parserBuilder()
        //     .setSigningKey(key).build()
        //     .parseClaimsJws(token).getBody();

        // AFTER:
        Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        // Rest of filter logic unchanged
        return chain.filter(exchange);
    }
}
```

---

### Step 4: Redis Reactive Template

Spring Data Redis 3.x (shipped with Spring Boot 3) changes the property prefix:

```yaml
# BEFORE
spring:
  redis:
    host: redis
    port: 6379

# AFTER
spring:
  data:
    redis:
      host: redis
      port: 6379
```

Redis reactive template code itself (`ReactiveRedisTemplate`, `ReactiveStringRedisTemplate`) has no API changes.

---

### Step 5: Spring Cloud Gateway Routes

Route configuration in `application.yml` is unchanged for Spring Cloud Gateway 4.x:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/v1/auth/**
          filters:
            - StripPrefix=0
```

One change: default filter ordering may differ. Verify JWT filter is applied before route filters:

```java
@Override
public int getOrder() {
    return -1;  // Run before all other filters
}
```

---

### Step 6: Rate Limiting (Redis-based)

If `RequestRateLimiterGatewayFilterFactory` is used:

```yaml
# AFTER — property path change
spring:
  cloud:
    gateway:
      default-filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 10
            redis-rate-limiter.burstCapacity: 20
            # key-resolver bean reference unchanged
```

---

### Step 7: Application Properties Cleanup

Add `spring-boot-properties-migrator` temporarily:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-properties-migrator</artifactId>
    <scope>runtime</scope>
</dependency>
```

Start the gateway and check for deprecation warnings in startup logs. Fix all warned properties, then remove this dependency.

---

### Step 8: End-to-End JWT Flow Test

```bash
# 1. Start: redis, service-registry, config-server, auth-service, api-gateway

# 2. Login via gateway
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@chrono.com","password":"Admin@123"}'
# Expected: 200 OK with JWT token

# 3. Use token for protected call
curl http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer <token>"
# Expected: 200 OK (gateway validates JWT and routes to user-service)

# 4. Use invalid/expired token
curl http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer invalid.token.here"
# Expected: 401 Unauthorized
```

---

## Completion Criteria

- [ ] Zero `javax.*` imports in api-gateway
- [ ] `mvn -pl api-gateway clean package` succeeds
- [ ] Gateway starts with `spring.profiles.active=dev`
- [ ] `/actuator/health` returns `UP`
- [ ] Login via gateway returns JWT token
- [ ] Valid JWT passes through to downstream service
- [ ] Invalid/blacklisted JWT returns 401
- [ ] Rate limiting works with Redis
- [ ] `spring-boot-properties-migrator` removed after property cleanup

---

## Notes / Observations

*(Fill in during execution)*

| Date | Observation |
|---|---|
| | |
