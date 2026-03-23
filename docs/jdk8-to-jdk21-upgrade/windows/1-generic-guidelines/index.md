# Generic Guidelines: JDK 8 → JDK 21 Upgrade (Windows)

> **OS:** Windows 10 / Windows 11
> **Shell:** PowerShell 7+

---

## Sub-Pages

| Page | Description |
|---|---|
| [01 - Overview & Planning](./01-overview-and-planning.md) | Why upgrade, risks, Windows environment setup, go/no-go checklist |
| [02 - Maven & Build Changes](./02-maven-and-build-changes.md) | POM updates, compiler plugin, Windows toolchain setup |
| [03 - Dependency Upgrades](./03-dependency-upgrades.md) | Version matrix — identical to Mac |
| [04 - Spring Boot 3.x Migration](./04-spring-boot-3x-migration.md) | javax→jakarta with PowerShell, property changes |
| [05 - Code Changes](./05-code-changes.md) | Language-level changes — identical to Mac |
| [06 - Security Migration](./06-security-migration.md) | Spring Security 6.x — identical to Mac |
| [07 - Tests Migration](./07-tests-migration.md) | Test suite migration — identical to Mac |

---

## Recommended Reading Order

1. **Overview & Planning** — Windows environment setup
2. **Maven & Build Changes** — unblocks compilation
3. **Dependency Upgrades** — resolve version conflicts
4. **Spring Boot 3.x** — javax→jakarta bulk replace via PowerShell
5. **Code Changes** — removed APIs
6. **Security Migration** — Spring Security 6
7. **Tests Migration** — validate test suite is green
