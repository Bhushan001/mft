# Task 3 — Config Server & Service Registry

> **Owner:** Bhushan Gadekar
> **Sprint:** 1 — Week 1
> **Status:** Pending
> **Modules:** `backend/config-server`, `backend/service-registry`

---

## Objective

Upgrade the infrastructure/platform services. These are simpler than application services — minimal code, mostly configuration changes.

---

## Config Server Migration

### Dependency Update

```xml
<!-- pom.xml — managed by parent, just verify -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-server</artifactId>
</dependency>
```

Spring Cloud 2023 manages the version. No explicit version override needed.

### Application Configuration

```yaml
# application.yml — check for deprecated properties
server:
  port: 8888

spring:
  application:
    name: config-server
  profiles:
    active: native
  cloud:
    config:
      server:
        native:
          search-locations: classpath:/config/

# Spring Boot 3 actuator format change
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always
```

### Verify

```bash
mvn -pl config-server clean package -DskipTests
java -jar config-server/target/*.jar
# Visit: http://localhost:8888/actuator/health → {"status":"UP"}
```

---

## Service Registry (Eureka) Migration

### Dependency

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

### Main Class

```java
@SpringBootApplication
@EnableEurekaServer
public class ServiceRegistryApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceRegistryApplication.class, args);
    }
}
```

No changes needed here — `@EnableEurekaServer` is unchanged.

### Application Configuration

```yaml
# application.yml
server:
  port: 8761

spring:
  application:
    name: service-registry

eureka:
  instance:
    hostname: localhost
  client:
    registerWithEureka: false
    fetchRegistry: false
    serviceUrl:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
  server:
    enableSelfPreservation: false

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

### Known Issue: Eureka with Spring Boot 3

Spring Cloud 2023 Eureka server has a known issue with Spring Boot 3.2+ where the dashboard may throw errors. Add this to `application.yml` if the Eureka dashboard shows errors:

```yaml
eureka:
  server:
    waitTimeInMsWhenSyncEmpty: 0
    response-cache-update-interval-ms: 3000
```

### Verify

```bash
mvn -pl service-registry clean package -DskipTests
java -jar service-registry/target/*.jar
# Visit: http://localhost:8761 → Eureka dashboard
# Visit: http://localhost:8761/actuator/health → {"status":"UP"}
```

---

## Dockerfile Updates (Both Services)

```dockerfile
# BEFORE
FROM maven:3.8.8-eclipse-temurin-8 AS build
...
FROM eclipse-temurin:8-jre-alpine

# AFTER
FROM maven:3.9.9-eclipse-temurin-21 AS build
...
FROM eclipse-temurin:21-jre-alpine
```

---

## Completion Criteria

- [ ] `mvn -pl config-server clean package` succeeds
- [ ] Config server starts and `/actuator/health` returns `UP`
- [ ] `mvn -pl service-registry clean package` succeeds
- [ ] Service registry starts and Eureka dashboard loads
- [ ] Both Dockerfiles updated to JDK 21 base images
- [ ] Verified via docker-compose: other services can register with Eureka

---

## Notes / Observations

*(Fill in during execution)*

| Date | Observation |
|---|---|
| | |
