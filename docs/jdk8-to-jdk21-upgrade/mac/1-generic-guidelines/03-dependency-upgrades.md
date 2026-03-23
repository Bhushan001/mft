# 03 - Dependency Upgrades

> **Confluence Page:** Generic Guidelines / Dependency Upgrades
> **Owner:** All team members

---

## Version Matrix

| Dependency | JDK 8 Version | JDK 21 Version | Breaking? |
|---|---|---|---|
| Spring Boot | 2.7.18 | 3.3.4 | **YES** — javax→jakarta, Security 6 |
| Spring Cloud | 2021.0.9 | 2023.0.3 | **YES** — bootstrap context changes |
| Spring Security | 5.7.x | 6.3.x | **YES** — WebSecurityConfigurerAdapter removed |
| Spring Data JPA | 2.7.x | 3.3.x | **YES** — Hibernate 6, QueryDSL changes |
| Hibernate | 5.6.x | 6.5.x | **YES** — HQL syntax, `@Type` annotation |
| Spring Batch | 4.3.x | 5.1.x | **YES** — Job/Step builder API changes |
| JJWT | 0.11.5 | 0.12.6 | **YES** — builder API changes |
| Resilience4j | 1.7.1 | 2.2.0 | Minor — some config property renames |
| Lombok | 1.18.30 | 1.18.34 | No — annotation processor order matters |
| MapStruct | 1.5.5.Final | 1.6.2.Final | No |
| Liquibase | 4.x | 4.29.x | Minor — some property renames |
| PostgreSQL JDBC | 42.6.0 | 42.7.4 | No |
| Testcontainers | — | 1.20.x | Add if not present |

---

## 1. Spring Boot 3.3.x

Spring Boot 3 is the most impactful change. It:
- Requires **JDK 17+** (we target 21)
- Migrates all internal APIs from `javax.*` to `jakarta.*`
- Upgrades Spring Security to 6.x
- Upgrades Hibernate to 6.x
- Drops several auto-configuration classes

```xml
<!-- parent pom.xml -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.4</version>
</parent>
```

---

## 2. Spring Cloud 2023.0.3

Spring Cloud 2023 (codename Leyton) aligns with Spring Boot 3.3.x.

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-dependencies</artifactId>
    <version>2023.0.3</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

### Config Client Changes
The `spring.cloud.config.uri` property still works, but bootstrap context loading changes:

```yaml
# BEFORE (bootstrap.yml)
spring:
  cloud:
    config:
      uri: http://config-server:8888

# AFTER (application.yml) - bootstrap is deprecated
spring:
  config:
    import: "optional:configserver:http://config-server:8888"
```

> **Note:** `spring-cloud-starter-bootstrap` dependency can be added to preserve old behavior temporarily, but migrate to the new `spring.config.import` approach for clean upgrade.

---

## 3. JJWT 0.12.x

JJWT 0.12.x has **API-breaking changes** from 0.11.x.

```xml
<!-- BEFORE -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>

<!-- AFTER — same GAV, new version -->
<jjwt.version>0.12.6</jjwt.version>
```

### Code Changes Required for JJWT 0.12.x

```java
// BEFORE (0.11.x)
String token = Jwts.builder()
    .setSubject(userId)
    .setExpiration(expiry)
    .signWith(key, SignatureAlgorithm.HS256)
    .compact();

Claims claims = Jwts.parserBuilder()
    .setSigningKey(key)
    .build()
    .parseClaimsJws(token)
    .getBody();

// AFTER (0.12.x)
String token = Jwts.builder()
    .subject(userId)
    .expiration(expiry)
    .signWith(key)           // Algorithm inferred from key type
    .compact();

Claims claims = Jwts.parser()
    .verifyWith(secretKey)   // parserBuilder() → parser()
    .build()
    .parseSignedClaims(token) // parseClaimsJws() → parseSignedClaims()
    .getPayload();            // getBody() → getPayload()
```

---

## 4. Resilience4j 2.x

```xml
<resilience4j.version>2.2.0</resilience4j.version>
```

Configuration property prefix changed:

```yaml
# BEFORE
resilience4j.circuitbreaker.instances.myService:
  registerHealthIndicator: true

# AFTER (same — no change for basic config)
# BUT: some metrics/actuator integration paths changed
```

Feign integration dependency change:

```xml
<!-- BEFORE -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-feign</artifactId>
</dependency>

<!-- AFTER — use Spring Cloud Circuit Breaker starter instead -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

---

## 5. Spring Batch 5.x (etl-service only)

Spring Batch 5 has **significant breaking changes**.

```xml
<!-- Managed by Spring Boot 3 parent — no explicit version needed -->
<dependency>
    <groupId>org.springframework.batch</groupId>
    <artifactId>spring-batch-core</artifactId>
</dependency>
```

### Key Breaking Changes

```java
// BEFORE (Spring Batch 4)
@Bean
public Job myJob(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory) {
    return jobBuilderFactory.get("myJob")
        .start(stepBuilderFactory.get("myStep")
            .<InputType, OutputType>chunk(10)
            .reader(reader())
            .processor(processor())
            .writer(writer())
            .build())
        .build();
}

// AFTER (Spring Batch 5)
@Bean
public Job myJob(JobRepository jobRepository, PlatformTransactionManager txManager) {
    return new JobBuilder("myJob", jobRepository)
        .start(new StepBuilder("myStep", jobRepository)
            .<InputType, OutputType>chunk(10, txManager)
            .reader(reader())
            .processor(processor())
            .writer(writer())
            .build())
        .build();
}
```

> `JobBuilderFactory` and `StepBuilderFactory` are **removed** in Spring Batch 5.

---

## 6. Lombok 1.18.34

```xml
<lombok.version>1.18.34</lombok.version>
```

No API changes. Ensure annotation processor is configured in maven-compiler-plugin (see Build Changes page). Common issue on JDK 21:

```
error: cannot find symbol @Builder
```

Fix: ensure Lombok is in `annotationProcessorPaths` **before** MapStruct.

---

## 7. MapStruct 1.6.x

```xml
<mapstruct.version>1.6.2.Final</mapstruct.version>
```

No breaking changes. Works with Lombok 1.18.34 when processor ordering is correct.

---

## 8. Liquibase 4.29.x

Managed by Spring Boot 3 parent. Key changes:

```yaml
# BEFORE
spring:
  liquibase:
    change-log: classpath:/db/changelog/db.changelog-master.yaml

# AFTER — same, but property name changed
spring:
  liquibase:
    change-log: classpath:/db/changelog/db.changelog-master.yaml
    # New property (optional):
    enabled: true
```

Liquibase 4.x requires `liquibase-core` — already included via `spring-boot-starter-data-jpa`.

---

## Dependency Resolution Tips

```bash
# See full dependency tree for a module
mvn -pl auth-service dependency:tree -Dverbose

# Check for version conflicts
mvn -pl auth-service dependency:tree -Dincludes=org.springframework.security

# Force dependency updates
mvn -pl auth-service versions:display-dependency-updates

# Check for plugin updates
mvn versions:display-plugin-updates
```
