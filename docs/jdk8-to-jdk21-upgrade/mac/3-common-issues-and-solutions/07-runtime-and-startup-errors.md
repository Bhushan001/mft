# 07 - Runtime and Startup Errors

---

## Issue 1: `BeanCreationException` — circular dependency

**Error:**
```
The dependencies of some of the beans in the application context form a cycle:
  securityConfig -> userDetailsService -> authService -> passwordEncoder -> securityConfig
```

**Root Cause:** Spring Boot 3 disabled circular dependency resolution by default (`spring.main.allow-circular-references` defaults to `false`).

**Fix — Preferred:** Break the cycle by refactoring:
```java
// Move PasswordEncoder bean to a separate @Configuration class
@Configuration
public class PasswordEncoderConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Fix — Temporary:** Allow circular references (not recommended for production):
```yaml
spring:
  main:
    allow-circular-references: true
```

---

## Issue 2: Service fails to start — `UnsatisfiedDependencyException`

**Error:**
```
UnsatisfiedDependencyException: Error creating bean with name 'authService':
Unsatisfied dependency expressed through constructor parameter 0;
No qualifying bean of type 'AuthenticationManager' available
```

**Root Cause:** `AuthenticationManager` is no longer auto-created. Must be explicitly exposed as `@Bean`.

**Fix:**
```java
@Configuration
public class SecurityConfig {

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

---

## Issue 3: Redis connection refused at startup

**Error:**
```
RedisConnectionFailureException: Unable to connect to Redis
```

**Common Causes:**

1. **Wrong property key:** `spring.redis.*` → `spring.data.redis.*` (Spring Boot 3 change)
   ```yaml
   spring:
     data:
       redis:
         host: ${REDIS_HOST:localhost}
         port: 6379
   ```

2. **Redis not started:** Ensure Redis is running before the service:
   ```bash
   docker compose up -d redis
   # Wait for: redis ... healthy
   docker compose up -d auth-service
   ```

3. **Wrong host in docker-compose:** Service refers to `localhost` but inside Docker network it's `redis`:
   ```yaml
   REDIS_HOST: redis  # not localhost
   ```

---

## Issue 4: Liquibase migration fails — `object already exists`

**Error:**
```
liquibase.exception.MigrationFailedException:
  Migration failed for changeset db/changelog/001-init.yaml::1::chrono:
  Reason: ERROR: relation "user_credentials" already exists
```

**Root Cause:** Running Liquibase against a database that already has the old schema created by a different Liquibase version or manually.

**Fix:**
```bash
# Option 1: Mark changesets as already run (if schema is correct)
mvn liquibase:changelogSync \
  -Dliquibase.url=jdbc:postgresql://localhost:5432/chrono_db?currentSchema=chrono_auth \
  -Dliquibase.username=postgres \
  -Dliquibase.password=postgres

# Option 2: Drop and recreate schema (local dev only)
psql -U postgres -d chrono_db -c "DROP SCHEMA chrono_auth CASCADE; CREATE SCHEMA chrono_auth;"
```

---

## Issue 5: `NoSuchMethodError` at runtime — JAR version conflict

**Error:**
```
java.lang.NoSuchMethodError: 'io.jsonwebtoken.JwtBuilder io.jsonwebtoken.Jwts.builder()'
```

**Root Cause:** Multiple JJWT versions on the classpath (e.g., 0.11.5 and 0.12.x).

**Fix:** Enforce the version in parent POM:
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.6</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Check for conflicting version:
```bash
mvn -pl auth-service dependency:tree -Dincludes=io.jsonwebtoken
```

---

## Issue 6: `DataSource` bean not found for multi-schema setup

**Error:**
```
Cannot determine embedded database driver class for database type NONE
```

**Root Cause:** Spring Boot 3 requires an explicit DataSource configuration for non-H2 databases if auto-configuration cannot determine the type.

**Fix:** Ensure `spring.datasource.url` is set:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:5432/${DB_NAME:chrono_db}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
  jpa:
    properties:
      hibernate:
        default_schema: chrono_auth  # per-service schema
```

---

## Issue 7: `NullPointerException` in JWT filter at startup

**Symptom:** Gateway starts but first request throws NPE in `JwtAuthenticationFilter`.

**Common Causes:**

1. **`secretKey` is null:** The `@Value("${jwt.secret}")` not injected — property not found in config.
   ```bash
   # Verify the property is present
   curl http://localhost:8888/api-gateway/dev  # config server endpoint
   ```

2. **Key initialization order:** If `SecretKey` is initialized in `@PostConstruct` but the bean is used before `@PostConstruct` runs.
   ```java
   // Fix: use lazy initialization
   private SecretKey getKey() {
       if (secretKey == null) {
           secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
       }
       return secretKey;
   }
   ```

---

## Issue 8: `StackOverflowError` in Feign call (circular service call)

**Symptom:** ETL service calls engine-service, engine-service calls mapper-service, and a misconfigured Feign client causes infinite loop.

**Fix:** Ensure Feign clients use service names from Eureka, not `localhost`:
```yaml
# application.yml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connectTimeout: 5000
            readTimeout: 10000
```

And verify `@FeignClient(name = "engine-service")` uses the registered Eureka name, not an IP address.

---

## Issue 9: `HikariPool-1 - Connection is not available` under load

**Symptom:** After migration, connection pool exhaustion under moderate load.

**Root Cause:** JDK 21 with virtual threads may create more concurrent connections than the pool allows.

**Fix:** Increase HikariCP pool size:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20      # default is 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

> If using virtual threads, consider using `spring.threads.virtual.enabled=true` — virtual threads are designed to block and return connections quickly, so pool pressure actually decreases with Loom. Monitor with `/actuator/metrics/hikaricp.connections.active`.
