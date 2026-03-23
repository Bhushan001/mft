# Common Issues and Solutions (Windows)

> **OS:** Windows 10 / Windows 11
> **Shell:** PowerShell 7+

---

## Sub-Pages

| Page | Description |
|---|---|
| [01 - javax to jakarta](./01-javax-to-jakarta.md) | PowerShell replacement commands, missed imports |
| [02 - Compilation Errors](./02-compilation-errors.md) | JDK 21 compiler errors + Windows-specific issues |
| [03 - Spring Security 6.x](./03-spring-security-6x.md) | Same as Mac — all Java code |
| [04 - Hibernate 6.x](./04-hibernate-6x.md) | Same as Mac — all Java code |
| [05 - Spring Cloud & Config](./05-spring-cloud-config.md) | Same as Mac + Windows PowerShell tips |
| [06 - Spring Batch 5](./06-spring-batch-5.md) | Same as Mac — all Java code |
| [07 - Runtime and Startup Errors](./07-runtime-and-startup-errors.md) | Same as Mac + Windows-specific env issues |

---

## Quick Lookup: Error → Page

| Error Snippet | Go To |
|---|---|
| `package javax.persistence does not exist` | [01 - javax to jakarta](./01-javax-to-jakarta.md) |
| `'java' is not recognized` | [02 - Compilation Errors](./02-compilation-errors.md) |
| `'mvn' is not recognized` | [02 - Compilation Errors](./02-compilation-errors.md) |
| `WebSecurityConfigurerAdapter` not found | [03 - Spring Security 6.x](./03-spring-security-6x.md) |
| `antMatchers` not found | [03 - Spring Security 6.x](./03-spring-security-6x.md) |
| `JobBuilderFactory` not found | [06 - Spring Batch 5](./06-spring-batch-5.md) |
| `@Type` annotation error | [04 - Hibernate 6.x](./04-hibernate-6x.md) |
| `bootstrap.yml` not loading | [05 - Spring Cloud & Config](./05-spring-cloud-config.md) |
| `BeanCreationException` at startup | [07 - Runtime and Startup Errors](./07-runtime-and-startup-errors.md) |
| `JAVA_HOME` not set / wrong version | [02 - Compilation Errors](./02-compilation-errors.md) |
| Path too long build failure | [02 - Compilation Errors](./02-compilation-errors.md) |

---

## Add New Issues

When you hit a new issue, add it to the relevant page:
1. Error message / symptom
2. Root cause
3. Fix (with PowerShell commands where applicable)
4. Service affected
