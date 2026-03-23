# Generic Guidelines: JDK 8 → JDK 21 Upgrade

> **Confluence Parent Page:** JDK Migration / Generic Guidelines
> **Audience:** All team members — applicable to any Spring Boot microservice project

---

## Sub-Pages

| Page | Description |
|---|---|
| [01 - Overview & Planning](./01-overview-and-planning.md) | Why upgrade, risks, migration strategy, and go/no-go checklist |
| [02 - Maven & Build Changes](./02-maven-and-build-changes.md) | POM updates, compiler plugin, toolchains, multi-module setup |
| [03 - Dependency Upgrades](./03-dependency-upgrades.md) | Spring Boot, Spring Cloud, JJWT, Lombok, MapStruct, Resilience4j version matrix |
| [04 - Spring Boot 3.x Migration](./04-spring-boot-3x-migration.md) | javax→jakarta, auto-configuration changes, actuator, properties migration |
| [05 - Code Changes](./05-code-changes.md) | Language-level changes, removed APIs, new Java 21 features to adopt |
| [06 - Security Migration](./06-security-migration.md) | Spring Security 6.x changes, JWT filter migration, WebFlux security |
| [07 - Tests Migration](./07-tests-migration.md) | JUnit 5 alignment, Spring Boot Test changes, MockMvc, TestContainers |

---

## Recommended Reading Order

1. Start with **Overview & Planning** to understand scope and risk
2. Execute **Maven & Build Changes** first — this unblocks compilation
3. Apply **Dependency Upgrades** — resolve version conflicts
4. Fix **Spring Boot 3.x** breaking changes — javax→jakarta is the largest change
5. Fix **Code Changes** — language-level and removed API issues
6. Fix **Security Migration** — Spring Security 6 is a separate breaking change
7. Validate **Tests Migration** — ensure test suite is green before sign-off
