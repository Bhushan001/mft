# Common Issues and Solutions

> **Confluence Parent Page:** JDK Migration / Common Issues and Solutions
> **Audience:** All team members
> **Purpose:** Living document — add new issues as they are discovered during migration

---

## Sub-Pages

| Page | Description |
|---|---|
| [01 - javax to jakarta](./01-javax-to-jakarta.md) | Package rename errors, missed imports, edge cases |
| [02 - Compilation Errors](./02-compilation-errors.md) | JDK 21 compiler errors, module system, Lombok/MapStruct |
| [03 - Spring Security 6.x](./03-spring-security-6x.md) | WebSecurityConfigurerAdapter removal, antMatchers, filter chain |
| [04 - Hibernate 6.x](./04-hibernate-6x.md) | HQL changes, @Type removal, naming strategy, N+1 queries |
| [05 - Spring Cloud & Config](./05-spring-cloud-config.md) | Bootstrap context, Sleuth→Micrometer, Feign, Eureka issues |
| [06 - Spring Batch 5](./06-spring-batch-5.md) | JobBuilderFactory removal, schema changes, job launcher |
| [07 - Runtime and Startup Errors](./07-runtime-and-startup-errors.md) | Application context failures, Redis, connection pool, circular deps |

---

## How to Contribute

When you encounter a new issue:
1. Add it to the relevant sub-page under the correct heading
2. Include: **Error message / symptom**, **Root cause**, **Fix**, **Service affected**
3. If it doesn't fit an existing page, add a new entry here and create the page

---

## Quick Lookup: Error → Page

| Error Snippet | Go To |
|---|---|
| `package javax.persistence does not exist` | [01 - javax to jakarta](./01-javax-to-jakarta.md) |
| `package javax.validation does not exist` | [01 - javax to jakarta](./01-javax-to-jakarta.md) |
| `WebSecurityConfigurerAdapter` not found | [03 - Spring Security 6.x](./03-spring-security-6x.md) |
| `antMatchers` not found | [03 - Spring Security 6.x](./03-spring-security-6x.md) |
| `JobBuilderFactory` not found | [06 - Spring Batch 5](./06-spring-batch-5.md) |
| `StepBuilderFactory` not found | [06 - Spring Batch 5](./06-spring-batch-5.md) |
| `@Type` annotation error | [04 - Hibernate 6.x](./04-hibernate-6x.md) |
| `InaccessibleObjectException` | [02 - Compilation Errors](./02-compilation-errors.md) |
| `BeanCreationException` at startup | [07 - Runtime and Startup Errors](./07-runtime-and-startup-errors.md) |
| `spring.redis` property not found | [07 - Runtime and Startup Errors](./07-runtime-and-startup-errors.md) |
| `parseClaimsJws` not found | [02 - Compilation Errors](./02-compilation-errors.md) |
| `bootstrap.yml` not loading | [05 - Spring Cloud & Config](./05-spring-cloud-config.md) |
