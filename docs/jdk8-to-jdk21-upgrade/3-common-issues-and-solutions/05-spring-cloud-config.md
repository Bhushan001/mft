# 05 - Spring Cloud & Config Issues

---

## Issue 1: `bootstrap.yml` not loading

**Symptom:** Config server properties are not loaded; service uses default values.

**Root Cause:** Spring Cloud 2022+ removed automatic bootstrap context loading. `bootstrap.yml` is no longer automatically processed.

**Fix — Option A (Recommended):** Migrate to `spring.config.import`:
```yaml
# application.yml — replace bootstrap.yml entirely
spring:
  application:
    name: auth-service
  config:
    import: "optional:configserver:http://config-server:8888"
  cloud:
    config:
      fail-fast: false
```

**Fix — Option B (Temporary — keep bootstrap.yml):** Add the bootstrap starter:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bootstrap</artifactId>
</dependency>
```

> Option A is cleaner and the forward-compatible approach.

---

## Issue 2: `ConfigDataLocationNotFoundException` at startup

**Error:**
```
ConfigDataLocationNotFoundException: Config data location
  'configserver:http://config-server:8888' cannot be found
```

**Root Cause:** Config server not running when service starts, and `optional:` prefix not used.

**Fix:** Add `optional:` prefix:
```yaml
spring:
  config:
    import: "optional:configserver:http://config-server:8888"
```

Or set:
```yaml
spring:
  cloud:
    config:
      fail-fast: false
```

---

## Issue 3: Spring Cloud Sleuth classes not found

**Error:**
```
[ERROR] package org.springframework.cloud.sleuth does not exist
```

**Root Cause:** Spring Cloud Sleuth is **removed** from Spring Cloud 2022+. Replaced by Micrometer Tracing.

**Fix:** Replace Sleuth dependencies:
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
# REMOVE spring.sleuth.*
# REMOVE spring.zipkin.*

# ADD
management:
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
  tracing:
    sampling:
      probability: 1.0
```

---

## Issue 4: Feign client `404` after Spring Cloud upgrade

**Symptom:** Feign call returns 404 even though the target service is running.

**Common Causes:**

1. **Service name mismatch in Eureka:** Verify `spring.application.name` in the target service matches the `@FeignClient(name = "...")`.

2. **Load balancer not found:** Ensure Spring Cloud LoadBalancer is on the classpath:
   ```xml
   <dependency>
       <groupId>org.springframework.cloud</groupId>
       <artifactId>spring-cloud-starter-loadbalancer</artifactId>
   </dependency>
   ```
   > `spring-cloud-starter-netflix-ribbon` is removed in Spring Cloud 2021+. LoadBalancer replaces it.

3. **Wrong base path:** If the Feign client `path` attribute is wrong:
   ```java
   @FeignClient(name = "auth-service", path = "/api/v1/auth/internal")
   ```

---

## Issue 5: Eureka dashboard shows errors with Spring Boot 3.2+

**Symptom:** Eureka dashboard at `http://localhost:8761` loads but shows `Whitelabel Error Page` or incomplete rendering.

**Root Cause:** Eureka server's Freemarker template may conflict with Spring Boot 3.2 auto-configuration.

**Fix:** Add to `service-registry/application.yml`:
```yaml
eureka:
  server:
    waitTimeInMsWhenSyncEmpty: 0

spring:
  freemarker:
    prefer-file-system-access: false  # force classpath loading for Eureka templates
```

---

## Issue 6: `spring.redis.*` properties ignored

**Symptom:** Redis connection fails. Properties seem correct but not picked up.

**Root Cause:** Spring Boot 3 renamed `spring.redis.*` to `spring.data.redis.*`.

**Fix:**
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

**Services affected:** api-gateway, auth-service, user-service, mapper-service, engine-service.

---

## Issue 7: OpenFeign `@RequestHeader` not forwarding headers

**Symptom:** Downstream service receives empty `X-Tenant-Id` from Feign calls.

**Root Cause:** Feign does not automatically forward headers from the incoming request. Headers must be explicitly passed or configured via a `RequestInterceptor`.

**Fix — Option A:** Pass headers explicitly:
```java
ApiResponse<Void> registerCredential(
    @RequestHeader("X-User-Id") String userId,
    @RequestBody RegisterCredentialRequest request);
```

**Fix — Option B:** Use `RequestInterceptor` to propagate headers automatically:
```java
@Bean
public RequestInterceptor requestInterceptor(HttpServletRequest request) {
    return template -> {
        template.header("X-Tenant-Id", request.getHeader("X-Tenant-Id"));
        template.header("X-User-Id", request.getHeader("X-User-Id"));
    };
}
```
