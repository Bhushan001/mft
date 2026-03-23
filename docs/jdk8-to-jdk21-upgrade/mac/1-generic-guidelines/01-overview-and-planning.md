# 01 - Overview & Planning

> **Confluence Page:** Generic Guidelines / Overview & Planning
> **Owner:** Bhushan Gadekar

---

## Why Upgrade to JDK 21?

| Reason | Detail |
|---|---|
| **JDK 8 EOL** | Oracle public support ended March 2022; security patches require paid support |
| **Spring Boot 3.x requires JDK 17+** | Spring Boot 2.7.x EOL was November 2023; no further CVE patches |
| **Performance** | JDK 21 ZGC/G1 improvements reduce GC pause times by up to 60% |
| **Virtual Threads (Project Loom)** | Massively scalable concurrency without reactive complexity |
| **Modern Language Features** | Records, Sealed classes, Pattern matching, Text blocks |
| **Security** | 5 years of CVE fixes between JDK 8 and JDK 21 |

---

## Migration Strategy: Big Bang vs Strangler Fig

### Recommended: Module-by-Module (Strangler Fig)

Migrate one service at a time in dependency order:

```
commons → config-server → service-registry → api-gateway
       → auth-service → user-service → tenant-service
       → mapper-service → engine-service
       → etl-service → orchestrator-service → audit-service
```

**Rationale:**
- Each service can be deployed and tested independently
- Reduces blast radius if a service fails post-migration
- Allows parallel work across team members

### Not Recommended: Big Bang
Migrating all services simultaneously makes debugging exponentially harder.

---

## Pre-Migration Checklist

### Environment Setup
- [ ] Install JDK 21 (Eclipse Temurin or Amazon Corretto recommended)
- [ ] Set `JAVA_HOME` to JDK 21
- [ ] Install Maven 3.9.x (required for Spring Boot 3.x)
- [ ] Verify: `java -version` → `openjdk 21`
- [ ] Verify: `mvn -version` → `Apache Maven 3.9.x`
- [ ] Update IDE (IntelliJ IDEA 2023.3+ or Eclipse 2023-12+) for JDK 21 support

### Code Baseline
- [ ] All existing tests pass on JDK 8 before starting migration
- [ ] No unresolved `TODO` / `FIXME` items in migration scope
- [ ] Create a migration branch: `git checkout -b jdk21-migration`
- [ ] Tag the last JDK 8 release: `git tag jdk8-baseline`

### Dependency Audit
- [ ] Run `mvn dependency:tree` on each service and save output
- [ ] Identify all `javax.*` imports: `grep -r "import javax\." --include="*.java" | wc -l`
- [ ] Identify all `sun.*` imports (will break): `grep -r "import sun\." --include="*.java"`
- [ ] Check for Nashorn JS usage: `grep -r "ScriptEngine\|Nashorn" --include="*.java"`

---

## Migration Phases

### Phase 1: Foundation (Week 1)
- Migrate `commons` + `config-server` + `service-registry`
- Update parent POM to Spring Boot 3.x
- Fix all `javax→jakarta` in commons
- Validate compilation

### Phase 2: Gateway & Auth (Week 2)
- Migrate `api-gateway` (WebFlux security changes are significant)
- Migrate `auth-service` (Spring Security 6 changes)
- End-to-end login flow must work before proceeding

### Phase 3: Core Services (Week 3)
- Migrate `user-service`, `tenant-service`, `mapper-service`, `engine-service`
- Validate inter-service Feign calls work

### Phase 4: Batch & Orchestration (Week 4)
- Migrate `etl-service` (Spring Batch 5 has breaking changes)
- Migrate `orchestrator-service`, `audit-service`

### Phase 5: Integration & Sign-off (Week 5)
- Full integration test run via docker-compose
- Performance baseline comparison (JDK 8 vs JDK 21)
- Documentation update

---

## Risk Register

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| `javax→jakarta` missed in a service | High | High | Grep scan before/after migration |
| Spring Batch 5 API breaking changes | High | High | Refer to official migration guide; test thoroughly |
| Hibernate 6 incompatible named queries | Medium | High | Run full integration test suite |
| Feign client incompatibility | Medium | Medium | Test each Feign client call after migration |
| JWT library API changes | Low | High | JJWT 0.12.x has builder API changes |
| Lombok annotation processor on JDK 21 | Low | Medium | Use Lombok 1.18.32+ |
| Test failures due to stricter reflection | Medium | Medium | Add `--add-opens` JVM args if needed |

---

## Go / No-Go Criteria

### Go (Proceed to Next Service)
- All unit tests pass
- Service starts successfully with `spring.profiles.active=dev`
- Actuator `/health` returns `UP`
- Feign clients return expected responses in integration test

### No-Go (Stop and Fix)
- Any `ClassNotFoundException` or `NoClassDefFoundError` at startup
- Flyway/Liquibase migration fails
- Security filter chain throws `NullPointerException` on valid JWT

---

## Useful Commands

```bash
# Find all javax imports
grep -rn "import javax\." backend/ --include="*.java"

# Find all sun.* imports
grep -rn "import sun\." backend/ --include="*.java"

# Check JDK version used by Maven
mvn -version

# Compile single module
mvn -pl commons clean compile

# Run specific service tests
mvn -pl auth-service test

# Full build skipping tests
mvn clean install -DskipTests
```
